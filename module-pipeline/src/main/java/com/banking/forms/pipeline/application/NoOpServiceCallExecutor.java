package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.spi.ServiceCallContext;
import com.banking.forms.pipeline.spi.ServiceCallExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Default when {@code module-service-integration} is not on the classpath. */
@Component
@ConditionalOnMissingBean(ServiceCallExecutor.class)
public class NoOpServiceCallExecutor implements ServiceCallExecutor {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public int invoke(ServiceCallContext context) {
        return 0;
    }
}
