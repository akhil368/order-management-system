package com.oms.inventoryservice.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeadLetterConsumer {

    @KafkaListener(
            topics = "order-placed-events.DLT",
            groupId = "dlt-consumer-group",
            containerFactory = "dltListenerContainerFactory")   // dedicated String factory
    public void handleDeadLetter(
            @Payload String message,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_TOPIC,    required = false) String originalTopic,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_FQCN,    required = false) String exceptionClass,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage) {

        log.error("====== DEAD LETTER RECEIVED ======");
        log.error("Original topic : {}", originalTopic);
        log.error("Failed payload : {}", message);
        log.error("Exception type : {}", exceptionClass);
        log.error("Exception msg  : {}", exceptionMessage);
        log.error("Action         : manual investigation / replay required");
        log.error("==================================");
    }
}
