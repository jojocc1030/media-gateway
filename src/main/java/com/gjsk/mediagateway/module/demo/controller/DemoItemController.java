package com.gjsk.mediagateway.module.demo.controller;

import com.gjsk.mediagateway.common.api.BaseResponse;
import com.gjsk.mediagateway.common.api.PageResult;
import com.gjsk.mediagateway.common.idempotent.Idempotent;
import com.gjsk.mediagateway.module.demo.dto.query.DemoItemPageQuery;
import com.gjsk.mediagateway.module.demo.dto.request.DemoItemCreateRequest;
import com.gjsk.mediagateway.module.demo.dto.request.DemoItemUpdateRequest;
import com.gjsk.mediagateway.module.demo.dto.vo.DemoItemVO;
import com.gjsk.mediagateway.module.demo.service.DemoItemService;
import com.gjsk.mediagateway.security.RequireRole;
import com.gjsk.mediagateway.security.RoleType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例模块接口：演示统一响应、分页、校验、幂等、角色校验与 RESTful 路由规范。
 *
 * <p>该模块不是业务功能，仅作为新增业务模块的复制起点。</p>
 */
@Tag(name = "示例模块", description = "演示统一响应、分页、校验、幂等与角色校验的标准写法")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/demo/items")
public class DemoItemController {

    private final DemoItemService demoItemService;

    /**
     * 创建示例记录。
     */
    @Operation(summary = "创建示例记录", description = "需要 Idempotency-Key 请求头；ADMIN/OPERATOR 角色可调用。")
    @Idempotent(ttlSeconds = 300)
    @RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})
    @PostMapping
    public BaseResponse<DemoItemVO> create(@Valid @RequestBody DemoItemCreateRequest request) {
        return BaseResponse.ok(demoItemService.create(request));
    }

    /**
     * 分页查询示例记录。
     */
    @Operation(summary = "分页查询示例记录", description = "登录即可访问；page/page_size 使用 snake_case。")
    @GetMapping("/page")
    public BaseResponse<PageResult<DemoItemVO>> page(
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，最大 100", example = "20")
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @Parameter(description = "名称模糊搜索", example = "示例")
            @RequestParam(required = false) String name,
            @Parameter(description = "状态：0-停用 1-启用", example = "1")
            @RequestParam(required = false) Integer status) {
        DemoItemPageQuery query = new DemoItemPageQuery();
        if (page != null) {
            query.setPage(page);
        }
        if (pageSize != null) {
            query.setPageSize(pageSize);
        }
        query.setName(name);
        query.setStatus(status);
        return BaseResponse.ok(demoItemService.page(query));
    }

    /**
     * 查询示例记录详情。
     */
    @Operation(summary = "查询示例记录详情", description = "登录即可访问。")
    @GetMapping("/{item_id}")
    public BaseResponse<DemoItemVO> detail(
            @Parameter(description = "业务主键", required = true)
            @PathVariable("item_id") String itemId) {
        return BaseResponse.ok(demoItemService.detail(itemId));
    }

    /**
     * 更新示例记录。
     */
    @Operation(summary = "更新示例记录", description = "ADMIN/OPERATOR 角色可调用。")
    @RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})
    @PutMapping("/{item_id}")
    public BaseResponse<DemoItemVO> update(
            @Parameter(description = "业务主键", required = true)
            @PathVariable("item_id") String itemId,
            @Valid @RequestBody DemoItemUpdateRequest request) {
        return BaseResponse.ok(demoItemService.update(itemId, request));
    }

    /**
     * 逻辑删除示例记录。
     */
    @Operation(summary = "删除示例记录", description = "逻辑删除；ADMIN/OPERATOR 角色可调用。")
    @RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})
    @DeleteMapping("/{item_id}")
    public BaseResponse<Void> delete(
            @Parameter(description = "业务主键", required = true)
            @PathVariable("item_id") String itemId) {
        demoItemService.delete(itemId);
        return BaseResponse.ok();
    }
}
