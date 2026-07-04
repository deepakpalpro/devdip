package com.banking.forms.serviceintegration.infrastructure;

import com.banking.forms.serviceintegration.spi.AdapterConfig;
import com.banking.forms.serviceintegration.spi.AdapterTypes;
import com.banking.forms.serviceintegration.spi.ServiceAdapter;
import com.banking.forms.serviceintegration.spi.ServiceRequest;
import com.banking.forms.serviceintegration.spi.ServiceResult;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogServiceAdapter implements ServiceAdapter {

    private static final Logger log = LoggerFactory.getLogger(LogServiceAdapter.class);

    @Override
    public String adapterId() {
        return "log-service";
    }

    @Override
    public String adapterType() {
        return AdapterTypes.LOG;
    }

    @Override
    public ServiceResult execute(ServiceRequest request, AdapterConfig config) {
        log.info(
                "[service:log] operation={} form={} submission={} payloadKeys={}",
                request.operation(),
                request.formCode(),
                request.submissionId(),
                request.payload() == null ? 0 : request.payload().size());
        return ServiceResult.success("log-" + UUID.randomUUID(), request.payload());
    }
}
