package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.constant.CommonConstant;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysUserDTO;
import com.vibe.system.dto.SysUserPasswordDTO;
import com.vibe.system.dto.SysUserQueryDTO;
import com.vibe.system.dto.SysUserStatusDTO;
import com.vibe.system.dto.SysUserRoleDTO;
import com.vibe.system.entity.SysUserEntity;
import com.vibe.system.entity.SysUserRoleEntity;
import com.vibe.system.mapper.SysUserMapper;
import com.vibe.system.mapper.SysUserRoleMapper;
import com.vibe.system.service.SysMenuService;
import com.vibe.system.service.SysRoleService;
import com.vibe.system.service.SysUserService;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysUserVO;
import com.vibe.system.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 系统用户服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;

    /**
     * BCrypt 密码编码器。Bean 由 module-auth 的 {@code SecurityConfig#passwordEncoder} 暴露，
     * 在 bootstrap 聚合后运行时注入；module-system 编译期仅依赖 spring-security-core 接口。
     */
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUserVO> page(SysUserQueryDTO query) {
        IPage<SysUserVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysUserVO> result = sysUserMapper.selectUserPage(page, query);
        // 填充角色列表
        if (result.getRecords() != null) {
            for (SysUserVO vo : result.getRecords()) {
                vo.setRoles(sysUserMapper.selectRolesByUserId(vo.getId()));
            }
        }
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUserDTO dto) {
        // 唯一性校验
        checkUsernameUnique(dto.getUsername(), null);
        if (StringUtils.hasText(dto.getPhone())) {
            checkPhoneUnique(dto.getPhone(), null);
        }
        SysUserEntity entity = new SysUserEntity();
        entity.setUsername(dto.getUsername());
        // 密码 BCrypt 加密；未填则使用默认密码
        String rawPwd = StringUtils.hasText(dto.getPassword())
                ? dto.getPassword() : SystemConstant.DEFAULT_PASSWORD;
        entity.setPassword(passwordEncoder.encode(rawPwd));
        entity.setRealName(dto.getRealName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setAvatar(dto.getAvatar());
        entity.setStatus(resolveStatus(dto.getStatus(), SystemConstant.USER_STATUS_ACTIVE));
        entity.setTenantType(StringUtils.hasText(dto.getTenantType())
                ? dto.getTenantType() : SystemConstant.TENANT_INTERNAL);
        entity.setTenantId(dto.getTenantId());
        entity.setOrgId(dto.getOrgId());
        sysUserMapper.insert(entity);

        // 保存角色关联（roleCodes → roleIds 转换）
        List<Long> roleIds = sysRoleService.getRoleIdsByCodes(dto.getRoleCodes());
        saveUserRoles(entity.getId(), roleIds);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "用户ID不能为空");
        }
        SysUserEntity exist = sysUserMapper.selectById(dto.getId());
        if (exist == null) {
            throw BusinessException.of(ResultCode.USER_NOT_FOUND);
        }
        if (StringUtils.hasText(dto.getUsername())
                && !dto.getUsername().equals(exist.getUsername())) {
            checkUsernameUnique(dto.getUsername(), dto.getId());
            exist.setUsername(dto.getUsername());
        }
        if (StringUtils.hasText(dto.getPhone()) && !dto.getPhone().equals(exist.getPhone())) {
            checkPhoneUnique(dto.getPhone(), dto.getId());
            exist.setPhone(dto.getPhone());
        }
        exist.setRealName(dto.getRealName());
        exist.setEmail(dto.getEmail());
        exist.setAvatar(dto.getAvatar());
        if (dto.getStatus() != null) {
            exist.setStatus(resolveStatus(dto.getStatus(), null));
        }
        if (StringUtils.hasText(dto.getTenantType())) {
            exist.setTenantType(dto.getTenantType());
        }
        exist.setTenantId(dto.getTenantId());
        exist.setOrgId(dto.getOrgId());
        sysUserMapper.updateById(exist);

        // 重新分配角色（roleCodes → roleIds 转换）
        if (dto.getRoleCodes() != null) {
            List<Long> roleIds = sysRoleService.getRoleIdsByCodes(dto.getRoleCodes());
            saveUserRoles(dto.getId(), roleIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "用户ID不能为空");
        }
        // 不允许删除超级管理员（id=1）
        if (id == 1L) {
            throw new BusinessException(ResultCode.BUSINESS_CONFLICT, "不允许删除超级管理员账号");
        }
        SysUserEntity exist = sysUserMapper.selectById(id);
        if (exist == null) {
            throw BusinessException.of(ResultCode.USER_NOT_FOUND);
        }
        sysUserMapper.deleteById(id);
        // 清理角色关联（逻辑删除）
        sysUserRoleMapper.deleteByUserId(id);
    }

    @Override
    public SysUserVO getDetail(Long id) {
        SysUserVO vo = sysUserMapper.selectVoById(id);
        if (vo == null) {
            throw BusinessException.of(ResultCode.USER_NOT_FOUND);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, SysUserRoleDTO dto) {
        if (sysUserMapper.selectById(userId) == null) {
            throw BusinessException.of(ResultCode.USER_NOT_FOUND);
        }
        // roleCodes → roleIds 转换
        List<Long> roleIds = sysRoleService.getRoleIdsByCodes(dto.getRoleCodes());
        saveUserRoles(userId, roleIds);
    }

    @Override
    public List<RoleSimpleVO> getUserRoles(Long userId) {
        return sysUserMapper.selectRolesByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long userId, SysUserStatusDTO dto) {
        SysUserEntity exist = sysUserMapper.selectById(userId);
        if (exist == null) {
            throw BusinessException.of(ResultCode.USER_NOT_FOUND);
        }
        SysUserEntity update = new SysUserEntity();
        update.setId(userId);
        update.setStatus(resolveStatus(dto.getStatus(), null));
        sysUserMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, SysUserPasswordDTO dto) {
        SysUserEntity exist = sysUserMapper.selectById(userId);
        if (exist == null) {
            throw BusinessException.of(ResultCode.USER_NOT_FOUND);
        }
        SysUserEntity update = new SysUserEntity();
        update.setId(userId);
        String rawPwd = StringUtils.hasText(dto.getNewPassword())
                ? dto.getNewPassword() : SystemConstant.DEFAULT_PASSWORD;
        update.setPassword(passwordEncoder.encode(rawPwd));
        sysUserMapper.updateById(update);
    }

    @Override
    public SysUserVO findByUsername(String username) {
        return sysUserMapper.selectByUsernameWithRoles(username);
    }

    @Override
    public SysUserVO findByPhone(String phone) {
        return sysUserMapper.selectByPhoneWithRoles(phone);
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        if (userId == null) {
            return;
        }
        SysUserEntity update = new SysUserEntity();
        update.setId(userId);
        update.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(update);
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }
        SysUserVO userVo = sysUserMapper.selectVoById(ctx.getUserId());
        if (userVo == null) {
            throw BusinessException.of(ResultCode.USER_NOT_FOUND);
        }
        UserInfoVO info = new UserInfoVO();
        info.setUserId(userVo.getId());
        info.setUserName(userVo.getUsername());
        info.setRealName(userVo.getRealName());
        info.setPhone(userVo.getPhone());
        info.setEmail(userVo.getEmail());
        info.setAvatar(userVo.getAvatar());
        info.setStatus(userVo.getStatus());
        info.setTenantType(userVo.getTenantType());
        info.setTenantId(userVo.getTenantId());
        info.setOrgId(userVo.getOrgId());
        info.setOrgName(userVo.getOrgName());
        info.setLastLoginTime(userVo.getLastLoginTime());

        List<String> roleCodes = sysRoleService.getRoleCodesByUserId(ctx.getUserId());
        info.setRoles(roleCodes != null ? roleCodes : Collections.emptyList());

        List<String> perms = sysMenuService.getPermissionsByUserId(ctx.getUserId());
        info.setPermissions(perms != null ? perms : Collections.emptyList());
        return info;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }
        SysUserEntity exist = sysUserMapper.selectById(userId);
        if (exist == null) {
            throw BusinessException.of(ResultCode.USER_NOT_FOUND);
        }
        // 校验旧密码
        if (!StringUtils.hasText(oldPassword)
                || !passwordEncoder.matches(oldPassword, exist.getPassword())) {
            throw BusinessException.of(ResultCode.PASSWORD_ERROR);
        }
        // 更新为新密码
        SysUserEntity update = new SysUserEntity();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(update);
    }

    /* ============ 私有方法 ============ */

    /**
     * 将前端传入的数字状态（1/0）转为实体状态字符串（ACTIVE/DISABLED）。
     *
     * @param status        前端传入的状态值（1-启用 0-禁用，可为 null）
     * @param defaultStatus 当 status 为 null 时使用的默认值（如 "ACTIVE"）
     * @return 实体状态字符串
     */
    private String resolveStatus(Integer status, String defaultStatus) {
        if (status == null) {
            return defaultStatus;
        }
        return status == 1 ? SystemConstant.USER_STATUS_ACTIVE : SystemConstant.USER_STATUS_DISABLED;
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUserEntity::getId, excludeId);
        }
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "登录账号已存在");
        }
    }

    private void checkPhoneUnique(String phone, Long excludeId) {
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getPhone, phone);
        if (excludeId != null) {
            wrapper.ne(SysUserEntity::getId, excludeId);
        }
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "手机号已被使用");
        }
    }

    /**
     * 保存用户角色关联：先逻辑删除旧关联，再批量插入新关联
     */
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.deleteByUserId(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<SysUserRoleEntity> list = new ArrayList<>(roleIds.size());
        for (Long roleId : roleIds) {
            SysUserRoleEntity ur = new SysUserRoleEntity();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        // MyBatis-Plus 批量插入
        for (SysUserRoleEntity ur : list) {
            sysUserRoleMapper.insert(ur);
        }
    }
}
