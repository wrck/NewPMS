package com.vibe.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录成功响应 VO
 *
 * <p>结构与前端 vibe-web 的 {@code LoginResult} 类型对齐：
 * <ul>
 *   <li>顶层：{@code token / refreshToken / expiresIn / userInfo}</li>
 *   <li>{@code userInfo} 嵌套用户基本信息（userId、userName、realName、avatar、email、phone、roles、tenantType、tenantId、orgId、orgName）</li>
 * </ul>
 *
 * @author vibe
 */
@Data
@Schema(description = "登录成功响应")
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "刷新 Token（PC 端暂返回空字符串，预留扩展）")
    private String refreshToken = "";

    @Schema(description = "有效期（秒）", example = "28800")
    private Long expiresIn;

    @Schema(description = "用户信息（嵌套对象，与前端 userInfo 对齐）")
    private UserInfo userInfo;

    /**
     * 登录响应内嵌的用户信息对象。
     *
     * <p>字段命名与前端 {@code UserInfo} 类型完全一致。</p>
     */
    @Data
    @Schema(description = "登录用户信息")
    public static class UserInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "用户名")
        private String userName;

        @Schema(description = "真实姓名")
        private String realName;

        @Schema(description = "头像地址")
        private String avatar;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "手机号")
        private String phone;

        @Schema(description = "角色编码列表")
        private List<String> roles;

        @Schema(description = "租户类型 INTERNAL/AGENT/CUSTOMER")
        private String tenantType;

        @Schema(description = "租户ID")
        private Long tenantId;

        @Schema(description = "组织ID")
        private Long orgId;

        @Schema(description = "组织名称")
        private String orgName;
    }
}
