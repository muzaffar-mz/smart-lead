package com.muzaffar.smartlead.service.message;

import com.muzaffar.smartlead.domain.entity.Message;
import com.muzaffar.smartlead.domain.repository.MessageRepository;
import com.muzaffar.smartlead.exceptions.NotFoundException;
import com.muzaffar.smartlead.model.request.MessageRequest;
import com.muzaffar.smartlead.service.hgface.HuggingFaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private HuggingFaceService hfService;

    private MessageService underTest;

    @BeforeEach
    void setUp() {
        underTest = new MessageService(messageRepository, hfService);
    }

    @Captor
    private ArgumentCaptor<Message> messageCaptor;
    @Captor
    private ArgumentCaptor<Specification<Message>> specCaptor;
    @Captor
    private ArgumentCaptor<MessageDTO> dtoCaptor;

    private void stubSave(long id) {
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            saved.setId(id);
            return saved;
        });
    }

    @Test
    void createMessageStoresEmailContactAsEmail() {

        //given
        var email = "john@acme.com";
        var message = "Need a demo";
        var messageRequest = new MessageRequest("   " + email + "   ", message);
        var ip = "10.0.0.1";
        var id = 42L;

        when(messageRepository.save(any())).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            saved.setId(id);
            return saved;
        });

        //when
        underTest.createMessage(messageRequest, ip);

        //then
        verify(messageRepository).save(messageCaptor.capture());
        var capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getId()).isEqualTo(id);
        assertThat(capturedMessage.getEmail()).isEqualTo(email);
        assertThat(capturedMessage.getPhone()).isNull();
        assertThat(capturedMessage.getIp()).isEqualTo(ip);
        assertThat(capturedMessage.getMessage()).isEqualTo(message);
    }

    @Test
    void createMessageStoresPhoneContactNormalized() {

        //given
        var contact = "+1 (555) 014-2000";
        var message = "Call me";
        var messageRequest = new MessageRequest(contact, message);
        var ip = "10.0.0.2";
        var id = 234L;

        stubSave(id);


        //when
        underTest.createMessage(messageRequest, ip);

        //then
        verify(messageRepository).save(messageCaptor.capture());
        var capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getId()).isEqualTo(id);
        assertThat(capturedMessage.getEmail()).isNull();
        assertThat(capturedMessage.getPhone()).isEqualTo("+15550142000");
        assertThat(capturedMessage.getIp()).isEqualTo(ip);
        assertThat(capturedMessage.getMessage()).isEqualTo(message);
    }

    @Test
    void createMessageHandsTheSavedMessageToTheHuggingFaceService() {
        //given
        var email = "john@acme.com";
        var message = "Need a demo";
        var messageRequest = new MessageRequest("   " + email + "   ", message);
        var ip = "10.0.0.1";
        var id = 15L;
        stubSave(id);

        //when
        underTest.createMessage(messageRequest, ip);

        //then
        verify(hfService).submitMessageAsync(dtoCaptor.capture());
        var capturedDTO = dtoCaptor.getValue();
        assertThat(capturedDTO.id()).isEqualTo(id);
        assertThat(capturedDTO.email()).isEqualTo(email);
        assertThat(capturedDTO.phone()).isNull();
        assertThat(capturedDTO.message()).isEqualTo(message);
    }

    @Test
    void canGetReturnsTheMessageAsDto() {
        //given
        long id = 10;
        var mail = "john@acme.com";
        var message = "HELLO";
        var entity = createEntity(id, mail, message);

        when(messageRepository.findById(id)).thenReturn(Optional.of(entity));

        //when
        var actual = underTest.get(id);

        var expected = MessageDTO.from(entity);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void willThrowWhenGetMessageReturnsEmptyOptional() {
        //given
        Long id = 99L;
        when(messageRepository.findById(id)).thenReturn(Optional.empty());

        //when
        //then

        assertThatThrownBy(() -> underTest.get(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void findUsesTheFilterPagingAndMapsToDtos() {
        //when
        var id = 1L;
        var entity = createEntity(id, null, null);
        var filter = new MessageFilter(null, null, null, null, null, null, 1, 10, "id", "desc");
        when(messageRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        //when
        var result = underTest.find(filter);

        var actual = result.getContent();
        var expected = List.of(MessageDTO.from(entity));

        //then
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findAll(any(Specification.class), pageableCaptor.capture());
        var capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable).isEqualTo(filter.toPageable());
        assertThat(result.getContent()).extracting(MessageDTO::id).containsExactly(1L);
        assertThat(actual).isEqualTo(expected);

    }


    private Message createEntity(long id, String mail, String message) {
        var entity = new Message();
        entity.setId(id);
        entity.setEmail(mail);
        entity.setMessage(message);
        return entity;
    }
}
