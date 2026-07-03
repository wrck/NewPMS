package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysUserQueryDTO;
import com.vibe.system.entity.SysUserEntity;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    /**
     * 按用户名查询用户（含角色列表），供登录使用
     */
    SysUserVO selectByUsernameWithRoles(@Param("username") String username);

    /**
     * 按手机号查询用户（含角色列表）
     */
    SysUserVO selectByPhoneWithRoles(@Param("phone") String phone);

    /**
     * 按 ID 查询用户（含角色列表与组织名称）
     */
    SysUserVO selectVoById(@Param("id") Long id);

    /**
     * 分页查询用户（含角色、组织信息）
     */
    IPage<SysUserVO> selectUserPage(IPage<SysUserVO> page, @Param("query") SysUserQueryDTO query);

    /**
     * 查询用户关联的角色列表
     */
    List<RoleSimpleVO> selectRolesByUserId(@Param("userId") Long userId);
}
