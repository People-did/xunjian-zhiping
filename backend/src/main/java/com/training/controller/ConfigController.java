package com.training.controller;

import com.training.common.Result;
import com.training.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {
    
    private final ConfigService configService;
    
    @GetMapping("/all")
    public Result<Map<String, String>> getAllConfigs() {
        return Result.success(configService.getAllConfigs());
    }
    
    @PutMapping("/{key}")
    public Result<Void> updateConfig(@PathVariable String key, @RequestBody Map<String, String> body) {
        return configService.updateConfig(key, body.get("value"));
    }
    
    @PutMapping("/batch")
    public Result<Void> batchUpdateConfigs(@RequestBody Map<String, String> configs) {
        return configService.batchUpdateConfigs(configs);
    }
}
