package com.muzaffar.smartlead.service.message.model;

import com.muzaffar.smartlead.service.message.MessageFilter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MessageFilterTest {

    private static MessageFilter filter(LocalDateTime from, LocalDateTime to) {
        return new MessageFilter(null, null, null, null, from, to, null, null, null, null);
    }

    @Test
    void withoutDatesCoversTheLast30Days() {

        //given

        //when
        var result = filter(null, null);

        //then
        assertThat(result.to()).isCloseTo(LocalDateTime.now(), within(Duration.ofSeconds(5)));
        assertThat(result.from()).isEqualTo(result.to().minusDays(30));
    }

    @Test
    void withOnlyToStartsThirtyDaysBeforeIt() {
        //given
        var to = LocalDateTime.of(2026, 10, 31, 23, 59);

        //when
        var result = filter(null, to);

        //then
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.from()).isEqualTo(to.minusDays(30));
    }

    @Test
    void withOnlyFromEndsNow() {
        //given
        var from = LocalDateTime.of(2026, 10, 1, 0, 0);

        //when
        var result = filter(from, null);

        //then
        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isCloseTo(LocalDateTime.now(), within(Duration.ofSeconds(5)));
    }

    @Test
    void withBothDatesKeepsThem() {
        //given
        var from = LocalDateTime.of(2026, 1, 1, 0, 0);
        var to = LocalDateTime.of(2026, 2, 1, 0, 0);

        //when
        var result = filter(from, to);

        //then
        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
    }

    @Test
    void pagingDefaultsToFirstPageOf100SortedByIdAscending() {
        //given
        //when
        var pageable = filter(null, null).toPageable();

        //then
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void toPageableUsesTheRequestedPagingAndSorting() {
        //given
        var sortBy = "dateCreated";
        var direction = "DESC";
        var page = 2;
        var size = 25;

        //when
        var result = new MessageFilter(null, null, null, null, null, null, page, size, sortBy, direction);
        var pageable = result.toPageable();

        assertThat(pageable.getPageNumber()).isEqualTo(page);
        assertThat(pageable.getPageSize()).isEqualTo(size);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, sortBy));
    }
}
