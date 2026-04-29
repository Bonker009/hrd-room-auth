package org.kshrd.hrdroomservice.security.email;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DisposableEmailDomainBlocklist {

    private static final String RESOURCE_PATH = "security/disposable-email-domains.txt";
    private Set<String> disposableDomains = Set.of();

    @PostConstruct
    void load() {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        if (!resource.exists()) {
            log.warn("Disposable email domain list is missing at classpath:{}", RESOURCE_PATH);
            disposableDomains = Set.of();
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String raw = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            disposableDomains =
                    raw.lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty())
                            .filter(line -> !line.startsWith("#"))
                            .map(this::normalizeDomain)
                            .collect(Collectors.toUnmodifiableSet());
            log.info("Loaded {} disposable email domains", disposableDomains.size());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load disposable email domains", ex);
        }
    }

    public boolean isDisposable(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return false;
        }
        String domain = normalizeDomain(email.substring(at + 1));
        return disposableDomains.contains(domain);
    }

    private String normalizeDomain(String domain) {
        return domain.toLowerCase(Locale.ROOT);
    }
}
