package com.training.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.common.Result;
import com.training.entity.SysConfig;
import com.training.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigService extends ServiceImpl<SysConfigMapper, SysConfig> {
    
    private final SysConfigMapper configMapper;
    
    public Map<String, String> getAllConfigs() {
        List<SysConfig> configs = configMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        for (SysConfig config : configs) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }
    
    public Result<Void> updateConfig(String key, String value) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        
        SysConfig config = configMapper.selectOne(wrapper);
        if (config != null) {
            config.setConfigValue(value);
            configMapper.updateById(config);
        } else {
            config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            configMapper.insert(config);
        }
        
        return Result.success();
    }
    
    public Result<Void> batchUpdateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            updateConfig(entry.getKey(), entry.getValue());
        }
        return Result.success();
    }
}
