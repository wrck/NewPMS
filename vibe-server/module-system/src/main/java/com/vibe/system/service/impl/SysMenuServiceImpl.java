package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.converter.SysConverters;
import com.vibe.system.dto.SysMenuDTO;
import com.vibe.system.entity.SysMenuEntity;
import com.vibe.system.mapper.SysMenuMapper;
import com.vibe.system.service.SysMenuService;
import com.vibe.system.service.SysRoleService;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 菜单权限服务实现
 *
 * @author vibe
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleService sysRoleService;

    @Override
    public List<SysMenuVO> listTree() {
        List<SysMenuVO> all = sysMenuMapper.selectAllMenuVo();
        return SysConverters.buildMenuTree(all);
    }

    @Override
    public List<SysMenuVO> listAll() {
        return sysMenuMapper.selectAllMenuVo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysMenuDTO dto) {
        SysMenuEntity entity = new SysMenuEntity();
        copyDtoToEntity(dto, entity);
        sysMenuMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysMenuDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "菜单ID不能为空");
        }
        SysMenuEntity exist = sysMenuMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜单不存在");
        }
        // 不允许将自身设为父节点
        if (dto.getParentId() != null && dto.getParentId().equals(dto.getId())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "父菜单不能为自身");
        }
        copyDtoToEntity(dto, exist);
        sysMenuMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 存在子菜单则禁止删除
        LambdaQueryWrapper<SysMenuEntity> childWrapper = new LambdaQueryWrapper<SysMenuEntity>()
                .eq(SysMenuEntity::getParentId, id);
        if (sysMenuMapper.selectCount(childWrapper) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_CONFLICT, "存在子菜单，无法删除");
        }
        sysMenuMapper.deleteById(id);
    }

    @Override
    public SysMenuVO getDetail(Long id) {
        SysMenuEntity entity = sysMenuMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜单不存在");
        }
        return SysConverters.toMenuVo(entity);
    }

    @Override
    public List<SysMenuVO> getMenusByRoleId(Long roleId) {
        List<Long> roleIds = new ArrayList<>();
        roleIds.add(roleId);
        List<SysMenuVO> menus = sysMenuMapper.selectMenusByRoleIds(roleIds);
        // 仅返回 MENU 类型（按钮不出现在路由树）
        List<SysMenuVO> filtered = new ArrayList<>();
        for (SysMenuVO m : menus) {
            if (SystemConstant.MENU_TYPE_MENU.equals(m.getMenuType())) {
                filtered.add(m);
            }
        }
        return SysConverters.buildMenuTree(filtered);
    }

    @Override
    public List<SysMenuVO> getMenusByUserId(Long userId) {
        UserContext ctx = UserContextHolder.get();
        // 超级管理员返回全部菜单
        boolean isSuperAdmin = ctx != null && ctx.isSuperAdmin();
        List<SysMenuVO> menus;
        if (isSuperAdmin) {
            menus = sysMenuMapper.selectAllMenuVo();
        } else {
            List<RoleSimpleVO> roles = sysRoleService.getRolesByUserId(userId);
            if (CollectionUtils.isEmpty(roles)) {
                return Collections.emptyList();
            }
            List<Long> roleIds = new ArrayList<>(roles.size());
            for (RoleSimpleVO r : roles) {
                roleIds.add(r.getId());
            }
            menus = sysMenuMapper.selectMenusByRoleIds(roleIds);
        }
        // 仅返回可见的 MENU 类型
        List<SysMenuVO> filtered = new ArrayList<>();
        for (SysMenuVO m : menus) {
            if (SystemConstant.MENU_TYPE_MENU.equals(m.getMenuType())
                    && (m.getVisible() == null || m.getVisible() == 1)) {
                filtered.add(m);
            }
        }
        return SysConverters.buildMenuTree(filtered);
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        UserContext ctx = UserContextHolder.get();
        if (ctx != null && ctx.isSuperAdmin()) {
            // 超管拥有全部权限标识
            LambdaQueryWrapper<SysMenuEntity> wrapper = new LambdaQueryWrapper<SysMenuEntity>()
                    .isNotNull(SysMenuEntity::getPerms)
                    .ne(SysMenuEntity::getPerms, "");
            List<SysMenuEntity> all = sysMenuMapper.selectList(wrapper);
            List<String> perms = new ArrayList<>(all.size());
            for (SysMenuEntity m : all) {
                perms.add(m.getPerms());
            }
            return perms;
        }
        List<String> perms = sysMenuMapper.selectPermissionsByUserId(userId);
        return perms != null ? perms : Collections.emptyList();
    }

    @Override
    public List<Long> getMenuIdsByPermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = new ArrayList<>();
        List<String> permsToQuery = new ArrayList<>();
        // 区分数字字符串（菜单ID）和权限标识
        for (String code : permissionCodes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            try {
                menuIds.add(Long.parseLong(code));
            } catch (NumberFormatException e) {
                // 非数字，作为权限标识查询
                permsToQuery.add(code);
            }
        }
        // 按权限标识查询菜单ID
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

    private void copyDtoToEntity(SysMenuDTO dto, SysMenuEntity entity) {
        entity.setParentId(dto.getParentId() == null ? SystemConstant.ROOT_PARENT_ID : dto.getParentId());
        entity.setMenuName(dto.getMenuName());
        entity.setMenuType(dto.getMenuType());
        entity.setPath(dto.getPath());
        entity.setComponent(dto.getComponent());
        entity.setPerms(dto.getPerms());
        entity.setIcon(dto.getIcon());
        entity.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        entity.setVisible(dto.getVisible() == null ? SystemConstant.STATUS_ENABLED : dto.getVisible());
    }
}
