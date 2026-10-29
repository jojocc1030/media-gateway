package com.gjsk.mediagateway.module.demo.controller;

import com.gjsk.mediagateway.common.api.PageResult;
import com.gjsk.mediagateway.common.error.NotFoundException;
import com.gjsk.mediagateway.module.demo.dto.vo.DemoItemVO;
import com.gjsk.mediagateway.module.demo.service.DemoItemService;
import com.gjsk.mediagateway.support.MvcTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DemoItemControllerTest {

    private final DemoItemService demoItemService = mock(DemoItemService.class);
    private final MockMvc mockMvc = MvcTestSupport.standaloneMockMvc(new DemoItemController(demoItemService));

    @Test
    void createShouldReturnSnakeCaseVo() throws Exception {
        when(demoItemService.create(any())).thenReturn(sampleVO());

        mockMvc.perform(post("/api/demo/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"名称","status":1,"remark":"备注"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.item_id").value("item001"))
                .andExpect(jsonPath("$.data.name").value("名称"))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.create_time").exists());

        verify(demoItemService).create(any());
    }

    @Test
    void createShouldRejectMissingName() throws Exception {
        mockMvc.perform(post("/api/demo/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));
    }

    @Test
    void pageShouldExposePaginationFieldsInData() throws Exception {
        when(demoItemService.page(any()))
                .thenReturn(PageResult.of(List.of(sampleVO()), 2, 10, 11));

        mockMvc.perform(get("/api/demo/items/page")
                        .param("page", "2")
                        .param("page_size", "10")
                        .param("name", "名称")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.page_size").value(10))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.items[0].item_id").value("item001"));
    }

    @Test
    void detailShouldReturn404WhenMissing() throws Exception {
        when(demoItemService.detail("missing")).thenThrow(new NotFoundException("示例记录不存在: missing"));

        mockMvc.perform(get("/api/demo/items/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateShouldReturnUpdatedVo() throws Exception {
        when(demoItemService.update(eq("item001"), any())).thenReturn(sampleVO());

        mockMvc.perform(put("/api/demo/items/item001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"名称","status":0,"remark":"备注"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item_id").value("item001"));

        verify(demoItemService).update(eq("item001"), any());
    }

    @Test
    void deleteShouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/demo/items/item001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(demoItemService).delete("item001");
    }

    private DemoItemVO sampleVO() {
        return new DemoItemVO("item001", "名称", 1, "备注",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"));
    }
}
