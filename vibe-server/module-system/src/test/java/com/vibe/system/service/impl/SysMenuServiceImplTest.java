package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vibe.common.context.UserContext;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.dto.SysMenuDTO;
import com.vibe.system.entity.SysMenuEntity;
import com.vibe.system.entity.SysRoleMenuEntity;
import com.vibe.system.mapper.SysMenuMapper;
import com.vibe.system.mapper.SysRoleMenuMapper;
import com.vibe.system.service.SysRoleService;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysMenuVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 菜单服务实现单元测试（Task 3 SubTask 3.7）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>listTree / listAll：树形构建与扁平列表</li>
 *   <li>create：parentId 默认值、visible 默认值、sortOrder 默认值</li>
 *   <li>update：id 为空、菜单不存在、parentId 不能为自身</li>
 *   <li>delete：存在子菜单时禁止删除</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>getMenusByRoleId：仅返回 MENU 类型并构建树</li>
 *   <li>getMenusByUserId：超管返回全部菜单、普通用户按角色查询、空角色返回空列表</li>
 *   <li>getPermissionsByUserId：超管返回全部权限标识</li>
 *   <li>getMenuIdsByPermissionCodes：数字直接解析、权限标识查询、null/空边界</li>
 *   <li>assignRolesToMenu：菜单不存在校验、覆盖式分配</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("菜单服务 SysMenuServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class SysMenuServiceImplTest {

    @Mock
    private SysMenuMapper sysMenuMapper;
    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Mock
    private SysRoleService sysRoleService;

    @InjectMocks
    private SysMenuServiceImpl menuService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /* ============ listTree / listAll ============ */

    @Nested
    @DisplayName("listTree / listAll 查询")
    class ListTest {

        @Test
        @DisplayName("listAll 直接返回 Mapper 结果")
        void should_list_all_menus() {
            SysMenuVO m1 = buildMenuVo(1L, 0L, "系统管理", SystemConstant.MENU_TYPE_MENU);
            SysMenuVO m2 = buildMenuVo(2L, 1L, "用户管理", SystemConstant.MENU_TYPE_MENU);
            when(sysMenuMapper.selectAllMenuVo()).thenReturn(List.of(m1, m2));

            List<SysMenuVO> result = menuService.listAll();

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("listTree 将扁平列表构建为树形：根节点的 children 已填充")
        void should_build_menu_tree() {
            SysMenuVO root = buildMenuVo(1L, 0L, "系统管理", SystemConstant.MENU_TYPE_MENU);
            SysMenuVO child = buildMenuVo(2L, 1L, "用户管理", SystemConstant.MENU_TYPE_MENU);
            SysMenuVO grandchild = buildMenuVo(3L, 2L, "用户新增", SystemConstant.MENU_TYPE_BUTTON);
            when(sysMenuMapper.selectAllMenuVo()).thenReturn(List.of(root, child, grandchild));

            List<SysMenuVO> tree = menuService.listTree();

            assertEquals(1, tree.size());
            assertEquals("系统管理", tree.get(0).getMenuName());
            assertNotNull(tree.get(0).getChildren());
            assertEquals(1, tree.get(0).getChildren().size());
            assertEquals("用户管理", tree.get(0).getChildren().get(0).getMenuName());
            // 孙子节点应挂在子节点下
            assertEquals("用户新增", tree.get(0).getChildren().get(0).getChildren().get(0).getMenuName());
        }

        @Test
        @DisplayName("listTree 空列表返回空根列表")
        void should_return_empty_tree_when_no_menus() {
            when(sysMenuMapper.selectAllMenuVo()).thenReturn(Collections.emptyList());

            List<SysMenuVO> tree = menuService.listTree();

            assertNotNull(tree);
            assertTrue(tree.isEmpty());
        }
    }

    /* ============ create ============ */

    @Nested
    @DisplayName("create 创建菜单")
    class CreateTest {

        @Test
        @DisplayName("正常创建：parentId 为 null 时默认 0、visible 为 null 时默认 1、sortOrder 为 null 时默认 0")
        void should_create_menu_with_defaults() {
            SysMenuDTO dto = new SysMenuDTO();
            dto.setMenuName("用户管理");
            dto.setMenuType(SystemConstant.MENU_TYPE_MENU);
            // parentId / visible / sortOrder 均为 null
            when(sysMenuMapper.insert(any(SysMenuEntity.class))).thenAnswer(invocation -> {
                SysMenuEntity e = invocation.getArgument(0);
                e.setId(100L);
                return 1;
            });

            Long id = menuService.create(dto);

            assertEquals(100L, id);
            ArgumentCaptor<SysMenuEntity> captor = ArgumentCaptor.forClass(SysMenuEntity.class);
            verify(sysMenuMapper).insert(captor.capture());
            assertAll("字段映射与默认值",
                    () -> assertEquals("用户管理", captor.getValue().getMenuName()),
                    () -> assertEquals(SystemConstant.MENU_TYPE_MENU, captor.getValue().getMenuType()),
                    () -> assertEquals(SystemConstant.ROOT_PARENT_ID, captor.getValue().getParentId()),
                    () -> assertEquals(SystemConstant.STATUS_ENABLED, captor.getValue().getVisible()),
                    () -> assertEquals(0, captor.getValue().getSortOrder())
            );
        }

        @Test
        @DisplayName("parentId / visible / sortOrder 显式传入时按传入值保存")
        void should_save_explicit_values() {
            SysMenuDTO dto = new SysMenuDTO();
            dto.setMenuName("新增用户");
            dto.setMenuType(SystemConstant.MENU_TYPE_BUTTON);
            dto.setParentId(5L);
            dto.setVisible(0);
            dto.setSortOrder(99);
            dto.setPerms("system:user:add");
            when(sysMenuMapper.insert(any(SysMenuEntity.class))).thenAnswer(invocation -> {
                SysMenuEntity e = invocation.getArgument(0);
                e.setId(1L);
                return 1;
            });

            menuService.create(dto);

            ArgumentCaptor<SysMenuEntity> captor = ArgumentCaptor.forClass(SysMenuEntity.class);
            verify(sysMenuMapper).insert(captor.capture());
            assertAll("显式值",
                    () -> assertEquals(5L, captor.getValue().getParentId()),
                    () -> assertEquals(0, captor.getValue().getVisible()),
                    () -> assertEquals(99, captor.getValue().getSortOrder()),
                    () -> assertEquals("system:user:add", captor.getValue().getPerms())
            );
        }
    }

    /* ============ update ============ */

    @Nested
    @DisplayName("update 更新菜单")
    class UpdateTest {

        @Test
        @DisplayName("id 为空抛 PARAM_MISSING")
        void should_throw_when_id_null() {
            SysMenuDTO dto = new SysMenuDTO();
            dto.setMenuName("x");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> menuService.update(dto));

            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
            verify(sysMenuMapper, never()).updateById(any(SysMenuEntity.class));
        }

        @Test
        @DisplayName("菜单不存在抛 NOT_FOUND")
        void should_throw_when_menu_not_exist() {
            SysMenuDTO dto = new SysMenuDTO();
            dto.setId(99L);
            dto.setMenuName("x");
            when(sysMenuMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> menuService.update(dto));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("parentId 等于自身 id 抛 PARAM_INVALID")
        void should_throw_when_parent_id_equals_self_id() {
            SysMenuDTO dto = new SysMenuDTO();
            dto.setId(5L);
            dto.setParentId(5L);
            dto.setMenuName("x");
            SysMenuEntity exist = new SysMenuEntity();
            exist.setId(5L);
            when(sysMenuMapper.selectById(5L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> menuService.update(dto));

            assertEquals(ResultCode.PARAM_INVALID.getCode(), ex.getCode());
            verify(sysMenuMapper, never()).updateById(any(SysMenuEntity.class));
        }

        @Test
        @DisplayName("正常更新：写入字段并调用 updateById")
        void should_update_menu() {
            SysMenuDTO dto = new SysMenuDTO();
            dto.setId(5L);
            dto.setMenuName("新名称");
            dto.setMenuType(SystemConstant.MENU_TYPE_MENU);
            dto.setParentId(1L);
            SysMenuEntity exist = new SysMenuEntity();
            exist.setId(5L);
            when(sysMenuMapper.selectById(5L)).thenReturn(exist);

            menuService.update(dto);

            verify(sysMenuMapper).updateById(any(SysMenuEntity.class));
            assertEquals("新名称", exist.getMenuName());
            assertEquals(1L, exist.getParentId());
        }
    }

    /* ============ delete ============ */

    @Nested
    @DisplayName("delete 删除菜单")
    class DeleteTest {

        @Test
        @DisplayName("存在子菜单时禁止删除，抛 BUSINESS_CONFLICT")
        void should_throw_when_has_children() {
            when(sysMenuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> menuService.delete(1L));

            assertEquals(ResultCode.BUSINESS_CONFLICT.getCode(), ex.getCode());
            verify(sysMenuMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("无子菜单时正常删除")
        void should_delete_when_no_children() {
            when(sysMenuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            menuService.delete(5L);

            verify(sysMenuMapper).deleteById(5L);
        }
    }

    /* ============ getDetail ============ */

    @Nested
    @DisplayName("getDetail 查询详情")
    class GetDetailTest {

        @Test
        @DisplayName("菜单不存在抛 NOT_FOUND")
        void should_throw_when_not_found() {
            when(sysMenuMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> menuService.getDetail(99L));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常返回 VO")
        void should_return_detail() {
            SysMenuEntity e = new SysMenuEntity();
            e.setId(1L);
            e.setMenuName("用户管理");
            e.setMenuType(SystemConstant.MENU_TYPE_MENU);
            when(sysMenuMapper.selectById(1L)).thenReturn(e);

            SysMenuVO vo = menuService.getDetail(1L);

            assertNotNull(vo);
            assertEquals("用户管理", vo.getMenuName());
        }
    }

    /* ============ getMenusByRoleId ============ */

    @Nested
    @DisplayName("getMenusByRoleId 按角色查询菜单")
    class GetMenusByRoleIdTest {

        @Test
        @DisplayName("仅返回 MENU 类型并构建树，BUTTON 类型被过滤掉")
        void should_filter_only_menu_type_and_build_tree() {
            SysMenuVO root = buildMenuVo(1L, 0L, "系统管理", SystemConstant.MENU_TYPE_MENU);
            SysMenuVO child = buildMenuVo(2L, 1L, "用户管理", SystemConstant.MENU_TYPE_MENU);
            SysMenuVO button = buildMenuVo(3L, 2L, "新增", SystemConstant.MENU_TYPE_BUTTON);
            when(sysMenuMapper.selectMenusByRoleIds(any())).thenReturn(List.of(root, child, button));

            List<SysMenuVO> tree = menuService.getMenusByRoleId(10L);

            assertEquals(1, tree.size());
            assertEquals("系统管理", tree.get(0).getMenuName());
            assertEquals(1, tree.get(0).getChildren().size());
            // BUTTON 类型不应出现在结果中
            assertEquals("用户管理", tree.get(0).getChildren().get(0).getMenuName());
        }
    }

    /* ============ getMenusByUserId ============ */

    @Nested
    @DisplayName("getMenusByUserId 按用户查询菜单")
    class GetMenusByUserIdTest {

        @Test
        @DisplayName("超级管理员返回全部可见 MENU 类型")
        void should_return_all_for_super_admin() {
            UserContextHolder.set(UserContext.builder()
                    .userId(1L).roles(List.of("SUPER_ADMIN")).build());
            SysMenuVO root = buildMenuVo(1L, 0L, "系统管理", SystemConstant.MENU_TYPE_MENU);
            root.setVisible(1);
            SysMenuVO hidden = buildMenuVo(2L, 0L, "隐藏菜单", SystemConstant.MENU_TYPE_MENU);
            hidden.setVisible(0);
            SysMenuVO button = buildMenuVo(3L, 0L, "按钮", SystemConstant.MENU_TYPE_BUTTON);
            when(sysMenuMapper.selectAllMenuVo()).thenReturn(List.of(root, hidden, button));

            List<SysMenuVO> tree = menuService.getMenusByUserId(1L);

            // 仅 visible=1 的 MENU 类型应出现在树中
            assertEquals(1, tree.size());
            assertEquals("系统管理", tree.get(0).getMenuName());
        }

        @Test
        @DisplayName("普通用户通过角色查询菜单，无角色返回空列表")
        void should_return_empty_when_no_roles() {
            UserContextHolder.set(UserContext.builder().userId(2L).build());
            when(sysRoleService.getRolesByUserId(2L)).thenReturn(Collections.emptyList());

            List<SysMenuVO> tree = menuService.getMenusByUserId(2L);

            assertNotNull(tree);
            assertTrue(tree.isEmpty());
            verify(sysMenuMapper, never()).selectMenusByRoleIds(any());
        }

        @Test
        @DisplayName("普通用户通过角色查询菜单并构建树")
        void should_query_menus_by_role_ids_for_normal_user() {
            UserContextHolder.set(UserContext.builder().userId(2L).build());
            RoleSimpleVO r = new RoleSimpleVO();
            r.setId(10L);
            when(sysRoleService.getRolesByUserId(2L)).thenReturn(List.of(r));
            SysMenuVO m = buildMenuVo(1L, 0L, "用户管理", SystemConstant.MENU_TYPE_MENU);
            m.setVisible(1);
            when(sysMenuMapper.selectMenusByRoleIds(any())).thenReturn(List.of(m));

            List<SysMenuVO> tree = menuService.getMenusByUserId(2L);

            assertEquals(1, tree.size());
            assertEquals("用户管理", tree.get(0).getMenuName());
        }
    }

    /* ============ getPermissionsByUserId ============ */

    @Nested
    @DisplayName("getPermissionsByUserId 按用户查询权限标识")
    class GetPermissionsByUserIdTest {

        @Test
        @DisplayName("超级管理员返回全部非空权限标识")
        void should_return_all_perms_for_super_admin() {
            UserContextHolder.set(UserContext.builder()
                    .userId(1L).roles(List.of("SUPER_ADMIN")).build());
            // 模拟 SQL 已过滤掉空 perms：Mapper 只返回非空 perms 的菜单
            SysMenuEntity m1 = new SysMenuEntity();
            m1.setPerms("system:user");
            SysMenuEntity m2 = new SysMenuEntity();
            m2.setPerms("system:role");
            when(sysMenuMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(m1, m2));

            List<String> perms = menuService.getPermissionsByUserId(1L);

            assertEquals(2, perms.size());
            assertTrue(perms.contains("system:user"));
            assertTrue(perms.contains("system:role"));
        }

        @Test
        @DisplayName("普通用户委托 Mapper 查询")
        void should_delegate_to_mapper_for_normal_user() {
            UserContextHolder.set(UserContext.builder().userId(2L).build());
            when(sysMenuMapper.selectPermissionsByUserId(2L))
                    .thenReturn(List.of("project:view"));

            List<String> perms = menuService.getPermissionsByUserId(2L);

            assertEquals(1, perms.size());
            assertEquals("project:view", perms.get(0));
        }

        @Test
        @DisplayName("Mapper 返回 null 时返回空列表")
        void should_return_empty_when_mapper_returns_null() {
            UserContextHolder.set(UserContext.builder().userId(2L).build());
            when(sysMenuMapper.selectPermissionsByUserId(2L)).thenReturn(null);

            List<String> perms = menuService.getPermissionsByUserId(2L);

            assertNotNull(perms);
            assertTrue(perms.isEmpty());
        }
    }

    /* ============ getMenuIdsByPermissionCodes ============ */

    @Nested
    @DisplayName("getMenuIdsByPermissionCodes 权限标识转菜单ID")
    class GetMenuIdsByPermissionCodesTest {

        @Test
        @DisplayName("入参为 null 返回空列表")
        void should_return_empty_when_null() {
            List<Long> result = menuService.getMenuIdsByPermissionCodes(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("入参为空列表返回空列表")
        void should_return_empty_when_empty() {
            List<Long> result = menuService.getMenuIdsByPermissionCodes(Collections.emptyList());

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("数字字符串直接解析为 menuId")
        void should_parse_numeric_codes() {
            List<Long> result = menuService.getMenuIdsByPermissionCodes(List.of("10", "20", "30"));

            assertEquals(3, result.size());
            assertTrue(result.contains(10L));
            assertTrue(result.contains(20L));
            assertTrue(result.contains(30L));
            // 数字字符串不需要查询 Mapper
            verify(sysMenuMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("权限标识通过 sysMenuMapper 查询 menuId")
        void should_query_menus_for_perms() {
            SysMenuEntity m = new SysMenuEntity();
            m.setId(50L);
            when(sysMenuMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(m));

            List<Long> result = menuService.getMenuIdsByPermissionCodes(List.of("system:user"));

            assertEquals(1, result.size());
            assertTrue(result.contains(50L));
        }

        @Test
        @DisplayName("空字符串与 null 元素被跳过")
        void should_skip_blank_and_null_elements() {
            // Arrays.asList 允许 null 元素（List.of 不允许）
            List<Long> result = menuService.getMenuIdsByPermissionCodes(Arrays.asList("10", "", "  ", null));

            assertEquals(1, result.size());
            assertTrue(result.contains(10L));
        }
    }

    /* ============ getRolesByMenuId / assignRolesToMenu ============ */

    @Nested
    @DisplayName("getRolesByMenuId / assignRolesToMenu")
    class RoleAssignmentTest {

        @Test
        @DisplayName("getRolesByMenuId menuId 为 null 返回空列表")
        void should_return_empty_when_menu_id_null() {
            List<RoleSimpleVO> result = menuService.getRolesByMenuId(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("getRolesByMenuId 正常返回角色列表")
        void should_return_roles_for_menu() {
            RoleSimpleVO r = new RoleSimpleVO();
            r.setId(1L);
            when(sysRoleMenuMapper.selectRolesByMenuId(10L)).thenReturn(List.of(r));

            List<RoleSimpleVO> result = menuService.getRolesByMenuId(10L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("getRolesByMenuId Mapper 返回 null 时返回空列表")
        void should_return_empty_when_mapper_returns_null() {
            when(sysRoleMenuMapper.selectRolesByMenuId(10L)).thenReturn(null);

            List<RoleSimpleVO> result = menuService.getRolesByMenuId(10L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("assignRolesToMenu menuId 为 null 抛 PARAM_MISSING")
        void should_throw_when_menu_id_null() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> menuService.assignRolesToMenu(null, List.of(1L)));

            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("assignRolesToMenu 菜单不存在抛 NOT_FOUND")
        void should_throw_when_menu_not_exist() {
            when(sysMenuMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> menuService.assignRolesToMenu(99L, List.of(1L)));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(sysRoleMenuMapper, never()).deleteByMenuId(anyLong());
        }

        @Test
        @DisplayName("assignRolesToMenu roleIds 为空时仅清除旧关联")
        void should_only_clear_when_role_ids_empty() {
            when(sysMenuMapper.selectById(10L)).thenReturn(new SysMenuEntity());

            menuService.assignRolesToMenu(10L, Collections.emptyList());

            verify(sysRoleMenuMapper).deleteByMenuId(10L);
            verify(sysRoleMenuMapper, never()).insert(any(SysRoleMenuEntity.class));
        }

        @Test
        @DisplayName("assignRolesToMenu 正常分配：先删旧再插新，跳过 null roleId")
        void should_assign_roles_to_menu() {
            when(sysMenuMapper.selectById(10L)).thenReturn(new SysMenuEntity());
            // Arrays.asList 允许 null 元素（List.of 不允许）
            menuService.assignRolesToMenu(10L, Arrays.asList(1L, null, 2L));

            verify(sysRoleMenuMapper).deleteByMenuId(10L);
            // null roleId 被跳过，仅插入 2 条
            verify(sysRoleMenuMapper, org.mockito.Mockito.times(2)).insert(any(SysRoleMenuEntity.class));
        }
    }

    /* ============ 测试辅助方法 ============ */

    private SysMenuVO buildMenuVo(Long id, Long parentId, String name, String type) {
        SysMenuVO vo = new SysMenuVO();
        vo.setId(id);
        vo.setParentId(parentId);
        vo.setMenuName(name);
        vo.setMenuType(type);
        vo.setVisible(1);
        return vo;
    }
}
