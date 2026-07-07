package com.banking.forms.serviceintegration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.forms.serviceintegration.application.ServiceOperations;
import com.banking.forms.serviceintegration.spi.AdapterConfig;
import com.banking.forms.serviceintegration.spi.ServiceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RestServiceAdapterTest {

    private final RestServiceAdapter adapter = new RestServiceAdapter(new ObjectMapper());

    @Test
    void failsWhenEndpointNotConfigured() {
        var result = adapter.execute(
                new ServiceRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "LOAN",
                        ServiceOperations.SUBMISSION_PROCESSED,
                        Map.of(),
                        Map.of()),
                new AdapterConfig(null));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.detail()).contains("endpoint");
    }
}
