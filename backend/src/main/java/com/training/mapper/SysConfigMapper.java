package com.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.training.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
    String getValueByKey(@Param("configKey") String configKey);
    List<SysConfig> selectByKeys(@Param("keys") List<String> keys);
}
