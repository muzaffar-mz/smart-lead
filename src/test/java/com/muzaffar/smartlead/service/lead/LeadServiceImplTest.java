package com.muzaffar.smartlead.service.lead;

import com.muzaffar.smartlead.domain.entity.Lead;
import com.muzaffar.smartlead.domain.entity.enums.LeadType;
import com.muzaffar.smartlead.domain.entity.enums.LeadUrgency;
import com.muzaffar.smartlead.domain.repository.LeadRepository;
import com.muzaffar.smartlead.exceptions.NotFoundException;
import com.muzaffar.smartlead.service.hgface.model.RawLeadClassificationDTO;
import com.muzaffar.smartlead.service.lead.impl.LeadServiceImpl;
import com.muzaffar.smartlead.service.lead.model.LeadDTO;
import com.muzaffar.smartlead.service.lead.model.LeadFilter;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceImplTest {

    @Mock
    private LeadRepository leadRepository;
    private LeadServiceImpl underTest;

    @BeforeEach
    void setUp() {
        underTest = new LeadServiceImpl(leadRepository);
    }

    @Captor
    private ArgumentCaptor<Lead> leadCaptor;
    @Captor
    private ArgumentCaptor<Specification<Lead>> specCaptor;

    private static RawLeadClassificationDTO classification(boolean isLead, String type, String urgency) {
        return new RawLeadClassificationDTO(isLead, type, "Pricing question", urgency, "Wants a quote");
    }

    @Test
    void submitLeadAsyncSavesTheLeadWithMappedEnums() {

        //given
        var type = "pricing-inquiry";
        var urgency = "high";
        var title = "Pricing question";
        var summary = "Wants a quote";
        var leadRaw = new RawLeadClassificationDTO(true, type, title, urgency, summary);
        long messageId = 5L;


        //when
        underTest.submitLeadAsync(messageId, leadRaw);

        //then
        verify(leadRepository).save(leadCaptor.capture());
        var capturedValue = leadCaptor.getValue();
        assertThat(capturedValue.getMessageId()).isEqualTo(messageId);
        assertThat(capturedValue.getTitle()).isEqualTo("Pricing question");
        assertThat(capturedValue.getType()).isEqualTo(LeadType.PRICING_INQUIRY);
        assertThat(capturedValue.getUrgency()).isEqualTo(LeadUrgency.HIGH);
        assertThat(capturedValue.getSummary()).isEqualTo("Wants a quote");

    }

    @Test
    void submitLeadAsyncSavesNothingWhenTheMessageIsNotALead() {
        //given
        var type = "pricing-inquiry";
        var urgency = "high";
        var title = "Pricing question";
        var summary = "Wants a quote";
        var leadRaw = new RawLeadClassificationDTO(false, type, title, urgency, summary);
        long messageId = 5L;

        //when
        underTest.submitLeadAsync(messageId, leadRaw);

        //then
        verifyNoInteractions(leadRepository);
    }

    @Test
    void submitLeadAsyncSwallowsUnknownEnumValuesWithoutSaving() {
        //given
        var type = "unknown-type";
        var urgency = "high";
        var title = "Pricing question";
        var summary = "Wants a quote";
        var leadRaw = new RawLeadClassificationDTO(true, type, title, urgency, summary);
        long messageId = 5L;

        //when
        //then
        assertThatCode(() -> underTest.submitLeadAsync(messageId, leadRaw))
                .doesNotThrowAnyException();

        verify(leadRepository, never()).save(any());
    }

    @Test
    void submitLeadAsyncSwallowsRepositoryFailures() {
        //given
        var type = "pricing-inquiry";
        var urgency = "high";
        var title = "Pricing question";
        var summary = "Wants a quote";
        var leadRaw = new RawLeadClassificationDTO(true, type, title, urgency, summary);
        long messageId = 5L;

        when(leadRepository.save(any())).thenThrow(new IllegalStateException("db down"));

        //when
        //then
        assertThatCode(() -> underTest.submitLeadAsync(messageId, leadRaw))
                .doesNotThrowAnyException();
    }

    @Test
    void getByIdReturnsTheLeadAsDto() {

        //given
        var id = 3L;
        var lead = createEntity(id);

        when(leadRepository.findById(id)).thenReturn(Optional.of(lead));

        //when
        var actual = underTest.getById(id);
        var expected = LeadDTO.from(lead);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getByIdThrowsNotFoundForUnknownId() {
        //when
        Long id = 15L;
        when(leadRepository.findById(id)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> underTest.getById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }



    @Test
    void findUsesTheFilterPagingAndMapsToDtos() {
        //given
        var id = 123L;
        var lead = createEntity(id);

        var filter = new LeadFilter(null, null, null, null, null, 0, 20, "urgency", "desc");
        when(leadRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lead)));


        //when
        var result = underTest.find(filter);

        var actual = result.getContent();
        var expected = List.of(LeadDTO.from(lead));

        //then
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(leadRepository).findAll(any(Specification.class), pageableCaptor.capture());
        var capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable).isEqualTo(filter.toPageable());
        assertThat(result.getContent()).hasSize(1);
        assertThat(actual).isEqualTo(expected);
    }

    private Lead createEntity(long id) {
        var lead = new Lead();
        lead.setId(id);
        lead.setType(LeadType.SUPPORT);
        lead.setUrgency(LeadUrgency.LOW);
        return lead;
    }
}
