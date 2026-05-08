package org.kshrd.hrdroomservice.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver(new ObservabilityProperties(List.of()));
    }

    @Nested
    class Resolve {

        @Test
        void trustedPeer_usesCfConnectingIp() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("127.0.0.1");
            r.addHeader("CF-Connecting-IP", "1.2.3.4");
            r.addHeader("X-Forwarded-For", "9.9.9.9");
            assertThat(resolver.resolve(r)).isEqualTo("1.2.3.4");
        }

        @Test
        void trustedPeer_usesTrueClientIpWhenCfAbsent() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("10.0.0.1");
            r.addHeader("True-Client-IP", "5.6.7.8");
            r.addHeader("X-Forwarded-For", "9.9.9.9");
            assertThat(resolver.resolve(r)).isEqualTo("5.6.7.8");
        }

        @Test
        void trustedPeer_usesForwardedHeader() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("192.168.1.1");
            r.addHeader("Forwarded", "proto=https;for=192.0.2.60;by=203.0.113.43");
            assertThat(resolver.resolve(r)).isEqualTo("192.0.2.60");
        }

        @Test
        void trustedPeer_usesXForwardedForFirstHop() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("172.20.0.5");
            r.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
            assertThat(resolver.resolve(r)).isEqualTo("203.0.113.10");
        }

        @Test
        void trustedPeer_usesXRealIpWhenOthersAbsent() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("127.0.0.1");
            r.addHeader("X-Real-IP", "198.51.100.2");
            assertThat(resolver.resolve(r)).isEqualTo("198.51.100.2");
        }

        @Test
        void untrustedPeer_ignoresForgedXForwardedFor() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("198.51.100.77");
            r.addHeader("X-Forwarded-For", "8.8.8.8");
            assertThat(resolver.resolve(r)).isEqualTo("198.51.100.77");
        }

        @Test
        void trustedPeer_ipv6BracketRemote_stripsBracketsForMatcher() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("[::1]");
            r.addHeader("X-Forwarded-For", "2001:db8::1");
            assertThat(resolver.resolve(r)).isEqualTo("2001:db8::1");
        }

        @Test
        void noForwardedHeaders_fallsBackToRemoteAddr() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("198.51.100.99");
            assertThat(resolver.resolve(r)).isEqualTo("198.51.100.99");
        }

        @Test
        void trustedPeer_xForwardedFor_stripsPortFromIpv4() {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.setRemoteAddr("127.0.0.1");
            r.addHeader("X-Forwarded-For", "192.0.2.1:12345");
            assertThat(resolver.resolve(r)).isEqualTo("192.0.2.1");
        }
    }

    @Nested
    class ParseForwardedFor {

        @Test
        void extractsQuotedIpv6WithPort() {
            assertThat(ClientIpResolver.parseForwardedFor("for=\"[2001:db8:cafe::17]:4711\""))
                    .isEqualTo("2001:db8:cafe::17");
        }

        @Test
        void extractsFirstForParam() {
            assertThat(ClientIpResolver.parseForwardedFor("for=192.0.2.43, for=198.51.100.77"))
                    .isEqualTo("192.0.2.43");
        }

        @Test
        void returnsNullWhenMissing() {
            assertThat(ClientIpResolver.parseForwardedFor("proto=https;by=203.0.113.60")).isNull();
        }
    }
}
