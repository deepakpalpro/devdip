package com.banking.forms.downstream.infrastructure;

import com.banking.forms.downstream.spi.ConnectorConfig;
import com.banking.forms.downstream.spi.ConnectorTypes;
import com.banking.forms.downstream.spi.DispatchResult;
import com.banking.forms.downstream.spi.DownstreamConnector;
import com.banking.forms.downstream.spi.OutboundEnvelope;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Zero-setup default downstream sink: writes the sanitized envelope to the application log instead of
 * delivering to a real system, so the downstream step is demoable and testable out of the box (mirrors
 * the in-JVM default providers used elsewhere). Enabled by default; real connectors ({@code rest-webhook},
 * and future kafka/s3 adapters) supersede it once configured and enabled.
 */
@Component
public class LogDownstreamConnector implements DownstreamConnector {

    private static final Logger log = LoggerFactory.getLogger(LogDownstreamConnector.class);

    @Override
    public String connectorId() {
        return "log-sink";
    }

    @Override
    public String connectorType() {
        return ConnectorTypes.LOG;
    }

    @Override
    public DispatchResult dispatch(OutboundEnvelope envelope, ConnectorConfig config) {
        log.info(
                "[downstream:log-sink] event={} form={} submission={} payloadBytes={}",
                envelope.eventType(),
                envelope.formCode(),
                envelope.submissionId(),
                envelope.payloadJson() == null ? 0 : envelope.payloadJson().length());
        return DispatchResult.dispatched("log-" + UUID.randomUUID());
    }
}
