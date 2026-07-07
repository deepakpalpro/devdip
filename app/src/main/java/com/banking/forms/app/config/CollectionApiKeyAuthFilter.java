package com.banking.forms.app.config;

import com.banking.forms.bff.collection.api.CollectionQueryController;
import com.banking.forms.collection.application.CollectionApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates {@code X-Api-Key} for external collection endpoints and binds the tenant. */
@Component
public class CollectionApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final CollectionApiKeyService apiKeyService;

    public CollectionApiKeyAuthFilter(CollectionApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/collection/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        UUID tenantId = apiKeyService.authenticate(apiKey).orElse(null);
        if (tenantId == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or missing collection API key");
            return;
        }
        request.setAttribute(CollectionQueryController.TENANT_ATTRIBUTE, tenantId);
        filterChain.doFilter(request, response);
    }
}
