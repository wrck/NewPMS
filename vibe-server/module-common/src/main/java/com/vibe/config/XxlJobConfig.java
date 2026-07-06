package com.vibe.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 2.4 执行器配置。
 *
 * <p>注入 {@link XxlJobSpringExecutor}，启动后自动注册到调度中心（xxl-job-admin），
 * 调度中心据此将任务调度到当前执行器实例。</p>
 *
 * <p>调度中心默认地址：{@code http://localhost:8081/xxl-job-admin}（由 docker-compose
 * 暴露 8081 → 容器 8080）。生产环境通过环境变量 {@code VIBE_XXL_JOB_ADMIN} 覆盖。</p>
 *
 * <p>通讯 Token 需与调度中心 application.properties 中
 * {@code xxl.job.accessToken} 保持一致。</p>
 *
 * @author vibe
 */
@Slf4j
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.access-token}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    /**
     * 执行器 IP，留空则由 XXL-JOB 自动获取本机 IP（推荐生产显式指定，避免容器多网卡误判）。
     */
    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    /**
     * XXL-JOB 执行器 Spring Bean。
     *
     * <p>实现 {@link SmartInitializingSingleton} 接口，Spring 容器初始化完成后自动启动内嵌服务
     * 并注册到调度中心；销毁时自动摘除注册。</p>
     */
    @Bean(initMethod = "start", destroyMethod = "destroy")
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job executor init: adminAddresses={}, appname={}, port={}, logPath={}",
                adminAddresses, appname, port, logPath);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appname);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
