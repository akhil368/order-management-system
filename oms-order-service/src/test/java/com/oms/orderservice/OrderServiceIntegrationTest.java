package com.oms.orderservice;

import com.oms.orderservice.dto.OrderResponse;
import com.oms.orderservice.dto.PlaceOrderRequest;
import com.oms.orderservice.repository.OrderRepository;
import com.oms.orderservice.service.OrderService;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0").withDatabaseName("order_db");

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

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    void placeOrder_persistsToMySql_andPublishesEvent() {
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setProductId("PROD-1");
        req.setCustomerId("CUST-1");
        req.setCustomerEmail("a@b.com");
        req.setQuantity(2);
        req.setPrice(new BigDecimal("499.00"));

        OrderResponse resp = orderService.placeOrder(req);

        assertThat(orderRepository.findById(resp.getOrderId())).isPresent();   // real MySQL

        try (KafkaConsumer<String, String> c = new KafkaConsumer<>(consumerProps())) {
            c.subscribe(List.of("order-placed-events"));
            await().atMost(15, SECONDS).untilAsserted(() -> {                  // real Kafka
                ConsumerRecords<String, String> recs = c.poll(Duration.ofMillis(500));
                assertThat(recs.count()).isGreaterThan(0);
                assertThat(recs.iterator().next().value()).contains(resp.getOrderId());
            });
        }
    }

    private Properties consumerProps() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "test-order-group");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return p;
    }
}