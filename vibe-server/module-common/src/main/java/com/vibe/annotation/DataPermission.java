package com.vibe.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 *
 * <p>标注于 Mapper 方法或 Service 方法上，配合 MyBatis-Plus 数据权限拦截器
 * ({@code DataPermissionInnerInterceptor}) 实现行级数据隔离。
 * 拦截器会根据当前登录用户角色，自动在 SELECT SQL 末尾追加 WHERE 条件。</p>
 *
 * <p>角色与字段映射：</p>
 * <ul>
 *   <li>SUPER_ADMIN / DIRECTOR：全部数据，不追加条件</li>
 *   <li>PM：{@link #pmField} = 当前 userId（自己负责的项目）</li>
 *   <li>ENGINEER：{@link #engineerField} = 当前 userId（分配给自己的任务）</li>
 *   <li>AGENT_ADMIN：{@link #agentField} = 当前 tenantId（本公司代理商数据）</li>
 *   <li>AGENT_ENGINEER：{@link #agentEngineerField} = 当前 userId（分配给自己的任务）</li>
 *   <li>CUSTOMER：{@link #customerField} = 当前 tenantId（自己关联的项目，只读）</li>
 * </ul>
 *
 * <p>典型用法：</p>
 * <pre>
 * // 项目列表：PM 看自己负责的项目，代理商看本公司项目，客户看自己关联项目
 * {@literal @}DataPermission(table = "project")
 * List<ProjectEntity> selectProjectList(...);
 *
 * // 外包任务：代理商管理员看本公司所有外包任务
 * {@literal @}DataPermission(table = "outsource_task", agentField = "agent_company_id")
 * List<OutsourceTaskEntity> selectOutsourceTaskList(...);
 *
 * // 多表 JOIN 时必须指定别名，避免字段歧义
 * {@literal @}DataPermission(table = "t", pmField = "pm_id")
 * List<ProjectTaskVO> selectTaskJoinProject(...);
 *
 * // 管理员绕过数据权限（如导出全部数据）
 * {@literal @}DataPermission(ignore = true)
 * List<ProjectEntity> exportAllProjects(...);
 * </pre>
 *
 * <p>注解位置说明：</p>
 * <ul>
 *   <li>标注在 Mapper 接口方法上：拦截器通过反射直接读取</li>
 *   <li>标注在 Service 方法上：通过 {@code DataPermissionAspect} 切面传递到 ThreadLocal，
 *       拦截器优先读取 ThreadLocal</li>
 * </ul>
 *
 * @author vibe
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 表别名（默认空字符串，表示使用主表无别名前缀）。
     *
     * <p>单表查询可不填；多表 JOIN 查询必须指定，避免 WHERE 字段歧义。
     * 例如 {@code table = "project"} 会拼接为 {@code AND project.pm_id = ?}。</p>
     */
    String table() default "";

    /**
     * PM（项目经理）角色过滤字段，默认 "pm_id"。
     *
     * <p>拼接条件：{@code AND {table}.pm_id = 当前userId}，仅看自己负责的项目。</p>
     */
    String pmField() default "pm_id";

    /**
     * ENGINEER（实施工程师）角色过滤字段，默认 "assignee_id"。
     *
     * <p>拼接条件：{@code AND {table}.assignee_id = 当前userId}，仅看分配给自己的任务。</p>
     */
    String engineerField() default "assignee_id";

    /**
     * AGENT_ADMIN（代理商管理员）角色过滤字段，默认 "agent_company_id"。
     *
     * <p>拼接条件：{@code AND {table}.agent_company_id = 当前tenantId}，
     * 仅看本代理商公司所有数据。</p>
     */
    String agentField() default "agent_company_id";

    /**
     * AGENT_ENGINEER（代理商工程师）角色过滤字段，默认 "agent_engineer_id"。
     *
     * <p>拼接条件：{@code AND {table}.agent_engineer_id = 当前userId}，
     * 仅看分配给自己的任务。</p>
     */
    String agentEngineerField() default "agent_engineer_id";

    /**
     * CUSTOMER（客户联系人）角色过滤字段，默认 "customer_id"。
     *
     * <p>拼接条件：{@code AND {table}.customer_id = 当前tenantId}，
     * 仅看自己关联的项目（只读，极有限字段）。</p>
     */
    String customerField() default "customer_id";

    /**
     * 是否忽略数据权限（管理员绕过，默认 false）。
     *
     * <p>置为 true 时，无论当前用户角色，均不追加 WHERE 条件。
     * 适用于全局导出、定时任务统计等需要看全部数据的场景。</p>
     */
    boolean ignore() default false;
}
