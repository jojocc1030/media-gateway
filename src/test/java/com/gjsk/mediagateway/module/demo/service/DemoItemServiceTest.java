package com.gjsk.mediagateway.module.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjsk.mediagateway.common.api.PageResult;
import com.gjsk.mediagateway.common.error.NotFoundException;
import com.gjsk.mediagateway.module.demo.dto.query.DemoItemPageQuery;
import com.gjsk.mediagateway.module.demo.dto.request.DemoItemCreateRequest;
import com.gjsk.mediagateway.module.demo.dto.request.DemoItemUpdateRequest;
import com.gjsk.mediagateway.module.demo.dto.vo.DemoItemVO;
import com.gjsk.mediagateway.module.demo.entity.DemoItem;
import com.gjsk.mediagateway.module.demo.mapper.DemoItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoItemServiceTest {

    @Mock
    private DemoItemMapper demoItemMapper;

    private DemoItemService demoItemService;

    @BeforeEach
    void setUp() {
        demoItemService = new DemoItemService(demoItemMapper);
    }

    @Test
    void createShouldGenerateBusinessIdAndNormalizeFields() {
        demoItemService.create(new DemoItemCreateRequest("  名称  ", 1, "   "));

        ArgumentCaptor<DemoItem> captor = ArgumentCaptor.forClass(DemoItem.class);
        verify(demoItemMapper).insert(captor.capture());
        DemoItem saved = captor.getValue();
        assertThat(saved.getItemId()).matches("[0-9a-f]{32}");
        assertThat(saved.getName()).isEqualTo("名称");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getRemark()).isNull();
    }

    @Test
    void pageShouldNormalizePaginationAndMapRecords() {
        Page<DemoItem> sqlPage = new Page<>(1, 20, 1);
        sqlPage.setRecords(List.of(sampleItem()));
        doReturn(sqlPage).when(demoItemMapper).selectPage(any(Page.class), any());

        DemoItemPageQuery query = new DemoItemPageQuery();
        query.setPage(0);
        query.setPageSize(500);
        query.setName(" 名称 ");
        query.setStatus(1);

        PageResult<DemoItemVO> result = demoItemService.page(query);

        assertThat(query.getPage()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(100);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).itemId()).isEqualTo("item001");

        ArgumentCaptor<Page<DemoItem>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(demoItemMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    void detailShouldReturnVo() {
        when(demoItemMapper.selectOne(any())).thenReturn(sampleItem());

        DemoItemVO vo = demoItemService.detail("item001");

        assertThat(vo.itemId()).isEqualTo("item001");
        assertThat(vo.name()).isEqualTo("名称");
    }

    @Test
    void detailShouldThrowWhenMissing() {
        when(demoItemMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> demoItemService.detail("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void detailShouldRejectBlankBusinessId() {
        assertThatThrownBy(() -> demoItemService.detail(" "))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateShouldPersistNormalizedFields() {
        DemoItem entity = sampleItem();
        when(demoItemMapper.selectOne(any())).thenReturn(entity);

        DemoItemVO vo = demoItemService.update("item001", new DemoItemUpdateRequest(" 新名称 ", 0, " 备注 "));

        verify(demoItemMapper).updateById(entity);
        assertThat(vo.name()).isEqualTo("新名称");
        assertThat(vo.status()).isZero();
        assertThat(vo.remark()).isEqualTo("备注");
    }

    @Test
    void updateShouldThrowWhenMissing() {
        when(demoItemMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> demoItemService.update("missing", new DemoItemUpdateRequest("名称", 1, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteShouldRemoveByPhysicalId() {
        when(demoItemMapper.selectOne(any())).thenReturn(sampleItem());

        demoItemService.delete("item001");

        verify(demoItemMapper).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenMissing() {
        when(demoItemMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> demoItemService.delete("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    private DemoItem sampleItem() {
        DemoItem item = new DemoItem();
        item.setId(1L);
        item.setItemId("item001");
        item.setName("名称");
        item.setStatus(1);
        item.setRemark("备注");
        item.setCreateTime(Instant.parse("2026-01-01T00:00:00Z"));
        item.setUpdateTime(Instant.parse("2026-01-01T00:00:00Z"));
        return item;
    }
}
