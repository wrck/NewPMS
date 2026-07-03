package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysRoleDTO;
import com.vibe.system.dto.SysRoleMenuDTO;
import com.vibe.system.dto.SysRoleQueryDTO;
import com.vibe.system.entity.SysMenuEntity;
import com.vibe.system.entity.SysRoleEntity;
import com.vibe.system.entity.SysRoleMenuEntity;
import com.vibe.system.entity.SysUserRoleEntity;
import com.vibe.system.mapper.SysMenuMapper;
import com.vibe.system.mapper.SysRoleMapper;
import com.vibe.system.mapper.SysRoleMenuMapper;
import com.vibe.system.mapper.SysUserRoleMapper;
import com.vibe.system.service.SysRoleService;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public PageResult<SysRoleVO> page(SysRoleQueryDTO query) {
        IPage<SysRoleVO> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());
        IPage<SysRoleVO> result = sysRoleMapper.selectRolePage(page, query);
        if (result.getRecords() != null) {
            for (SysRoleVO vo : result.getRecords()) {
                vo.setMenuIds(sysRoleMapper.selectMenuIdsByRoleId(vo.getId()));
            }
        }
        return PageResult.of(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    @Override
    public List<SysRoleVO> listAll() {
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getStatus, SystemConstant.STATUS_ENABLED)
                .orderByAsc(SysRoleEntity::getId);
        List<SysRoleEntity> list = sysRoleMapper.selectList(wrapper);
        List<SysRoleVO> result = new ArrayList<>(list.size());
        for (SysRoleEntity e : list) {
            result.add(com.vibe.system.converter.SysConverters.toRoleVo(e));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysRoleDTO dto) {
        checkRoleCodeUnique(dto.getRoleCode(), null);
        SysRoleEntity entity = new SysRoleEntity();
        entity.setRoleName(dto.getRoleName());
        entity.setRoleCode(dto.getRoleCode());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus() == null ? SystemConstant.STATUS_ENABLED : dto.getStatus());
        entity.setDataScope(StringUtils.hasText(dto.getDataScope())
                ? dto.getDataScope() : SystemConstant.DATA_SCOPE_ALL);
        sysRoleMapper.insert(entity);

        // permissionCodes → menuIds 转换
        if (dto.getPermissionCodes() != null) {
            List<Long> menuIds = resolveMenuIds(dto.getPermissionCodes());
            saveRoleMenus(entity.getId(), menuIds);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysRoleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "角色ID不能为空");
        }
        SysRoleEntity exist = sysRoleMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        if (SystemConstant.ROLE_SUPER_ADMIN.equals(exist.getRoleCode())
                && dto.getStatus() != null && dto.getStatus() == 0) {
            throw new BusinessException(ResultCode.BUSINESS_CONFLICT, "不允许禁用超级管理员角色");
        }
        if (StringUtils.hasText(dto.getRoleCode()) && !dto.getRoleCode().equals(exist.getRoleCode())) {
            checkRoleCodeUnique(dto.getRoleCode(), dto.getId());
            exist.setRoleCode(dto.getRoleCode());
        }
        exist.setRoleName(dto.getRoleName());
        exist.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            exist.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getDataScope())) {
            exist.setDataScope(dto.getDataScope());
        }
        sysRoleMapper.updateById(exist);

        // permissionCodes → menuIds 转换
        if (dto.getPermissionCodes() != null) {
            List<Long> menuIds = resolveMenuIds(dto.getPermissionCodes());
            saveRoleMenus(dto.getId(), menuIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRoleEntity exist = sysRoleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        if (SystemConstant.ROLE_SUPER_ADMIN.equals(exist.getRoleCode())) {
            throw new BusinessException(ResultCode.BUSINESS_CONFLICT, "不允许删除超级管理员角色");
        }
        // 角色下存在用户则禁止删除
        if (sysUserRoleMapper.countByRoleId(id) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_CONFLICT, "角色下存在关联用户，无法删除");
        }
        sysRoleMapper.deleteById(id);
        sysRoleMenuMapper.deleteByRoleId(id);
    }

    @Override
    public SysRoleVO getDetail(Long id) {
        SysRoleEntity entity = sysRoleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        SysRoleVO vo = com.vibe.system.converter.SysConverters.toRoleVo(entity);
        vo.setMenuIds(sysRoleMapper.selectMenuIdsByRoleId(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, SysRoleMenuDTO dto) {
        if (sysRoleMapper.selectById(roleId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        // permissionCodes → menuIds 转换
        List<Long> menuIds = resolveMenuIds(dto.getPermissionCodes());
        saveRoleMenus(roleId, menuIds);
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return sysRoleMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public List<RoleSimpleVO> getRolesByUserId(Long userId) {
        return sysRoleMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        return sysRoleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<Long> getRoleIdsByCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<SysRoleEntity>()
                .in(SysRoleEntity::getRoleCode, roleCodes);
        List<SysRoleEntity> roles = sysRoleMapper.selectList(wrapper);
        return roles.stream()
                .map(SysRoleEntity::getId)
                .collect(Collectors.toList());
    }

    /* ============ 私有方法 ============ */

    /**
     * 将前端传入的 permissionCodes（权限标识或菜单ID字符串）转换为菜单ID列表。
     *
     * <p>支持两种格式：
     * <ul>
     *   <li>数字字符串（如 ["1","2"]）—— 直接解析为 Long</li>
     *   <li>权限标识（如 ["system:user"]）—— 通过 perms 字段查询菜单ID</li>
     * </ul>
     */
    private List<Long> resolveMenuIds(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = new ArrayList<>();
        List<String> permsToQuery = new ArrayList<>();
        for (String code : permissionCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            try {
                menuIds.add(Long.parseLong(code));
            } catch (NumberFormatException e) {
                permsToQuery.add(code);
            }
        }
        if (!permsToQuery.isEmpty()) {
            LambdaQueryWrapper<SysMenuEntity> wrapper = new LambdaQueryWrapper<SysMenuEntity>()
                    .in(SysMenuEntity::getPerms, permsToQuery);
            List<SysMenuEntity> menus = sysMenuMapper.selectList(wrapper);
            for (SysMenuEntity m : menus) {
                menuIds.add(m.getId());
            }
        }
        return menuIds;
    }

    private void checkRoleCodeUnique(String roleCode, Long excludeId) {
        LambdaQueryWrapper<SysRoleEntity> wrapper = new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getRoleCode, roleCode);
        if (excludeId != null) {
            wrapper.ne(SysRoleEntity::getId, excludeId);
        }
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_DUPLICATED, "角色编码已存在");
        }
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.deleteByRoleId(roleId);
        if (CollectionUtils.isEmpty(menuIds)) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenuEntity rm = new SysRoleMenuEntity();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            sysRoleMenuMapper.insert(rm);
        }
    }
}
