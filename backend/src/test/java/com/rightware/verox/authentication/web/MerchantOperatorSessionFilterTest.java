package com.rightware.verox.authentication.web;

import com.rightware.verox.authentication.application.MerchantOperatorPrincipal;
import com.rightware.verox.authentication.application.MerchantOperatorSessionService;
import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantOperatorSessionFilterTest {
    @Test
    void mutationWithValidCsrfTokenReachesController() throws Exception {
        MerchantOperatorSessionService sessions = mock(MerchantOperatorSessionService.class);
        MerchantOperatorSessionFilter filter = new MerchantOperatorSessionFilter(sessions);
        when(sessions.authenticate("session")).thenReturn(Optional.of(authenticated()));
        when(sessions.csrfMatches("hash", "valid-token")).thenReturn(true);
        MockHttpServletRequest request = request("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void mutationWithoutCsrfTokenIsRejected() throws Exception {
        MerchantOperatorSessionService sessions = mock(MerchantOperatorSessionService.class);
        MerchantOperatorSessionFilter filter = new MerchantOperatorSessionFilter(sessions);
        when(sessions.authenticate("session")).thenReturn(Optional.of(authenticated()));
        when(sessions.csrfMatches("hash", null)).thenReturn(false);
        MockHttpServletRequest request = request(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void mutationWithInvalidCsrfTokenIsRejected() throws Exception {
        MerchantOperatorSessionService sessions = mock(MerchantOperatorSessionService.class);
        MerchantOperatorSessionFilter filter = new MerchantOperatorSessionFilter(sessions);
        when(sessions.authenticate("session")).thenReturn(Optional.of(authenticated()));
        when(sessions.csrfMatches("hash", "wrong-token")).thenReturn(false);
        MockHttpServletRequest request = request("wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    private MockHttpServletRequest request(String csrf) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/platform/v1/payments/pay_test/manual-rejection");
        request.setCookies(new jakarta.servlet.http.Cookie(MerchantOperatorSessionFilter.SESSION_COOKIE, "session"));
        if (csrf != null) request.addHeader(MerchantOperatorSessionFilter.CSRF_HEADER, csrf);
        return request;
    }

    private MerchantOperatorSessionService.AuthenticatedSession authenticated() {
        MerchantOperatorPrincipal principal = new MerchantOperatorPrincipal(UUID.randomUUID(), "Owen", UUID.randomUUID(), "Merchant", ApiKeyEnvironment.TEST, UUID.randomUUID());
        return new MerchantOperatorSessionService.AuthenticatedSession(principal, "hash", Instant.now().plusSeconds(3600));
    }
}
