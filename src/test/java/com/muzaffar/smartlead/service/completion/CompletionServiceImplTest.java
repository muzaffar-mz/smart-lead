package com.muzaffar.smartlead.service.completion;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.UsageDetails;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import com.muzaffar.smartlead.domain.repository.HfCompletionRepository;
import com.muzaffar.smartlead.exceptions.NotFoundException;
import com.muzaffar.smartlead.service.completion.impl.CompletionServiceImpl;
import com.muzaffar.smartlead.service.completion.model.CompletionDTO;
import com.muzaffar.smartlead.service.completion.model.CompletionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CompletionServiceImplTest {

    @Mock
    private HfCompletionRepository completionRepository;
    private CompletionServiceImpl underTest;

    @BeforeEach
    void setUp() {
        underTest = new CompletionServiceImpl(completionRepository);
    }

    @Test
    void getByIdReturnsTheCompletionAsDto() {
        //given
        var id = 3L;
        var completion = createEntity(id, 7L);
        when(completionRepository.findById(id)).thenReturn(Optional.of(completion));

        //when
        var actual = underTest.getById(id);

        //then
        assertThat(actual).isEqualTo(CompletionDTO.from(completion));
    }

    @Test
    void getByIdThrowsNotFoundForUnknownId() {
        //given
        Long id = 15L;
        when(completionRepository.findById(id)).thenReturn(Optional.empty());

        //when
        //then
        assertThatThrownBy(() -> underTest.getById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getByMessageIdReturnsTheCompletionAsDto() {
        //given
        var messageId = 7L;
        var completion = createEntity(3L, messageId);
        when(completionRepository.findByMessageId(messageId)).thenReturn(Optional.of(completion));

        //when
        var actual = underTest.getByMessageId(messageId);

        //then
        assertThat(actual).isEqualTo(CompletionDTO.from(completion));
        assertThat(actual.messageId()).isEqualTo(messageId);
    }

    @Test
    void getByMessageIdThrowsNotFoundWhenTheMessageHasNoCompletion() {
        //given
        Long messageId = 99L;
        when(completionRepository.findByMessageId(messageId)).thenReturn(Optional.empty());

        //when
        //then
        assertThatThrownBy(() -> underTest.getByMessageId(messageId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(messageId.toString());
    }

    @Test
    void findUsesTheFilterPagingAndMapsToDtos() {
        //given
        var completion = createEntity(123L, 5L);
        var filter = new CompletionFilter(CompletionStatus.PENDING, null, null, null, 0, 20, "status", "desc");
        when(completionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(completion)));

        //when
        var result = underTest.find(filter);

        //then
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(completionRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(filter.toPageable());
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).isEqualTo(List.of(CompletionDTO.from(completion)));
    }

    @Test
    void dtoCarriesTheUsageDetails() {
        //given
        var completion = createEntity(1L, 2L);
        var usage = new UsageDetails();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(20);
        usage.setTotalTokens(30);
        completion.setUsage(usage);

        //when
        var actual = CompletionDTO.from(completion);

        //then
        assertThat(actual.usage().promptTokens()).isEqualTo(10);
        assertThat(actual.usage().completionTokens()).isEqualTo(20);
        assertThat(actual.usage().totalTokens()).isEqualTo(30);
    }

    @Test
    void dtoHasNoUsageWhenThereIsNone() {
        //given
        var completion = createEntity(1L, 2L);

        //when
        var actual = CompletionDTO.from(completion);

        //then
        assertThat(actual.usage()).isNull();
    }

    private HfCompletion createEntity(long id, long messageId) {
        var completion = new HfCompletion();
        completion.setId(id);
        completion.setMessageId(messageId);
        completion.setStatus(CompletionStatus.SUCCESS);
        completion.setMessage("Need a demo");
        return completion;
    }
}
