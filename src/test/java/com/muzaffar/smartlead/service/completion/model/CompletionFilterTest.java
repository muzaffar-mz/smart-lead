package com.muzaffar.smartlead.service.completion.model;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class CompletionFilterTest {

    @Test
    void withoutDatesDoesNotLimitTheRange() {
        //given
        //when
        var result = new CompletionFilter(null, null, null, null, null, null, null, null);

        //then
        assertThat(result.from()).isNull();
        assertThat(result.to()).isNull();
    }

    @Test
    void pagingDefaultsToFirstPageOf100SortedByIdAscending() {
        //given
        //when
        var pageable = new CompletionFilter(null, null, null, null, null, null, null, null).toPageable();

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
        var pageable = new CompletionFilter(null, null, null, null, page, size, sortBy, direction).toPageable();

        //then
        assertThat(pageable.getPageNumber()).isEqualTo(page);
        assertThat(pageable.getPageSize()).isEqualTo(size);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, sortBy));
    }
}
