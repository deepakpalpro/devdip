package com.banking.forms.bff.consumer.api;

import com.banking.forms.discovery.application.DiscoveryEvaluationView;
import com.banking.forms.discovery.application.DiscoveryNotFoundException;
import com.banking.forms.discovery.application.DiscoveryService;
import com.banking.forms.discovery.application.QuestionnaireView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Consumer-facing Form Discovery Wizard endpoints: serve the preliminary questionnaire and evaluate
 * a user's answers into ranked form recommendations (persisting a discovery session for later
 * pre-population).
 */
@RestController
@RequestMapping("/api/consumer/v1/discovery")
public class ConsumerDiscoveryController {

    private final DiscoveryService discoveryService;

    public ConsumerDiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/{code}")
    public QuestionnaireView getQuestionnaire(
            @RequestHeader("X-Tenant-Id") UUID tenantId, @PathVariable("code") String code) {
        try {
            return discoveryService.getQuestionnaire(tenantId, code);
        } catch (DiscoveryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/{code}/evaluate")
    public DiscoveryEvaluationView evaluate(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) String userIdHeader,
            @PathVariable("code") String code,
            @Valid @RequestBody EvaluateRequest request) {
        UUID userId = DevRequestContext.resolveUserId(userIdHeader);
        try {
            return discoveryService.evaluate(tenantId, userId, code, request.answers());
        } catch (DiscoveryNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    public record EvaluateRequest(@NotNull Map<String, Object> answers) {}
}
