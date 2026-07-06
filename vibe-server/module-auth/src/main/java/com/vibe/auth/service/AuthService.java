package com.vibe.auth.service;

import com.vibe.auth.dto.AgentLoginDTO;
import com.vibe.auth.dto.CustomerLoginDTO;
import com.vibe.auth.dto.LoginDTO;
import com.vibe.auth.vo.LoginVO;

/**
 * 认证服务接口
 *
 * @author vibe
 */
public interface AuthService {

    /**
     * 账号密码登录（内部用户，PC/MOBILE 端）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 代理商登录（手机号 + 短信验证码，签发 AGENT 类型 Token）
     *
     * @param dto 代理商登录请求
     * @return 登录响应
     */
    LoginVO agentLogin(AgentLoginDTO dto);

    /**
     * 客户登录（手机号 + 短信验证码，签发 CUSTOMER 类型 Token）
     *
     * @param dto 客户登录请求
     * @return 登录响应
     */
    LoginVO customerLogin(CustomerLoginDTO dto);

    /**
     * 登出（Token 加入黑名单）
     */
    void logout(String token);

    /**
     * 刷新 Token
     */
    LoginVO refresh(String token);

    /**
     * 当前登录用户修改密码
     *
     * @param userId      当前用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文，长度 6~64）
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
