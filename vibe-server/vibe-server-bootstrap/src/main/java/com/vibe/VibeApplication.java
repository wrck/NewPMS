package com.vibe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 网络设备原厂实施项目管理系统 - 主启动类
 *
 * <p>扫描 com.vibe 下所有模块，启用定时任务与异步。</p>
 *
 * @author vibe
 */
@SpringBootApplication(scanBasePackages = "com.vibe")
@MapperScan("com.vibe.**.mapper")
@EnableScheduling
@EnableAsync
public class VibeApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeApplication.class, args);
    }
}
