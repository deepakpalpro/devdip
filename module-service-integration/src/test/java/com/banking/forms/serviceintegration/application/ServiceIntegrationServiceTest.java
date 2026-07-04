package com.banking.forms.serviceintegration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.forms.pipeline.spi.ServiceCallContext;
import com.banking.forms.serviceintegration.infrastructure.ServiceCallLogRepository;
import com.banking.forms.serviceintegration.spi.AdapterConfig;
import com.banking.forms.serviceintegration.spi.ServiceAdapter;
import com.banking.forms.serviceintegration.spi.ServiceRequest;
import com.banking.forms.serviceintegration.spi.ServiceResult;
import com.banking.forms.submission.application.SubmissionEventRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServiceIntegrationServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SUBMISSION = UUID.randomUUID();

    @Mock private ServiceAdapterRouter adapterRouter;
    @Mock private ServiceCallLogRepository callLogRepository;
    @Mock private SubmissionEventRecorder eventRecorder;
    @Mock private ServiceAdapter adapter;

    private ServiceIntegrationProperties properties;
    private ServiceIntegrationService service;

    @BeforeEach
    void setUp() {
        properties = new ServiceIntegrationProperties();
        service = new ServiceIntegrationService(
                adapterRouter, callLogRepository, eventRecorder, properties, new ObjectMapper());
    }

    @Test
    void invokeCallsAllEnabledProviders() {
        when(adapterRouter.resolveAllEnabled())
                .thenReturn(List.of(new ServiceAdapterRouter.Selection(
                        "log-service", "log", adapter, new AdapterConfig(null))));
        when(adapter.execute(any(), any())).thenReturn(ServiceResult.success("ref-1", Map.of("ok", true)));

        int invoked = service.invoke(new ServiceCallContext(
                TENANT, SUBMISSION, "LOAN", Map.of("loan", Map.of("amount", 1)), "REVIEW", 0.5));

        assertThat(invoked).isEqualTo(1);
        verify(callLogRepository).save(any());
        verify(eventRecorder).record(eq(SUBMISSION), eq("SERVICE_CALL_COMPLETED"), any(), any());
    }
}
