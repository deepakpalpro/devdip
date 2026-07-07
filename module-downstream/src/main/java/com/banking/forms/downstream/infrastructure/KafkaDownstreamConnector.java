package com.banking.forms.downstream.infrastructure;

import com.banking.forms.downstream.spi.ConnectorConfig;
import com.banking.forms.downstream.spi.ConnectorTypes;
import com.banking.forms.downstream.spi.DispatchResult;
import com.banking.forms.downstream.spi.DownstreamConnector;
import com.banking.forms.downstream.spi.OutboundEnvelope;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

/**
 * Publishes the sanitized envelope to a Kafka topic. Uses a short-lived producer per dispatch so no
 * broker connection is held when the connector is disabled.
 */
@Component
public class KafkaDownstreamConnector implements DownstreamConnector {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    @Override
    public String connectorId() {
        return "kafka-stream";
    }

    @Override
    public String connectorType() {
        return ConnectorTypes.KAFKA;
    }

    @Override
    public DispatchResult dispatch(OutboundEnvelope envelope, ConnectorConfig config) {
        String bootstrapServers = config.text("bootstrapServers", null);
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            return DispatchResult.failed("bootstrapServers not configured");
        }

        String topic = config.text("topic", "submissions.processed");
        String payload = envelope.payloadJson() == null ? "{}" : envelope.payloadJson();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "banking-forms-downstream-" + UUID.randomUUID());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) SEND_TIMEOUT.toMillis());
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, (int) SEND_TIMEOUT.toMillis());

        String key = envelope.submissionId() == null ? UUID.randomUUID().toString() : envelope.submissionId().toString();
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, payload.getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-Event-Type", envelope.eventType().getBytes(StandardCharsets.UTF_8));
        if (envelope.formCode() != null) {
            record.headers().add("X-Form-Code", envelope.formCode().getBytes(StandardCharsets.UTF_8));
        }

        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(props)) {
            RecordMetadata metadata = producer.send(record).get();
            return DispatchResult.dispatched(
                    metadata.topic() + "-" + metadata.partition() + "@" + metadata.offset(),
                    "partition=" + metadata.partition() + " offset=" + metadata.offset());
        } catch (Exception ex) {
            return DispatchResult.failed(ex.getMessage());
        }
    }
}
