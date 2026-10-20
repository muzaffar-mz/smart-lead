package com.muzaffar.smartlead.service.test;

public interface TestService {

    /**
     * Submits {@code count} messages with a random contact. The texts are the phrases of test-phrases.txt in random order,
     * each used once before any is used again when {@code count} is larger than the number of phrases.
     *
     * @return the number of submitted messages
     */
    int submitTestMessages(int count, String remoteAddr);
}
