package org.kshrd.hrdroomservice.security.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DisposableEmailDomainBlocklistTest {

    private DisposableEmailDomainBlocklist blocklist;

    @BeforeEach
    void setUp() {
        blocklist = new DisposableEmailDomainBlocklist();
        blocklist.load();
    }

    @Test
    void returnsTrueForDisposableDomains_caseInsensitive() {
        assertTrue(blocklist.isDisposable("foo@mailinator.com"));
        assertTrue(blocklist.isDisposable("Foo@MAILINATOR.COM"));
    }

    @Test
    void returnsFalseForLegitimateDomainsAndMalformedInput() {
        assertFalse(blocklist.isDisposable("foo@gmail.com"));
        assertFalse(blocklist.isDisposable("invalid-email"));
        assertFalse(blocklist.isDisposable(""));
        assertFalse(blocklist.isDisposable(null));
    }
}
