package com.muzaffar.smartlead.service.test;

import com.muzaffar.smartlead.model.request.MessageRequest;
import com.muzaffar.smartlead.service.message.MessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TestService {

    private static final List<String> FIRST_NAMES = List.of("john", "maria", "alex", "sara", "david", "olga", "ahmed", "emma", "li", "carlos", "nina", "tom");
    private static final List<String> LAST_NAMES = List.of("smith", "garcia", "ivanov", "khan", "miller", "chen", "silva", "brown", "novak", "lee", "rossi", "walker");
    private static final List<String> DOMAINS = List.of("acme.com", "globex.io", "initech.co", "northwind.org", "umbrella.net", "gmail.com", "outlook.com", "startup.io");
    private static final List<String> PHONE_FORMATS = List.of("+1%s%s%s", "+1 (%s) %s-%s", "(%s) %s-%s", "+44 %s %s %s", "%s-%s-%s", "+1.%s.%s.%s");

    private final MessageService messageService;
    private final Resource testPhrasesResource;

    public TestService(MessageService messageService,
                       @Value("classpath:hgface/test-phrases.txt") Resource testPhrasesResource) {
        this.messageService = messageService;
        this.testPhrasesResource = testPhrasesResource;
    }

    public int submitTestMessages(int count, String remoteAddr) {
        var phrases = readPhrases();
        if (phrases.isEmpty()) {
            return 0;
        }
        // every phrase is used once, in random order, before any is used again
        var order = new ArrayList<>(phrases);
        for (int i = 0; i < count; i++) {
            var position = i % order.size();
            if (position == 0) {
                Collections.shuffle(order);
            }
            messageService.createMessage(new MessageRequest(randomContact(), order.get(position)), remoteAddr);
        }
        return count;
    }

    private List<String> readPhrases() {
        try {
            return testPhrasesResource.getContentAsString(StandardCharsets.UTF_8)
                    .lines()
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the test phrases", e);
        }
    }

    private String randomContact() {
        return ThreadLocalRandom.current().nextBoolean() ? randomEmail() : randomPhone();
    }

    private String randomEmail() {
        var random = ThreadLocalRandom.current();
        var separator = random.nextBoolean() ? "." : "_";
        return pick(FIRST_NAMES) + separator + pick(LAST_NAMES) + random.nextInt(1, 100) + "@" + pick(DOMAINS);
    }

    private String randomPhone() {
        var random = ThreadLocalRandom.current();
        return pick(PHONE_FORMATS).formatted(
                "%03d".formatted(random.nextInt(200, 1000)),
                "%03d".formatted(random.nextInt(200, 1000)),
                "%04d".formatted(random.nextInt(10000)));
    }

    private static String pick(List<String> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }
}
