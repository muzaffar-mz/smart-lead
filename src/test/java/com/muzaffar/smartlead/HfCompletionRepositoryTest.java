package com.muzaffar.smartlead;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.Message;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import com.muzaffar.smartlead.domain.repository.HfCompletionRepository;
import com.muzaffar.smartlead.domain.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Limit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HfCompletionRepositoryTest extends AbstractTestcontainers {

    @Autowired
    private HfCompletionRepository underTest;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByStatusOrderByIdAsc() {
        // GIVEN
        var completions = List.of(
                saveCompletion(CompletionStatus.PENDING, "random message one"),
                saveCompletion(CompletionStatus.PENDING, "random message two"),
                saveCompletion(CompletionStatus.PENDING, "random message three"),
                saveCompletion(CompletionStatus.PENDING, "random message four"),
                saveCompletion(CompletionStatus.PENDING, "random message five"));
        var expectedIds = completions.stream().map(HfCompletion::getId).toList();

        // Updating the first row makes Postgres write a new version of it at the end of the table.
        // Without ORDER BY the query would now return it last, so this proves the sorting.
        completions.getFirst().setMessage("random message one, updated");
        underTest.saveAndFlush(completions.getFirst());
        // Forget the saved objects, so the query below reads rows from the database
        // instead of handing back the same Java instances.
        entityManager.clear();

        //When
        var actual = underTest.findByStatusOrderByIdAsc(CompletionStatus.PENDING, Limit.of(30));

        //Then
        assertThat(actual).hasSize(completions.size());
        assertThat(actual).extracting(HfCompletion::getId).containsExactlyElementsOf(expectedIds);
        assertThat(actual).extracting(HfCompletion::getId).isSorted();
        assertThat(actual).extracting(HfCompletion::getStatus).containsOnly(CompletionStatus.PENDING);
        assertThat(actual).extracting(HfCompletion::getMessage).containsExactly(
                "random message one, updated",
                "random message two",
                "random message three",
                "random message four",
                "random message five");
        assertThat(actual).extracting(HfCompletion::getMessageId)
                .containsExactlyElementsOf(completions.stream().map(HfCompletion::getMessageId).toList());
        assertThat(actual).allSatisfy(c -> {
            assertThat(c.getDateCreated()).isNotNull();
            assertThat(c.getDateUpdated()).isNotNull();
        });
    }

    @Test
    void findByStatusReturnsOnlyTheRequestedStatus() {
        // Given
        var pendingOne = saveCompletion(CompletionStatus.PENDING, "pending one");
        saveCompletion(CompletionStatus.SUCCESS, "already processed");
        var pendingTwo = saveCompletion(CompletionStatus.PENDING, "pending two");
        saveCompletion(CompletionStatus.ERROR, "failed for good");
        flushAndClear();

        //When
        var pending = underTest.findByStatusOrderByIdAsc(CompletionStatus.PENDING, Limit.of(30));
        var success = underTest.findByStatusOrderByIdAsc(CompletionStatus.SUCCESS, Limit.of(30));
        var error = underTest.findByStatusOrderByIdAsc(CompletionStatus.ERROR, Limit.of(30));

        //Then
        assertThat(pending).extracting(HfCompletion::getId).containsExactly(pendingOne.getId(), pendingTwo.getId());
        assertThat(success).extracting(HfCompletion::getMessage).containsExactly("already processed");
        assertThat(error).extracting(HfCompletion::getMessage).containsExactly("failed for good");
    }

    @Test
    void findByStatusHonoursTheLimitAndKeepsTheOldestRows() {
        // Given
        var first = saveCompletion(CompletionStatus.PENDING, "one");
        var second = saveCompletion(CompletionStatus.PENDING, "two");
        saveCompletion(CompletionStatus.PENDING, "three");
        flushAndClear();

        //When
        var batch = underTest.findByStatusOrderByIdAsc(CompletionStatus.PENDING, Limit.of(2));

        //Then
        assertThat(batch).extracting(HfCompletion::getId).containsExactly(first.getId(), second.getId());
    }

    @Test
    void findByStatusPicksUpRowsAgainOnceTheyAreNoLongerInTheLimit() {
        // Given: the scheduler marks processed rows, so the next batch starts with the following rows
        var first = saveCompletion(CompletionStatus.PENDING, "one");
        var second = saveCompletion(CompletionStatus.PENDING, "two");
        var third = saveCompletion(CompletionStatus.PENDING, "three");
        flushAndClear();
        var firstBatch = underTest.findByStatusOrderByIdAsc(CompletionStatus.PENDING, Limit.of(2));
        firstBatch.forEach(c -> c.setStatus(CompletionStatus.SUCCESS));
        underTest.saveAllAndFlush(firstBatch);
        entityManager.clear();

        //When
        var secondBatch = underTest.findByStatusOrderByIdAsc(CompletionStatus.PENDING, Limit.of(2));

        //Then
        assertThat(firstBatch).extracting(HfCompletion::getId).containsExactly(first.getId(), second.getId());
        assertThat(secondBatch).extracting(HfCompletion::getId).containsExactly(third.getId());
    }

    @Test
    void findByStatusReturnsAnEmptyListWhenNothingMatches() {
        // Given
        saveCompletion(CompletionStatus.SUCCESS, "already processed");
        flushAndClear();

        //When
        var actual = underTest.findByStatusOrderByIdAsc(CompletionStatus.PENDING, Limit.of(30));

        //Then
        assertThat(actual).isEmpty();
    }

    private HfCompletion saveCompletion(CompletionStatus status, String text) {
        var message = messageRepository.save(createMessage(FAKER.internet().safeEmailAddress(), text));

        var completion = new HfCompletion();
        completion.setMessageId(message.getId());
        completion.setStatus(status);
        completion.setMessage(text);
        return underTest.save(completion);
    }

    private Message createMessage(String mail, String message) {
        var entity = new Message();
        entity.setEmail(mail);
        entity.setMessage(message);
        return entity;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
