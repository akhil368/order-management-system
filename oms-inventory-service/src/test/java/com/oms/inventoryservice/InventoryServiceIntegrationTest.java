package com.oms.inventoryservice;

import com.oms.inventoryservice.entity.Inventory;
import com.oms.inventoryservice.event.OrderPlacedEvent;
import com.oms.inventoryservice.exception.InsufficientStockException;
import com.oms.inventoryservice.repository.InventoryRepository;
import com.oms.inventoryservice.service.InventoryService;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class InventoryServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("inventory_db");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired InventoryRepository repository;
    @Autowired InventoryService inventoryService;
    @Autowired KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void resetStock() {                       // make each test independent
        repository.save(new Inventory("PROD-1", 100));
        repository.save(new Inventory("PROD-2", 50));
        repository.save(new Inventory("PROD-3", 10));
    }

    @Test
    void reserve_decrementsStock_inRealMySql() {
        inventoryService.reserve("PROD-1", 30);
        assertThat(repository.findById("PROD-1").orElseThrow().getQuantity()).isEqualTo(70);
    }

    @Test
    void reserve_insufficient_throws_andDoesNotOversell() {
        assertThatThrownBy(() -> inventoryService.reserve("PROD-3", 50))
                .isInstanceOf(InsufficientStockException.class);
        // proves the atomic UPDATE matched 0 rows — quantity untouched
        assertThat(repository.findById("PROD-3").orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test
    void orderPlacedEvent_isConsumed_andStockReserved() {
        kafkaTemplate.send("order-placed-events", "PROD-1", event("PROD-1", 5));
        await().atMost(15, SECONDS).untilAsserted(() ->
                assertThat(repository.findById("PROD-1").orElseThrow().getQuantity()).isEqualTo(95));
    }

    @Test
    void permanentFailureEvent_landsOnDeadLetterTopic() {
        // quantity > 1000 -> IllegalArgumentException -> non-retryable -> DLT
        kafkaTemplate.send("order-placed-events", "PROD-2", event("PROD-2", 5000));

        try (KafkaConsumer<String, String> dlt = new KafkaConsumer<>(dltProps())) {
            dlt.subscribe(List.of("order-placed-events.DLT"));
            await().atMost(20, SECONDS).untilAsserted(() -> {
                ConsumerRecords<String, String> recs = dlt.poll(Duration.ofMillis(500));
                assertThat(recs.count()).isGreaterThan(0);
                assertThat(recs.iterator().next().value()).contains("PROD-2");
            });
        }
    }

    private OrderPlacedEvent event(String productId, int qty) {
        return OrderPlacedEvent.builder()
                .orderId("test-" + System.nanoTime())
                .productId(productId).customerId("C").customerEmail("a@b.com")
                .quantity(qty).price(new BigDecimal("10.00")).placedAt(LocalDateTime.now())
                .build();
    }

    private Properties dltProps() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlt-group");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return p;
    }
}