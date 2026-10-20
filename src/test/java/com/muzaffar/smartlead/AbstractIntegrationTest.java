package com.muzaffar.smartlead;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.Lead;
import com.muzaffar.smartlead.domain.entity.Message;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import com.muzaffar.smartlead.domain.entity.enums.LeadType;
import com.muzaffar.smartlead.domain.entity.enums.LeadUrgency;
import com.muzaffar.smartlead.domain.repository.HfCompletionRepository;
import com.muzaffar.smartlead.domain.repository.LeadRepository;
import com.muzaffar.smartlead.domain.repository.MessageRepository;
import com.muzaffar.smartlead.service.hgface.client.HuggingFaceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Starts the whole application against a Testcontainers Postgres and calls it through MockMvc, so requests go
 * through the real security filters, validation, controllers, services, repositories and Liquibase schema.
 * Only the outside world is faked: the Hugging Face client is a mock.
 * <p>
 * The tests are not transactional: the async code saves rows from other threads, which could not see
 * data that is still inside a test transaction. The tables are emptied before every test instead.
 */
@SpringBootTest(properties = {
        "security.api-key=" + AbstractIntegrationTest.API_KEY,
        "security.test-key=" + AbstractIntegrationTest.TEST_KEY,
        "spring.security.user.name=" + AbstractIntegrationTest.SWAGGER_USER,
        "spring.security.user.password=" + AbstractIntegrationTest.SWAGGER_PASSWORD,
        "hugging-face.token=test-token"
})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest extends AbstractTestcontainers {

    public static final String API_KEY = "test-api-key";
    public static final String TEST_KEY = "test-test-key";
    public static final String SWAGGER_USER = "test-user";
    public static final String SWAGGER_PASSWORD = "test-password";

    protected static final String API_KEY_HEADER = "X-API-KEY";
    protected static final String TEST_KEY_HEADER = "X-TEST-KEY";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected MessageRepository messageRepository;
    @Autowired
    protected LeadRepository leadRepository;
    @Autowired
    protected HfCompletionRepository completionRepository;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor asyncExecutor;

    @MockitoBean
    protected HuggingFaceClient huggingFaceClient;

    /**
     * Before: the previous test class may have left rows. After: the container is shared with the repository tests,
     * which expect empty tables, and these tests commit their data.
     */
    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        waitForAsyncTasks();
        // one statement, so no row can slip in between deleting the children and the messages
        jdbcTemplate.execute("truncate table hf_completions, leads, messages restart identity cascade");
    }

    /** submitMessageAsync and submitLeadAsync save rows from the async executor, possibly after the request returned. */
    protected void waitForAsyncTasks() {
        var pool = ((ThreadPoolTaskExecutor) asyncExecutor).getThreadPoolExecutor();
        await().atMost(Duration.ofSeconds(10))
                .until(() -> pool.getActiveCount() == 0 && pool.getQueue().isEmpty());
    }

    protected static MockHttpServletRequestBuilder authorizedGet(String url, Object... uriVars) {
        return get(url, uriVars).header(API_KEY_HEADER, API_KEY);
    }

    protected static MockHttpServletRequestBuilder authorizedPost(String url, String json) {
        return post(url)
                .header(API_KEY_HEADER, API_KEY)
                .contentType("application/json")
                .content(json);
    }

    protected Message saveMessage(String email, String phone, String ip, String text) {
        var message = new Message();
        message.setEmail(email);
        message.setPhone(phone);
        message.setIp(ip);
        message.setMessage(text);
        return messageRepository.save(message);
    }

    protected Lead saveLead(long messageId, LeadType type, LeadUrgency urgency) {
        var lead = new Lead();
        lead.setMessageId(messageId);
        lead.setTitle("Lead for message " + messageId);
        lead.setType(type);
        lead.setUrgency(urgency);
        lead.setSummary("Summary " + messageId);
        return leadRepository.save(lead);
    }

    protected HfCompletion saveCompletion(long messageId, CompletionStatus status, String text) {
        var completion = new HfCompletion();
        completion.setMessageId(messageId);
        completion.setStatus(status);
        completion.setMessage(text);
        return completionRepository.save(completion);
    }

    /** date_created is set on insert, so tests move it with SQL to get older rows. */
    protected void setDateCreated(String table, long id, LocalDateTime dateCreated) {
        jdbcTemplate.update("update " + table + " set date_created = ? where id = ?", dateCreated, id);
    }
}
