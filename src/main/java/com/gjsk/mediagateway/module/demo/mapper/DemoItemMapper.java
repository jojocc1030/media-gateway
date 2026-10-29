package com.gjsk.mediagateway.module.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gjsk.mediagateway.module.demo.entity.DemoItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 示例记录 Mapper。
 */
@Mapper
public interface DemoItemMapper extends BaseMapper<DemoItem> {
}
