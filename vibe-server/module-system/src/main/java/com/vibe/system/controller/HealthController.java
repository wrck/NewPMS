package com.vibe.system.controller;

import com.vibe.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统管理模块健康检查
 *
 * <p>模块启动状态探针，用于网关与服务发现健康检测。</p>
 *
 * @author vibe
 */
@Tag(name = "健康管理-系统管理", description = "模块启动状态探针")
@RestController
@RequestMapping("/api/v1/system")
public class HealthController {

    @Operation(summary = "健康检查", description = "返回 ok 表示模块已就绪")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("ok");
    }
}
