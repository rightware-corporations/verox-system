package com.rightware.verox.webhook.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

@Component
public class WebhookDestinationPolicy {

    private final boolean production;
    private final boolean allowLocalDevelopment;

    public WebhookDestinationPolicy(
        Environment environment,
        @Value("${verox.webhook.destination.allow-local-development:false}")
        boolean allowLocalDevelopment
    ) {
        this.production = environment.acceptsProfiles(Profiles.of("production"));
        this.allowLocalDevelopment = !production && allowLocalDevelopment;
    }

    public String validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("Webhook URL is required");
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Webhook URL is invalid", exception);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null || host == null) {
            throw new IllegalArgumentException("Webhook URL must be absolute");
        }

        scheme = scheme.toLowerCase(Locale.ROOT);

        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Webhook URL must use HTTP(S)");
        }

        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Webhook URL must not contain user information");
        }

        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("Webhook URL must not contain a fragment");
        }

        if (production && !scheme.equals("https")) {
            throw new IllegalArgumentException("Webhook URL must use HTTPS in production");
        }

        if (!production && !allowLocalDevelopment && !scheme.equals("https")) {
            throw new IllegalArgumentException(
                "HTTP webhook URLs require the explicit local-development exception"
            );
        }

        if (allowLocalDevelopment) {
            return uri.toString();
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);

        if (normalizedHost.equals("localhost")
            || normalizedHost.endsWith(".localhost")) {
            throw new IllegalArgumentException("Webhook destination is not public");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Webhook destination could not be resolved", exception);
        }

        if (addresses.length == 0) {
            throw new IllegalArgumentException("Webhook destination could not be resolved");
        }

        for (InetAddress address : addresses) {
            if (isForbiddenAddress(address)) {
                throw new IllegalArgumentException("Webhook destination is not public");
            }
        }

        return uri.toString();
    }

    static boolean isForbiddenAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();

        if (address instanceof Inet4Address) {
            return isForbiddenIpv4(bytes);
        }

        if (bytes.length == 16) {
            // IPv4-mapped IPv6 ::ffff:a.b.c.d
            boolean mapped = true;
            for (int index = 0; index < 10; index++) {
                if (bytes[index] != 0) {
                    mapped = false;
                    break;
                }
            }

            if (mapped && bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff) {
                return isForbiddenIpv4(new byte[] {
                    bytes[12], bytes[13], bytes[14], bytes[15]
                });
            }

            int first = unsigned(bytes[0]);
            int second = unsigned(bytes[1]);

            // fc00::/7 — IPv6 Unique Local Address.
            if ((first & 0xfe) == 0xfc) {
                return true;
            }

            // 2001:db8::/32 — documentation range.
            if (first == 0x20
                && second == 0x01
                && unsigned(bytes[2]) == 0x0d
                && unsigned(bytes[3]) == 0xb8) {
                return true;
            }
        }

        return false;
    }

    private static boolean isForbiddenIpv4(byte[] bytes) {
        int a = unsigned(bytes[0]);
        int b = unsigned(bytes[1]);
        int c = unsigned(bytes[2]);

        return
            a == 0
            || a == 10
            || a == 127
            || (a == 100 && b >= 64 && b <= 127)
            || (a == 169 && b == 254)
            || (a == 172 && b >= 16 && b <= 31)
            || (a == 192 && b == 0 && c == 0)
            || (a == 192 && b == 0 && c == 2)
            || (a == 192 && b == 168)
            || (a == 198 && (b == 18 || b == 19))
            || (a == 198 && b == 51 && c == 100)
            || (a == 203 && b == 0 && c == 113)
            || a >= 224;
    }

    private static int unsigned(byte value) {
        return value & 0xff;
    }
}