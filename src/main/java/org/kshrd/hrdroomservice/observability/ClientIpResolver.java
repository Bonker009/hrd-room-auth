package org.kshrd.hrdroomservice.observability;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final Pattern FORWARDED_FOR_PARAM =
            Pattern.compile("(?i)for\\s*=\\s*(\"[^\"]+\"|[^;,\\s]+)");

    private final List<IpAddressMatcher> trustedMatchers;

    public ClientIpResolver(ObservabilityProperties properties) {
        List<IpAddressMatcher> matchers = new ArrayList<>();
        for (String cidr : properties.trustedProxyCidrs()) {
            matchers.add(new IpAddressMatcher(cidr));
        }
        this.trustedMatchers = List.copyOf(matchers);
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (isTrustedPeer(remoteAddr)) {
            String fromHeader = firstNonBlankHeader(request, "CF-Connecting-IP", "True-Client-IP");
            if (fromHeader != null) {
                return fromHeader;
            }
            String forwarded = parseForwardedFor(request.getHeader("Forwarded"));
            if (forwarded != null) {
                return forwarded;
            }
            String xff = firstIpFromXForwardedFor(request.getHeader("X-Forwarded-For"));
            if (xff != null) {
                return xff;
            }
            String realIp = normalizeIp(request.getHeader("X-Real-IP"));
            if (isUsableIp(realIp)) {
                return realIp;
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedPeer(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        for (IpAddressMatcher matcher : trustedMatchers) {
            if (matcher.matches(remoteAddr)) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlankHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String v = request.getHeader(name);
            if (isUsableIp(v)) {
                return normalizeIp(v.trim());
            }
        }
        return null;
    }

    private static String firstIpFromXForwardedFor(String xff) {
        if (xff == null || xff.isBlank()) {
            return null;
        }
        String first = xff.split(",")[0].trim();
        if (first.isEmpty() || "unknown".equalsIgnoreCase(first)) {
            return null;
        }
        // Strip port if present: "1.2.3.4:12345" or "[::1]:12345"
        if (first.startsWith("[")) {
            int endBracket = first.indexOf(']');
            if (endBracket > 0) {
                return normalizeIp(first.substring(1, endBracket));
            }
        }
        int colon = first.indexOf(':');
        if (colon > 0 && first.indexOf(':') == first.lastIndexOf(':')) {
            // single colon -> likely IPv4:port
            return normalizeIp(first.substring(0, colon));
        }
        return normalizeIp(first);
    }

    static String parseForwardedFor(String forwarded) {
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }
        Matcher m = FORWARDED_FOR_PARAM.matcher(forwarded);
        if (!m.find()) {
            return null;
        }
        String raw = m.group(1).trim();
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
            raw = raw.substring(1, raw.length() - 1);
        }
        raw = raw.trim();
        if (raw.startsWith("[")) {
            int endBracket = raw.indexOf(']');
            if (endBracket > 0) {
                raw = raw.substring(1, endBracket);
            }
        } else {
            int colon = raw.lastIndexOf(':');
            if (colon > 0 && raw.indexOf(':') == colon) {
                raw = raw.substring(0, colon);
            }
        }
        if (!isUsableIp(raw)) {
            return null;
        }
        return normalizeIp(raw);
    }

    private static boolean isUsableIp(String value) {
        if (value == null) {
            return false;
        }
        String t = value.trim();
        return !t.isEmpty() && !"unknown".equalsIgnoreCase(t);
    }

    private static String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }
        String t = ip.trim();
        if (t.startsWith("[") && t.endsWith("]") && t.length() > 2) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }
}
