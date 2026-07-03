package com.vibe.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 当前登录用户上下文
 *
 * <p>由 {@link UserContextHolder} 通过 ThreadLocal 维护，过滤器解析 JWT 后写入。</p>
 *
 * @author vibe
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String userName;
    /** 真实姓名 */
    private String realName;
    /** 角色编码列表 */
    @Builder.Default
    private List<String> roles = Collections.emptyList();
    /** 租户类型：INTERNAL / AGENT / CUSTOMER */
    private String tenantType;
    /** 租户ID（代理商登录时为代理商公司ID，其他为 null） */
    private Long tenantId;
    /** 组织ID */
    private Long orgId;
    /** 客户端类型：PC / MOBILE / AGENT / CUSTOMER */
    private String clientType;
    /** Token ID（jti） */
    private String tokenId;

    /* ============ 角色判定快捷方法 ============ */

    public boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    public boolean isInternal() {
        return "INTERNAL".equals(tenantType);
    }

    public boolean isAgent() {
        return "AGENT".equals(tenantType);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equals(tenantType);
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... roleList) {
        if (roles == null || roles.isEmpty() || roleList == null) {
            return false;
        }
        for (String r : roleList) {
            if (roles.contains(r)) {
                return true;
            }
        }
        return false;
    }
}
