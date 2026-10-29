package com.gjsk.mediagateway.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageQueryTest {

    @Test
    void defaultsShouldBeFirstPageAndTwentyItems() {
        PageQuery query = new PageQuery();

        assertThat(query.currentPage()).isEqualTo(1);
        assertThat(query.currentPageSize()).isEqualTo(20);
        assertThat(query.offset()).isZero();
    }

    @Test
    void nullValuesShouldFallBackToDefaults() {
        PageQuery query = new PageQuery();
        query.setPage(null);
        query.setPageSize(null);

        query.normalize();

        assertThat(query.getPage()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(20);
    }

    @Test
    void invalidValuesShouldBeClamped() {
        PageQuery query = new PageQuery();
        query.setPage(0);
        query.setPageSize(1000);

        query.normalize();

        assertThat(query.getPage()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(PageQuery.MAX_PAGE_SIZE);
    }

    @Test
    void offsetShouldBeCalculatedFromCurrentPage() {
        PageQuery query = new PageQuery();
        query.setPage(3);
        query.setPageSize(20);

        assertThat(query.offset()).isEqualTo(40);
    }
}
