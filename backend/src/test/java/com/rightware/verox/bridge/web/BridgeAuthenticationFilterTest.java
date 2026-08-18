package com.rightware.verox.bridge.web;

import com.rightware.verox.authentication.domain.ApiKeyEnvironment;
import com.rightware.verox.bridge.application.BridgePrincipal;
import com.rightware.verox.bridge.application.BridgeService;
import com.rightware.verox.common.ratelimit.RateLimitExceededException;
import com.rightware.verox.common.ratelimit.RateLimitGuard;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BridgeAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidBridgeAndConsumesBridgeQuota()
        throws Exception {

        BridgeService bridgeService =
            mock(BridgeService.class);

        RateLimitGuard rateLimitGuard =
            mock(RateLimitGuard.class);

        BridgeAuthenticationFilter filter =
            new BridgeAuthenticationFilter(
                bridgeService,
                rateLimitGuard
            );

        FilterChain chain = mock(FilterChain.class);

        BridgePrincipal principal =
            principal();

        when(
            bridgeService.authenticate("vx_bridge_valid")
        ).thenReturn(Optional.of(principal));

        MockHttpServletRequest request =
            bridgeRequest();

        request.addHeader(
            "Authorization",
            "Bearer vx_bridge_valid"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        doAnswer(invocation -> {
            assertThat(
                SecurityContextHolder
                    .getContext()
                    .getAuthentication()
            ).isNotNull();

            assertThat(
                SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal()
            ).isEqualTo(principal);

            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        verify(rateLimitGuard)
            .checkBridge(principal.bridgeId());

        verify(chain).doFilter(any(), any());

        assertThat(
            SecurityContextHolder
                .getContext()
                .getAuthentication()
        ).isNull();
    }

    @Test
    void invalidBridgeCredentialDoesNotConsumeLegitimateQuota()
        throws Exception {

        BridgeService bridgeService =
            mock(BridgeService.class);

        RateLimitGuard rateLimitGuard =
            mock(RateLimitGuard.class);

        BridgeAuthenticationFilter filter =
            new BridgeAuthenticationFilter(
                bridgeService,
                rateLimitGuard
            );

        FilterChain chain = mock(FilterChain.class);

        when(
            bridgeService.authenticate("vx_bridge_invalid")
        ).thenReturn(Optional.empty());

        MockHttpServletRequest request =
            bridgeRequest();

        request.addHeader(
            "Authorization",
            "Bearer vx_bridge_invalid"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus())
            .isEqualTo(401);

        assertThat(response.getContentAsString())
            .contains("INVALID_BRIDGE_CREDENTIAL");

        verify(rateLimitGuard, never())
            .checkBridge(any());

        verify(chain, never())
            .doFilter(any(), any());
    }

    @Test
    void rateLimitedBridgeIsRejectedBeforeController()
        throws Exception {

        BridgeService bridgeService =
            mock(BridgeService.class);

        RateLimitGuard rateLimitGuard =
            mock(RateLimitGuard.class);

        BridgeAuthenticationFilter filter =
            new BridgeAuthenticationFilter(
                bridgeService,
                rateLimitGuard
            );

        FilterChain chain = mock(FilterChain.class);

        BridgePrincipal principal =
            principal();

        when(
            bridgeService.authenticate("vx_bridge_valid")
        ).thenReturn(Optional.of(principal));

        doThrow(
            new RateLimitExceededException(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Try again later.",
                19
            )
        ).when(rateLimitGuard)
            .checkBridge(principal.bridgeId());

        MockHttpServletRequest request =
            bridgeRequest();

        request.addHeader(
            "Authorization",
            "Bearer vx_bridge_valid"
        );

        MockHttpServletResponse response =
            new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus())
            .isEqualTo(429);

        assertThat(
            response.getHeader("Retry-After")
        ).isEqualTo("19");

        assertThat(response.getContentAsString())
            .contains("RATE_LIMIT_EXCEEDED");

        verify(chain, never())
            .doFilter(any(), any());

        assertThat(
            SecurityContextHolder
                .getContext()
                .getAuthentication()
        ).isNull();
    }

    private MockHttpServletRequest bridgeRequest() {
        return new MockHttpServletRequest(
            "POST",
            "/v1/bridges/brg_test/evidence"
        );
    }

    private BridgePrincipal principal() {
        return new BridgePrincipal(
            UUID.randomUUID(),
            "brg_test",
            UUID.randomUUID(),
            ApiKeyEnvironment.TEST,
            "MPESA"
        );
    }
}