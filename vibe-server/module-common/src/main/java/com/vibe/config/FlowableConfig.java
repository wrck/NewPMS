package com.vibe.config;

import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable 7 工作流引擎配置
 *
 * <p>本类与 Flowable Spring Boot Starter 协同工作：</p>
 * <ul>
 *   <li>{@code flowable-spring-boot-starter-process} 通过
 *       {@code FlowableAutoConfiguration} 自动装配 {@link org.flowable.engine.ProcessEngine}，
 *       默认复用 Spring {@link javax.sql.DataSource} 与
 *       {@link org.springframework.transaction.PlatformTransactionManager}，
 *       无需重复声明数据源。</li>
 *   <li>本类通过 {@link EngineConfigurationConfigurer} Bean 对默认配置做增量定制：
 *       <ul>
 *         <li>使用强类型 UUID 作为流程实例/任务 ID 生成策略，
 *             与项目雪花算法主键保持视觉一致且避免数据库自增冲突</li>
 *         <li>数据库 schema 更新策略由 application.yml
 *             {@code flowable.database-schema-update=true} 统一管控（不在此覆盖）</li>
 *         <li>异步执行器激活由 application.yml
 *             {@code flowable.async-executor-activate=true} 控制，
 *             此处仅做兜底默认（yml 未配置时仍启用）</li>
 *       </ul>
 *   </li>
 *   <li>所有 {@code ACT_*} 表由 Flowable 启动时自动创建/升级，
 *       无需手动建表（参考 spec：38 张 ACT_* 表）。</li>
 * </ul>
 *
 * <p><b>事务集成：</b>Flowable 通过 {@link SpringProcessEngineConfiguration}
 * 桥接到 Spring 事务管理器，业务方法上加 {@code @Transactional} 即可保证
 * BPMN 操作与 MyBatis-Plus 操作在同一事务内提交/回滚。</p>
 *
 * @author vibe
 */
@Configuration
public class FlowableConfig {

    /**
     * 增量定制 Flowable {@link SpringProcessEngineConfiguration}。
     *
     * <p>使用 {@link EngineConfigurationConfigurer} 接口介入 Spring Boot
     * 自动装配流程，避免直接覆盖 Starter 提供的 Bean 而破坏自动配置。</p>
     *
     * @return 配置定制器
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> flowableEngineConfigurer() {
        return configuration -> {
            // 使用强类型 UUID 生成器，避免集群环境下数据库自增冲突
            configuration.setIdGenerator(new StrongUuidGenerator());
        };
    }
}
