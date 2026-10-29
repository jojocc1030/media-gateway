package com.gjsk.mediagateway.module.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gjsk.mediagateway.common.api.PageResult;
import com.gjsk.mediagateway.common.error.NotFoundException;
import com.gjsk.mediagateway.common.util.IdUtils;
import com.gjsk.mediagateway.module.demo.dto.query.DemoItemPageQuery;
import com.gjsk.mediagateway.module.demo.dto.request.DemoItemCreateRequest;
import com.gjsk.mediagateway.module.demo.dto.request.DemoItemUpdateRequest;
import com.gjsk.mediagateway.module.demo.dto.vo.DemoItemVO;
import com.gjsk.mediagateway.module.demo.entity.DemoItem;
import com.gjsk.mediagateway.module.demo.mapper.DemoItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 示例业务服务：演示事务边界、查询条件、唯一/存在性校验与 VO 转换规范。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoItemService {

    private final DemoItemMapper demoItemMapper;

    /**
     * 创建示例记录。
     */
    @Transactional
    public DemoItemVO create(DemoItemCreateRequest request) {
        DemoItem entity = new DemoItem();
        entity.setItemId(IdUtils.newId());
        entity.setName(trimRequired(request.name()));
        entity.setStatus(request.status());
        entity.setRemark(trimToNull(request.remark()));
        demoItemMapper.insert(entity);
        log.info("示例记录创建: itemId={} name={}", entity.getItemId(), entity.getName());
        return toVO(entity);
    }

    /**
     * 分页查询示例记录。
     */
    public PageResult<DemoItemVO> page(DemoItemPageQuery query) {
        query.normalize();
        String name = trimToNull(query.getName());
        LambdaQueryWrapper<DemoItem> wrapper = new LambdaQueryWrapper<DemoItem>()
                .like(name != null, DemoItem::getName, name)
                .eq(query.getStatus() != null, DemoItem::getStatus, query.getStatus())
                .orderByDesc(DemoItem::getCreateTime)
                .orderByDesc(DemoItem::getId);
        Page<DemoItem> page = demoItemMapper.selectPage(
                new Page<>(query.currentPage(), query.currentPageSize()), wrapper);
        return PageResult.of(page, this::toVO);
    }

    /**
     * 查询示例记录详情。
     */
    public DemoItemVO detail(String itemId) {
        return toVO(requireItem(itemId));
    }

    /**
     * 更新示例记录。
     */
    @Transactional
    public DemoItemVO update(String itemId, DemoItemUpdateRequest request) {
        DemoItem entity = requireItem(itemId);
        entity.setName(trimRequired(request.name()));
        entity.setStatus(request.status());
        entity.setRemark(trimToNull(request.remark()));
        demoItemMapper.updateById(entity);
        log.info("示例记录更新: itemId={}", itemId);
        return toVO(entity);
    }

    /**
     * 逻辑删除示例记录。
     */
    @Transactional
    public void delete(String itemId) {
        DemoItem entity = requireItem(itemId);
        demoItemMapper.deleteById(entity.getId());
        log.info("示例记录删除: itemId={}", itemId);
    }

    private DemoItem requireItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new NotFoundException("示例记录不存在");
        }
        DemoItem entity = demoItemMapper.selectOne(
                new LambdaQueryWrapper<DemoItem>().eq(DemoItem::getItemId, itemId));
        if (entity == null) {
            throw new NotFoundException("示例记录不存在: " + itemId);
        }
        return entity;
    }

    private DemoItemVO toVO(DemoItem entity) {
        return new DemoItemVO(entity.getItemId(), entity.getName(), entity.getStatus(),
                entity.getRemark(), entity.getCreateTime(), entity.getUpdateTime());
    }

    private String trimRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
