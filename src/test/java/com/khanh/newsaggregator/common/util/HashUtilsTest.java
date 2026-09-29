package com.khanh.newsaggregator.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilsTest {

    @Test
    void sha256_shouldReturn64CharsHex() {
        String url = "https://vnexpress.net/thoi-su/bai-viet-mau-12345.html";
        String hash = HashUtils.sha256(url);

        assertNotNull(hash);
        assertEquals(64, hash.length());
        // Verify deterministic output
        assertEquals(hash, HashUtils.sha256(url));
    }

    @Test
    void sha256_differentInputs_shouldProduceDifferentHashes() {
        String hash1 = HashUtils.sha256("https://vnexpress.net/tin-1");
        String hash2 = HashUtils.sha256("https://vnexpress.net/tin-2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void sha256_nullInput_shouldReturnNull() {
        assertNull(HashUtils.sha256(null));
    }
}
