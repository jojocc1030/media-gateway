package com.gjsk.mediagateway.common.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @Test
    void ofShouldCarryItemsAndPaginationFields() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 2, 10, 42);

        assertThat(result.getItems()).containsExactly("a", "b");
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(42);
    }

    @Test
    void ofMybatisPageShouldMapRecords() {
        Page<Integer> page = new Page<>(2, 10, 42);
        page.setRecords(List.of(1, 2, 3));

        PageResult<String> result = PageResult.of(page, String::valueOf);

        assertThat(result.getItems()).containsExactly("1", "2", "3");
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(42);
    }
}
