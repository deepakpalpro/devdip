package com.banking.forms.pipeline.application;

import com.banking.forms.pipeline.spi.Pipelet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PipeletRegistry {

    private final Map<String, Pipelet> pipeletsByCode = new HashMap<>();

    public PipeletRegistry(List<Pipelet> pipelets) {
        for (Pipelet pipelet : pipelets) {
            pipeletsByCode.put(pipelet.code(), pipelet);
        }
    }

    public Optional<Pipelet> find(String code) {
        return Optional.ofNullable(pipeletsByCode.get(code));
    }

    public Pipelet require(String code) {
        return find(code).orElseThrow(() -> new PipelineConfigurationException("No pipelet implementation for: " + code));
    }

    public boolean hasImplementation(String code) {
        return pipeletsByCode.containsKey(code);
    }
}
