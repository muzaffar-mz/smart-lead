package com.muzaffar.smartlead;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "security.api-key=test-api-key",
        "spring.security.user.name=test-user",
        "spring.security.user.password=test-password",
        "hugging-face.token=test-token"
})
class SmartLeadApplicationTests extends AbstractTestcontainers {

    @Test
    void contextLoads() {
    }

}
