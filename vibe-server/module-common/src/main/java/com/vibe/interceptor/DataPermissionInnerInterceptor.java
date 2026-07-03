package com.vibe.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.vibe.annotation.DataPermission;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 数据权限 MyBatis-Plus 内部拦截器
 *
 * <p>基于 {@link DataPermission} 注解和 {@link UserContext} 当前用户角色，
 * 自动在 SELECT SQL 末尾追加 WHERE 条件，实现行级数据隔离。</p>
 *
 * <p>核心流程（{@link #beforeQuery}）：</p>
 * <ol>
 *   <li><b>解析注解</b>：优先从 {@link DataPermissionContext}（Service 方法 AOP 传入）读取，
 *       否则通过反射从 Mapper 接口方法读取。无注解或 {@code ignore=true} → 跳过</li>
 *   <li><b>获取用户</b>：从 {@link UserContextHolder} 获取当前登录用户。
 *       无登录用户（定时任务等）→ 跳过，不拼接条件</li>
 *   <li><b>角色判定</b>：SUPER_ADMIN / DIRECTOR → 全部数据，跳过</li>
 *   <li><b>构建条件</b>：根据角色与注解字段配置，拼接 WHERE 条件片段
 *       （如 {@code project.pm_id = 1001}）</li>
 *   <li><b>SQL 改写</b>：使用 JSqlParser 4.9 解析原 SQL，在 WHERE 子句末尾 AND 追加条件。
 *       仅对 SELECT 生效，INSERT/UPDATE/DELETE 不受影响</li>
 *   <li><b>反射回写</b>：将改写后的 SQL 通过反射写回 {@link BoundSql} 的 sql 字段</li>
 * </ol>
 *
 * <p>角色与拼接规则（参见设计文档 1.5 数据权限规则）：</p>
 * <ul>
 *   <li>SUPER_ADMIN / DIRECTOR → 全部数据，不追加条件</li>
 *   <li>PM → {@code AND {table}.{pmField} = userId}</li>
 *   <li>ENGINEER → {@code AND {table}.{engineerField} = userId}</li>
 *   <li>AGENT_ADMIN → {@code AND {table}.{agentField} = tenantId}（代理商公司ID）</li>
 *   <li>AGENT_ENGINEER → {@code AND {table}.{agentEngineerField} = userId}</li>
 *   <li>CUSTOMER → {@code AND {table}.{customerField} = tenantId}（客户关联ID）</li>
 * </ul>
 *
 * <p>注意：当用户ID/tenantId 为 null 但角色需要拼接条件时，追加永假条件 {@code 1 = 0}
 * 防止数据泄露。DISPATCHER / DEVICE_ADMIN / FINANCE 等角色默认不拼接（看全部），
 * 如需限制应在业务层显式查询。</p>
 *
 * <p>JSqlParser 4.9 API 适配：4.6 起 {@code SelectBody} 接口被移除，
 * {@code PlainSelect} / {@code SetOperationList} 直接继承 {@code Select}，
 * {@code CCJSqlParserUtil.parse()} 直接返回 {@code Statement}（实际为 {@code PlainSelect}
 * 或 {@code SetOperationList}），无需 {@code Select.getSelectBody()} 中转。</p>
 *
 * <p>注册方式：在 {@code MybatisPlusConfig} 中通过
 * {@code interceptor.addInnerInterceptor(new DataPermissionInnerInterceptor())} 注册，
 * 必须排在分页拦截器之前（先过滤再分页）。</p>
 *
 * @author vibe
 */
@Slf4j
public class DataPermissionInnerInterceptor implements InnerInterceptor {

    /** 永假条件，用于角色需要拼接但用户ID缺失时防止数据泄露 */
    private static final String ALWAYS_FALSE = "1 = 0";

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // 1. 解析 @DataPermission 注解（无注解 → 跳过，保证性能：只在标注方法上生效）
        DataPermission dataPermission = resolveAnnotation(ms);
        if (dataPermission == null || dataPermission.ignore()) {
            return;
        }

        // 2. 获取当前用户上下文（无登录用户如定时任务 → 跳过）
        UserContext userContext = UserContextHolder.get();
        if (userContext == null) {
            return;
        }

        // 3. 超级管理员/项目总监：全部数据，不拼接条件
        if (userContext.hasAnyRole("SUPER_ADMIN", "DIRECTOR")) {
            return;
        }

        // 4. 根据角色与注解字段构建 WHERE 条件片段
        String condition = buildCondition(dataPermission, userContext);
        if (condition == null || condition.isEmpty()) {
            return;
        }

        // 5. 使用 JSqlParser 改写 SQL（仅 SELECT 生效）
        String originalSql = boundSql.getSql();
        String newSql = injectWhere(originalSql, condition);
        if (newSql != null && !newSql.equals(originalSql)) {
            reflectSetSql(boundSql, newSql);
            if (log.isDebugEnabled()) {
                log.debug("数据权限拦截器改写 SQL: roles={}, condition={}, msId={}",
                        userContext.getRoles(), condition, ms.getId());
            }
        }
    }

    /**
     * 解析 {@link DataPermission} 注解。
     *
     * <p>优先从 {@link DataPermissionContext}（Service 方法 AOP 切面传递）读取，
     * 否则通过反射从 Mapper 接口方法读取。</p>
     *
     * @param ms MyBatis 映射语句
     * @return 注解实例，无则返回 null
     */
    private DataPermission resolveAnnotation(MappedStatement ms) {
        // 优先读 ThreadLocal（Service 方法上的注解，由 DataPermissionAspect 写入）
        DataPermission fromContext = DataPermissionContext.get();
        if (fromContext != null) {
            return fromContext;
        }

        // 反射读取 Mapper 接口方法上的注解
        String msId = ms.getId();
        if (msId == null || !msId.contains(".")) {
            return null;
        }
        int lastDot = msId.lastIndexOf('.');
        String className = msId.substring(0, lastDot);
        String methodName = msId.substring(lastDot + 1);
        try {
            Class<?> mapperClass = Class.forName(className);
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    DataPermission annotation = method.getAnnotation(DataPermission.class);
                    if (annotation != null) {
                        return annotation;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // 非 Mapper 接口（如 MyBatis-Plus 动态生成的代理类），忽略
        } catch (Throwable e) {
            log.warn("解析 @DataPermission 注解失败: msId={}, err={}", msId, e.getMessage());
        }
        return null;
    }

    /**
     * 根据角色和注解字段构建 WHERE 条件片段。
     *
     * @param dataPermission 注解配置
     * @param userContext    当前用户上下文
     * @return 条件 SQL 片段（如 {@code project.pm_id = 1001}），null 表示不追加条件
     */
    private String buildCondition(DataPermission dataPermission, UserContext userContext) {
        String table = dataPermission.table();
        String prefix = (table == null || table.isEmpty()) ? "" : (table + ".");

        // AGENT_ADMIN：本代理商公司所有数据
        if (userContext.hasRole("AGENT_ADMIN")) {
            Long tenantId = userContext.getTenantId();
            if (tenantId == null) {
                log.warn("AGENT_ADMIN 用户 tenantId 为空，追加永假条件防止数据泄露, userId={}",
                        userContext.getUserId());
                return prefix + dataPermission.agentField() + " = " + ALWAYS_FALSE;
            }
            return prefix + dataPermission.agentField() + " = " + tenantId;
        }

        // AGENT_ENGINEER：分配给自己的任务
        if (userContext.hasRole("AGENT_ENGINEER")) {
            Long userId = userContext.getUserId();
            if (userId == null) {
                return prefix + dataPermission.agentEngineerField() + " = " + ALWAYS_FALSE;
            }
            return prefix + dataPermission.agentEngineerField() + " = " + userId;
        }

        // CUSTOMER：自己关联的项目（只读）
        if (userContext.hasRole("CUSTOMER")) {
            Long customerId = userContext.getTenantId();
            if (customerId == null) {
                log.warn("CUSTOMER 用户 tenantId 为空，追加永假条件防止数据泄露, userId={}",
                        userContext.getUserId());
                return prefix + dataPermission.customerField() + " = " + ALWAYS_FALSE;
            }
            return prefix + dataPermission.customerField() + " = " + customerId;
        }

        // PM：自己负责的项目
        if (userContext.hasRole("PM")) {
            Long userId = userContext.getUserId();
            if (userId == null) {
                return prefix + dataPermission.pmField() + " = " + ALWAYS_FALSE;
            }
            return prefix + dataPermission.pmField() + " = " + userId;
        }

        // ENGINEER：分配给自己的任务
        if (userContext.hasRole("ENGINEER")) {
            Long userId = userContext.getUserId();
            if (userId == null) {
                return prefix + dataPermission.engineerField() + " = " + ALWAYS_FALSE;
            }
            return prefix + dataPermission.engineerField() + " = " + userId;
        }

        // 其他角色（DISPATCHER / DEVICE_ADMIN / FINANCE 等）：默认不拼接（看全部数据）
        // 如需限制，应在业务层显式查询或在注解中扩展字段
        return null;
    }

    /**
     * 使用 JSqlParser 4.9 在原 SQL 的 WHERE 子句末尾追加条件。
     *
     * <p>仅对 SELECT 生效，INSERT/UPDATE/DELETE 返回原 SQL。
     * 支持 PlainSelect（普通查询）和 SetOperationList（UNION 查询）。</p>
     *
     * <p>JSqlParser 4.9 适配：{@code CCJSqlParserUtil.parse()} 直接返回
     * {@code PlainSelect} 或 {@code SetOperationList}（均继承 {@code Select}），
     * 不再需要 {@code Select.getSelectBody()} 中转。</p>
     *
     * @param sql       原 SQL
     * @param condition 条件片段（如 {@code project.pm_id = 1001}）
     * @return 改写后的 SQL，解析失败时返回原 SQL
     */
    private String injectWhere(String sql, String condition) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);

            // 普通 SELECT：直接追加 WHERE 条件
            if (statement instanceof PlainSelect) {
                injectWhereToPlainSelect((PlainSelect) statement, condition);
                return statement.toString();
            }

            // UNION / INTERSECT / EXCEPT：给每个子 SELECT 追加条件
            if (statement instanceof SetOperationList) {
                SetOperationList setOperationList = (SetOperationList) statement;
                List<Select> selects = setOperationList.getSelects();
                if (selects != null) {
                    for (Select select : selects) {
                        if (select instanceof PlainSelect) {
                            injectWhereToPlainSelect((PlainSelect) select, condition);
                        }
                    }
                }
                return statement.toString();
            }

            // 非 SELECT（INSERT/UPDATE/DELETE）或其他类型，不处理
            return sql;
        } catch (Throwable e) {
            log.warn("数据权限 SQL 解析失败，跳过条件注入: sql={}, err={}", sql, e.getMessage());
            return sql;
        }
    }

    /**
     * 向 PlainSelect 的 WHERE 子句追加 AND 条件。
     */
    private void injectWhereToPlainSelect(PlainSelect plainSelect, String condition) throws Exception {
        Expression newCondition = CCJSqlParserUtil.parseCondExpression(condition);
        Expression where = plainSelect.getWhere();
        if (where == null) {
            plainSelect.setWhere(newCondition);
        } else {
            plainSelect.setWhere(new AndExpression(where, newCondition));
        }
    }

    /**
     * 通过反射修改 {@link BoundSql} 的 sql 字段。
     *
     * <p>BoundSql 的 sql 字段为 private，需通过反射覆盖。
     * 这是 MyBatis-Plus 拦截器修改 SQL 的标准做法。</p>
     */
    private void reflectSetSql(BoundSql boundSql, String newSql) {
        try {
            Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, newSql);
        } catch (Throwable e) {
            log.warn("修改 BoundSql sql 字段失败: {}", e.getMessage());
        }
    }
}
