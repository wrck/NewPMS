package com.vibe.auth.domain;

import com.vibe.agent.entity.AgentEngineerEntity;
import com.vibe.auth.domain.enums.UserType;
import com.vibe.project.entity.CustomerEntity;
import com.vibe.system.entity.SysUserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 认证用户聚合根（统一内部/代理商/客户三类用户身份）
 *
 * <p>作为 module-auth 的领域模型，封装认证成功后的用户身份信息。
 * 通过工厂方法 {@link #ofInternal} / {@link #ofAgent} / {@link #ofCustomer}
 * 从不同数据源实体构建，屏蔽底层实体差异。</p>
 *
 * <p>三类用户身份映射：</p>
 * <ul>
 *   <li>内部用户 —— 来源 {@link SysUserEntity}，userType = INTERNAL，
 *       tenantId 为空或所属组织</li>
 *   <li>代理商工程师 —— 来源 {@link AgentEngineerEntity}，userType = AGENT，
 *       tenantId 为代理商公司ID（agentCompanyId）</li>
 *   <li>客户 —— 来源 {@link CustomerEntity}，userType = CUSTOMER，
 *       tenantId 为客户ID（customer.id）</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID（对应各实体主键） */
    private Long id;
    /** 登录账号（内部用户为 username，代理商/客户为手机号） */
    private String username;
    /** 真实姓名（内部用户为 real_name，代理商为 name，客户为 contact_name） */
    private String realName;
    /** 用户类型 INTERNAL/AGENT/CUSTOMER */
    private UserType userType;
    /** 租户ID（代理商公司ID 或 客户ID，内部用户为空） */
    private Long tenantId;
    /** 角色编码列表 */
    @Builder.Default
    private List<String> roles = Collections.emptyList();
    /** 权限标识列表 */
    @Builder.Default
    private List<String> permissions = Collections.emptyList();

    /**
     * 工厂方法：从内部用户实体构建 AuthUser
     *
     * @param user        系统用户实体
     * @param roles       角色编码列表
     * @param permissions 权限标识列表
     * @return INTERNAL 类型 AuthUser
     */
    public static AuthUser ofInternal(SysUserEntity user, List<String> roles, List<String> permissions) {
        return AuthUser.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userType(UserType.INTERNAL)
                .tenantId(user.getTenantId())
                .roles(roles != null ? roles : Collections.emptyList())
                .permissions(permissions != null ? permissions : Collections.emptyList())
                .build();
    }

    /**
     * 工厂方法：从代理商工程师实体构建 AuthUser
     *
     * @param agent       代理商工程师实体
     * @param roles       角色编码列表（为 null 时默认 AGENT_ENGINEER）
     * @param permissions 权限标识列表
     * @return AGENT 类型 AuthUser
     */
    public static AuthUser ofAgent(AgentEngineerEntity agent, List<String> roles, List<String> permissions) {
        return AuthUser.builder()
                .id(agent.getId())
                .username(agent.getPhone())
                .realName(agent.getName())
                .userType(UserType.AGENT)
                .tenantId(agent.getAgentCompanyId())
                .roles(roles != null && !roles.isEmpty() ? roles : List.of("AGENT_ENGINEER"))
                .permissions(permissions != null ? permissions : Collections.emptyList())
                .build();
    }

    /**
     * 工厂方法：从客户实体构建 AuthUser
     *
     * @param customer    客户实体
     * @param roles       角色编码列表（为 null 时默认 CUSTOMER）
     * @param permissions 权限标识列表
     * @return CUSTOMER 类型 AuthUser
     */
    public static AuthUser ofCustomer(CustomerEntity customer, List<String> roles, List<String> permissions) {
        return AuthUser.builder()
                .id(customer.getId())
                .username(customer.getContactPhone())
                .realName(customer.getContactName())
                .userType(UserType.CUSTOMER)
                .tenantId(customer.getId())
                .roles(roles != null && !roles.isEmpty() ? roles : List.of("CUSTOMER"))
                .permissions(permissions != null ? permissions : Collections.emptyList())
                .build();
    }

    /**
     * 转换为 JWT 可识别的用户类型字符串
     */
    public String userTypeValue() {
        return userType == null ? null : userType.name();
    }
}
