package com.vibe.auth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.system.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 认证模块独立 SysUserMapper（从 module-system 解耦）
 *
 * <p>module-auth 不直接依赖 module-system 的 {@code SysUserService}，
 * 而是通过本 Mapper 直接查询 sys_user 表，仅保留认证所需的最小查询方法。</p>
 *
 * <p>注意：与 module-system 的 {@code com.vibe.system.mapper.SysUserMapper} 区分：
 * <ul>
 *   <li>本 Mapper 返回 {@link SysUserEntity}（仅基础字段），不关联角色/组织信息</li>
 *   <li>module-system 的 Mapper 返回 {@code SysUserVO}（含角色、组织名称等关联信息）</li>
 * </ul>
 * </p>
 *
 * <p>Bean 名称使用 {@code authSysUserMapper} 显式声明，避免与 module-system 的
 * {@code sysUserMapper} Bean 名称冲突。</p>
 *
 * @author vibe
 */
@Mapper
@Repository("authSysUserMapper")
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    /**
     * 按用户名查询用户（认证用，仅返回基础字段）
     *
     * @param username 用户名
     * @return 用户实体，未找到返回 null
     */
    SysUserEntity findByUsername(@Param("username") String username);

    /**
     * 按手机号查询用户（认证用，仅返回基础字段）
     *
     * @param phone 手机号
     * @return 用户实体，未找到返回 null
     */
    SysUserEntity findByPhone(@Param("phone") String phone);
}
