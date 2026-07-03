package com.vibe.system.service;

import com.vibe.common.result.PageResult;
import com.vibe.system.dto.SysUserDTO;
import com.vibe.system.dto.SysUserPasswordDTO;
import com.vibe.system.dto.SysUserQueryDTO;
import com.vibe.system.dto.SysUserStatusDTO;
import com.vibe.system.dto.SysUserRoleDTO;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysUserVO;
import com.vibe.system.vo.UserInfoVO;

import java.util.List;

/**
 * 系统用户服务
 *
 * @author vibe
 */
public interface SysUserService {

    /**
     * 分页查询用户（含角色、组织信息）
     */
    PageResult<SysUserVO> page(SysUserQueryDTO query);

    /**
     * 新增用户
     */
    Long create(SysUserDTO dto);

    /**
     * 编辑用户
     */
    void update(SysUserDTO dto);

    /**
     * 删除用户
     */
    void delete(Long id);

    /**
     * 查询用户详情（含角色列表）
     */
    SysUserVO getDetail(Long id);

    /**
     * 分配用户角色
     */
    void assignRoles(Long userId, SysUserRoleDTO dto);

    /**
     * 查询用户角色列表
     */
    List<RoleSimpleVO> getUserRoles(Long userId);

    /**
     * 变更用户状态
     */
    void changeStatus(Long userId, SysUserStatusDTO dto);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, SysUserPasswordDTO dto);

    /**
     * 按用户名查询用户（含角色列表），供 module-auth 登录使用。
     * 返回的 VO 不含 password 之外的敏感字段，password 由内部使用。
     */
    SysUserVO findByUsername(String username);

    /**
     * 按手机号查询用户
     */
    SysUserVO findByPhone(String phone);

    /**
     * 更新最后登录时间
     */
    void updateLastLoginTime(Long userId);

    /**
     * 获取当前登录用户信息（含角色编码与权限标识）
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 当前用户修改密码（需校验旧密码）
     *
     * @param userId      当前用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文，长度 6~64）
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
