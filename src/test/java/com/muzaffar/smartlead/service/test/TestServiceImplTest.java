package com.muzaffar.smartlead.service.test;

import com.muzaffar.smartlead.model.request.MessageRequest;
import com.muzaffar.smartlead.service.message.MessageService;
import com.muzaffar.smartlead.service.test.impl.TestServiceImpl;
import com.muzaffar.smartlead.validation.ContactUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TestServiceImplTest {

    @Mock
    private MessageService messageService;
    @Captor
    private ArgumentCaptor<MessageRequest> requestCaptor;

    private TestServiceImpl serviceWith(String phrases) {
        return new TestServiceImpl(messageService, new ByteArrayResource(phrases.getBytes()));
    }

    @Test
    void submitsTheRequestedNumberOfMessagesWithDistinctPhrases() {
        //given
        var ip = "10.0.0.9";
        var underTest = serviceWith("first phrase\n\n   \nsecond phrase\nthird phrase\n");

        //when
        var count = underTest.submitTestMessages(2, ip);

        //then
        assertThat(count).isEqualTo(2);
        verify(messageService, times(2)).createMessage(requestCaptor.capture(), eq(ip));
        assertThat(requestCaptor.getAllValues()).extracting(MessageRequest::message)
                .doesNotHaveDuplicates()
                .isSubsetOf("first phrase", "second phrase", "third phrase");
    }

    @Test
    void usesEveryPhraseOnceBeforeRepeatingAny() {
        //given
        var underTest = serviceWith("one\ntwo\nthree\n");

        //when
        var count = underTest.submitTestMessages(7, "10.0.0.9");

        //then
        assertThat(count).isEqualTo(7);
        verify(messageService, times(7)).createMessage(requestCaptor.capture(), any());
        var messages = requestCaptor.getAllValues().stream().map(MessageRequest::message).toList();
        assertThat(messages.subList(0, 3)).containsExactlyInAnyOrder("one", "two", "three");
        assertThat(messages.subList(3, 6)).containsExactlyInAnyOrder("one", "two", "three");
        assertThat(messages.get(6)).isIn("one", "two", "three");
    }

    @Test
    void phrasesAreNotSubmittedInFileOrder() {
        //given
        var phrases = IntStream.rangeClosed(1, 30).mapToObj(i -> "phrase " + i).toList();
        var underTest = serviceWith(String.join("\n", phrases));

        //when
        underTest.submitTestMessages(30, "10.0.0.9");

        //then
        verify(messageService, times(30)).createMessage(requestCaptor.capture(), any());
        var messages = requestCaptor.getAllValues().stream().map(MessageRequest::message).toList();
        assertThat(messages).containsExactlyInAnyOrderElementsOf(phrases).isNotEqualTo(phrases);
    }

    @Test
    void everyContactIsAValidEmailOrPhone() {
        //given
        var underTest = serviceWith("a\n");

        //when
        underTest.submitTestMessages(200, "10.0.0.9");

        //then
        verify(messageService, times(200)).createMessage(requestCaptor.capture(), any());
        assertThat(requestCaptor.getAllValues()).extracting(MessageRequest::contact)
                .allSatisfy(contact -> assertThat(
                        ContactUtils.isEmail(contact) || ContactUtils.isPhone(contact)).isTrue());
    }

    @Test
    void submitsNothingForAnEmptyFile() {
        //given
        var underTest = serviceWith("\n  \n");

        //when
        var count = underTest.submitTestMessages(5, "10.0.0.9");

        //then
        assertThat(count).isZero();
        verify(messageService, never()).createMessage(any(), any());
    }
}
