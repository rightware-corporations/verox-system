package com.rightware.verox.authentication.web;

import com.rightware.verox.authentication.application.ApiKeyService;
import com.rightware.verox.authentication.application.MerchantPrincipal;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiKeyAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidBearerApiKey() throws Exception {
        ApiKeyService service = mock(ApiKeyService.class);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(service);
        FilterChain chain = mock(FilterChain.class);

        MerchantPrincipal principal = new MerchantPrincipal(
            UUID.randomUUID(),
            "Event Merchant",
            UUID.randomUUID(),
            ApiKeyEnvironment.LIVE
        );

        when(service.authenticate("vx_live_valid")).thenReturn(Optional.of(principal));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/account");
        request.addHeader("Authorization", "Bearer vx_live_valid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsInvalidApiKeyWith401() throws Exception {
        ApiKeyService service = mock(ApiKeyService.class);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(service);
        FilterChain chain = mock(FilterChain.class);

        when(service.authenticate("vx_live_invalid")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/account");
        request.addHeader("Authorization", "Bearer vx_live_invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_API_KEY");
        verify(chain, never()).doFilter(any(), any());
    }
}
