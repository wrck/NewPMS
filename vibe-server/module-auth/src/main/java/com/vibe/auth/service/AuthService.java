package com.vibe.auth.service;

import com.vibe.auth.dto.LoginDTO;
import com.vibe.auth.vo.LoginVO;

/**
 * 认证服务接口
 *
 * @author vibe
 */
public interface AuthService {

    /**
     * 账号密码登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 登出（Token 加入黑名单）
     */
    void logout(String token);

    /**
     * 刷新 Token
     */
    LoginVO refresh(String token);

    /**
     * 客户手机号 + 短信验证码登录（临时 Token）
     */
    LoginVO customerLogin(String phone, String smsCode);

    /**
     * 当前登录用户修改密码
     *
     * @param userId      当前用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文，长度 6~64）
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
