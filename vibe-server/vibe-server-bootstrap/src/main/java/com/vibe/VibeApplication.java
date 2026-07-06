package com.vibe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 网络设备原厂实施项目管理系统 - 主启动类
 *
 * <p>扫描 com.vibe 下所有模块，启用定时任务与异步。</p>
 *
 * <p>{@code @EnableFeignClients} 启用 Spring Cloud OpenFeign，扫描 module-integration 下 4 个 Adapter
 * （ERP/IM/物流/OA）的 @FeignClient 接口。</p>
 *
 * @author vibe
 */
@SpringBootApplication(scanBasePackages = "com.vibe")
@MapperScan("com.vibe.**.mapper")
@EnableScheduling
@EnableAsync
@EnableFeignClients(basePackages = "com.vibe.integration.adapter")
public class VibeApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeApplication.class, args);
    }
}
