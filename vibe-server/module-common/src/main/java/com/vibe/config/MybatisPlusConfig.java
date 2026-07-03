package com.vibe.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.vibe.interceptor.DataPermissionInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * <p>包含：</p>
 * <ul>
 *   <li>数据权限插件（DataPermissionInnerInterceptor）：基于 @DataPermission 注解
 *       自动拼接 WHERE 条件实现行级数据隔离（必须排在分页前，先过滤再分页）</li>
 *   <li>分页插件（PaginationInnerInterceptor）</li>
 *   <li>乐观锁插件（OptimisticLockerInnerInterceptor）</li>
 *   <li>雪花算法 ID（通过 @TableId(type = ASSIGN_ID) 在 BaseEntity 中开启）</li>
 *   <li>Mapper 扫描（扫描所有模块的 com.vibe.**.mapper 包）</li>
 * </ul>
 *
 * @author vibe
 */
@Configuration
@MapperScan(basePackages = {"com.vibe.**.mapper"}, annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链。
     *
     * <p>InnerInterceptor 按添加顺序执行，数据权限必须排在分页之前，
     * 保证分页统计的 count 与 limit 均基于过滤后的 SQL。</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 数据权限插件：根据 @DataPermission 注解与当前用户角色自动追加 WHERE 条件
        // 必须排在分页插件之前（先过滤数据，再分页）
        interceptor.addInnerInterceptor(new DataPermissionInnerInterceptor());

        // 分页插件（MySQL 方言）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大 500 条
        paginationInterceptor.setMaxLimit(500L);
        // 页码超出后自动归 1，避免越界
        paginationInterceptor.setOverflow(false);
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
