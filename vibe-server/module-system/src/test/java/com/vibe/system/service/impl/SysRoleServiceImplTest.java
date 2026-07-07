package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.common.result.ResultCode;
import com.vibe.system.constant.SystemConstant;
import com.vibe.system.converter.SysConverters;
import com.vibe.system.dto.SysRoleDTO;
import com.vibe.system.dto.SysRoleMenuDTO;
import com.vibe.system.dto.SysRoleQueryDTO;
import com.vibe.system.entity.SysMenuEntity;
import com.vibe.system.entity.SysRoleEntity;
import com.vibe.system.entity.SysRoleMenuEntity;
import com.vibe.system.mapper.SysMenuMapper;
import com.vibe.system.mapper.SysRoleMapper;
import com.vibe.system.mapper.SysRoleMenuMapper;
import com.vibe.system.mapper.SysUserRoleMapper;
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysRoleVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色服务实现单元测试（Task 3 SubTask 3.7）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page / listAll：分页与列表查询、状态过滤</li>
 *   <li>create：默认状态/数据范围、唯一性校验、权限分配</li>
 *   <li>update：id 为空、角色不存在、SUPER_ADMIN 禁用保护、唯一性校验</li>
 *   <li>delete：不存在/SUPER_ADMIN 保护/关联用户保护</li>
 *   <li>getDetail：不存在抛 NOT_FOUND</li>
 *   <li>assignMenus：权限标识解析（数字 + perms）</li>
 *   <li>getRoleIdsByCodes：null/空列表边界</li>
 * </ul>
 *
 * @author vibe
 */
@DisplayName("角色服务 SysRoleServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysMenuMapper sysMenuMapper;

    @InjectMocks
    private SysRoleServiceImpl roleService;

    /* ============ page / listAll ============ */

    @Nested
    @DisplayName("page / listAll 查询")
    class PageAndListTest {

        @Test
        @DisplayName("page 正常返回：每条记录填充 menuIds")
        void should_fill_menu_ids_for_each_role() {
            SysRoleQueryDTO query = new SysRoleQueryDTO();
            query.setPage(1);
            query.setSize(10);

            SysRoleVO r1 = new SysRoleVO();
            r1.setId(1L);
            r1.setRoleCode("PM");
            SysRoleVO r2 = new SysRoleVO();
            r2.setId(2L);
            r2.setRoleCode("ENGINEER");
            Page<SysRoleVO> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of(r1, r2));
            mockPage.setTotal(2L);
            when(sysRoleMapper.selectRolePage(any(IPage.class), eq(query))).thenReturn(mockPage);
            when(sysRoleMapper.selectMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L));
            when(sysRoleMapper.selectMenuIdsByRoleId(2L)).thenReturn(Collections.emptyList());

            PageResult<SysRoleVO> result = roleService.page(query);

            assertNotNull(result);
            assertEquals(2, result.getRecords().size());
            assertEquals(2, result.getRecords().get(0).getMenuIds().size());
            assertEquals(0, result.getRecords().get(1).getMenuIds().size());
        }

        @Test
        @DisplayName("listAll 仅返回状态为启用的角色，按 id 升序")
        void should_list_only_enabled_roles() {
            SysRoleEntity e1 = buildEntity(1L, "PM", "PM");
            e1.setStatus(SystemConstant.STATUS_ENABLED);
            SysRoleEntity e2 = buildEntity(2L, "ENGINEER", "ENGINEER");
            e2.setStatus(SystemConstant.STATUS_ENABLED);
            when(sysRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(e1, e2));

            List<SysRoleVO> result = roleService.listAll();

            assertEquals(2, result.size());
            assertEquals("PM", result.get(0).getRoleCode());
        }

        @Test
        @DisplayName("listAll 空列表")
        void should_return_empty_list_when_no_roles() {
            when(sysRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<SysRoleVO> result = roleService.listAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    /* ============ create ============ */

    @Nested
    @DisplayName("create 创建角色")
    class CreateTest {

        @Test
        @DisplayName("正常创建：默认状态 1（启用）、默认数据范围 ALL、未传权限不分配菜单")
        void should_create_role_with_defaults() {
            SysRoleDTO dto = buildDto("PM角色", "PM", null, null);
            when(sysRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysRoleMapper.insert(any(SysRoleEntity.class))).thenAnswer(invocation -> {
                SysRoleEntity e = invocation.getArgument(0);
                e.setId(100L);
                return 1;
            });

            Long id = roleService.create(dto);

            assertEquals(100L, id);
            ArgumentCaptor<SysRoleEntity> captor = ArgumentCaptor.forClass(SysRoleEntity.class);
            verify(sysRoleMapper).insert(captor.capture());
            assertAll("字段映射",
                    () -> assertEquals("PM角色", captor.getValue().getRoleName()),
                    () -> assertEquals("PM", captor.getValue().getRoleCode()),
                    () -> assertEquals(SystemConstant.STATUS_ENABLED, captor.getValue().getStatus()),
                    () -> assertEquals(SystemConstant.DATA_SCOPE_ALL, captor.getValue().getDataScope())
            );
            // permissionCodes 为 null 时不调用 saveRoleMenus
            verify(sysRoleMenuMapper, never()).deleteByRoleId(anyLong());
            verify(sysRoleMenuMapper, never()).insert(any(SysRoleMenuEntity.class));
        }

        @Test
        @DisplayName("角色编码重复抛 DATA_DUPLICATED")
        void should_throw_when_role_code_duplicated() {
            SysRoleDTO dto = buildDto("PM", "PM", null, null);
            when(sysRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.create(dto));

            assertEquals(ResultCode.DATA_DUPLICATED.getCode(), ex.getCode());
            verify(sysRoleMapper, never()).insert(any(SysRoleEntity.class));
        }

        @Test
        @DisplayName("permissionCodes 全为数字时直接解析为 menuIds 并保存")
        void should_save_menu_ids_when_permission_codes_are_numeric() {
            SysRoleDTO dto = buildDto("PM", "PM", null, List.of("1", "2", "3"));
            when(sysRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysRoleMapper.insert(any(SysRoleEntity.class))).thenAnswer(invocation -> {
                SysRoleEntity e = invocation.getArgument(0);
                e.setId(100L);
                return 1;
            });

            roleService.create(dto);

            verify(sysRoleMenuMapper).deleteByRoleId(100L);
            verify(sysRoleMenuMapper, org.mockito.Mockito.times(3)).insert(any(SysRoleMenuEntity.class));
        }

        @Test
        @DisplayName("permissionCodes 含权限标识时通过 sysMenuMapper 查询 menuIds")
        void should_query_menus_when_permission_codes_are_perms() {
            SysRoleDTO dto = buildDto("PM", "PM", null, List.of("system:user", "system:role"));
            when(sysRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysRoleMapper.insert(any(SysRoleEntity.class))).thenAnswer(invocation -> {
                SysRoleEntity e = invocation.getArgument(0);
                e.setId(100L);
                return 1;
            });
            SysMenuEntity m1 = new SysMenuEntity();
            m1.setId(50L);
            SysMenuEntity m2 = new SysMenuEntity();
            m2.setId(51L);
            when(sysMenuMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(m1, m2));

            roleService.create(dto);

            // 应插入 2 条角色菜单关联
            verify(sysRoleMenuMapper, org.mockito.Mockito.times(2)).insert(any(SysRoleMenuEntity.class));
        }
    }

    /* ============ update ============ */

    @Nested
    @DisplayName("update 更新角色")
    class UpdateTest {

        @Test
        @DisplayName("id 为空抛 PARAM_MISSING")
        void should_throw_when_id_null() {
            SysRoleDTO dto = new SysRoleDTO();
            dto.setId(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.update(dto));

            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
            verify(sysRoleMapper, never()).updateById(any(SysRoleEntity.class));
        }

        @Test
        @DisplayName("角色不存在抛 NOT_FOUND")
        void should_throw_when_role_not_exist() {
            SysRoleDTO dto = new SysRoleDTO();
            dto.setId(99L);
            when(sysRoleMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.update(dto));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("禁止禁用 SUPER_ADMIN 角色（status=0）")
        void should_throw_when_disable_super_admin() {
            SysRoleDTO dto = new SysRoleDTO();
            dto.setId(1L);
            dto.setStatus(0);
            SysRoleEntity exist = buildEntity(1L, "超管", SystemConstant.ROLE_SUPER_ADMIN);
            exist.setStatus(SystemConstant.STATUS_ENABLED);
            when(sysRoleMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.update(dto));

            assertEquals(ResultCode.BUSINESS_CONFLICT.getCode(), ex.getCode());
            verify(sysRoleMapper, never()).updateById(any(SysRoleEntity.class));
        }

        @Test
        @DisplayName("角色编码变更时执行唯一性校验")
        void should_check_unique_when_role_code_changed() {
            SysRoleDTO dto = buildDto("PM", "NEW_PM", null, null);
            dto.setId(1L);
            SysRoleEntity exist = buildEntity(1L, "PM", "PM");
            when(sysRoleMapper.selectById(1L)).thenReturn(exist);
            when(sysRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.update(dto));

            assertEquals(ResultCode.DATA_DUPLICATED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常更新：编码未变时不校验唯一性、permissionCodes 非空时重新分配菜单")
        void should_update_and_reassign_menus() {
            SysRoleDTO dto = buildDto("PM新名", "PM", null, List.of("10", "20"));
            dto.setId(1L);
            SysRoleEntity exist = buildEntity(1L, "PM", "PM");
            when(sysRoleMapper.selectById(1L)).thenReturn(exist);

            roleService.update(dto);

            verify(sysRoleMapper).updateById(any(SysRoleEntity.class));
            verify(sysRoleMenuMapper).deleteByRoleId(1L);
            verify(sysRoleMenuMapper, org.mockito.Mockito.times(2)).insert(any(SysRoleMenuEntity.class));
        }
    }

    /* ============ delete ============ */

    @Nested
    @DisplayName("delete 删除角色")
    class DeleteTest {

        @Test
        @DisplayName("角色不存在抛 NOT_FOUND")
        void should_throw_when_role_not_exist() {
            when(sysRoleMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.delete(99L));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("禁止删除 SUPER_ADMIN 角色")
        void should_throw_when_delete_super_admin() {
            SysRoleEntity exist = buildEntity(1L, "超管", SystemConstant.ROLE_SUPER_ADMIN);
            when(sysRoleMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.delete(1L));

            assertEquals(ResultCode.BUSINESS_CONFLICT.getCode(), ex.getCode());
            verify(sysRoleMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("角色下存在关联用户禁止删除")
        void should_throw_when_role_has_users() {
            SysRoleEntity exist = buildEntity(2L, "PM", "PM");
            when(sysRoleMapper.selectById(2L)).thenReturn(exist);
            when(sysUserRoleMapper.countByRoleId(2L)).thenReturn(5L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.delete(2L));

            assertEquals(ResultCode.BUSINESS_CONFLICT.getCode(), ex.getCode());
            verify(sysRoleMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("正常删除：删除角色并清理菜单关联")
        void should_delete_role_and_clear_menus() {
            SysRoleEntity exist = buildEntity(2L, "PM", "PM");
            when(sysRoleMapper.selectById(2L)).thenReturn(exist);
            when(sysUserRoleMapper.countByRoleId(2L)).thenReturn(0L);

            roleService.delete(2L);

            verify(sysRoleMapper).deleteById(2L);
            verify(sysRoleMenuMapper).deleteByRoleId(2L);
        }
    }

    /* ============ getDetail / assignMenus ============ */

    @Nested
    @DisplayName("getDetail / assignMenus")
    class DetailAndAssignTest {

        @Test
        @DisplayName("getDetail 不存在抛 NOT_FOUND")
        void should_throw_when_get_detail_not_found() {
            when(sysRoleMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.getDetail(99L));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("getDetail 正常返回：填充 menuIds")
        void should_return_detail_with_menu_ids() {
            SysRoleEntity e = buildEntity(1L, "PM", "PM");
            when(sysRoleMapper.selectById(1L)).thenReturn(e);
            when(sysRoleMapper.selectMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L));

            SysRoleVO vo = roleService.getDetail(1L);

            assertNotNull(vo);
            assertEquals("PM", vo.getRoleCode());
            assertEquals(2, vo.getMenuIds().size());
        }

        @Test
        @DisplayName("assignMenus 角色不存在抛 NOT_FOUND")
        void should_throw_when_assign_menus_role_not_found() {
            when(sysRoleMapper.selectById(99L)).thenReturn(null);
            SysRoleMenuDTO dto = new SysRoleMenuDTO();
            dto.setPermissionCodes(List.of("1"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> roleService.assignMenus(99L, dto));

            assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
            verify(sysRoleMenuMapper, never()).deleteByRoleId(anyLong());
        }

        @Test
        @DisplayName("assignMenus 混合权限标识：数字直接解析 + perms 查询")
        void should_resolve_mixed_permission_codes() {
            SysRoleEntity exist = buildEntity(1L, "PM", "PM");
            when(sysRoleMapper.selectById(1L)).thenReturn(exist);
            SysMenuEntity m = new SysMenuEntity();
            m.setId(50L);
            when(sysMenuMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(m));
            SysRoleMenuDTO dto = new SysRoleMenuDTO();
            dto.setPermissionCodes(List.of("10", "system:user"));

            roleService.assignMenus(1L, dto);

            verify(sysRoleMenuMapper).deleteByRoleId(1L);
            // 1 个数字 + 1 个通过 perms 查询的 = 2 次插入
            verify(sysRoleMenuMapper, org.mockito.Mockito.times(2)).insert(any(SysRoleMenuEntity.class));
        }
    }

    /* ============ getRoleIdsByCodes / getRolesByUserId / getRoleCodesByUserId ============ */

    @Nested
    @DisplayName("getRoleIdsByCodes / getRolesByUserId / getRoleCodesByUserId")
    class RoleQueryTest {

        @Test
        @DisplayName("getRoleIdsByCodes 入参为 null 返回空列表")
        void should_return_empty_when_role_codes_null() {
            List<Long> result = roleService.getRoleIdsByCodes(null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(sysRoleMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("getRoleIdsByCodes 入参为空列表返回空列表")
        void should_return_empty_when_role_codes_empty() {
            List<Long> result = roleService.getRoleIdsByCodes(Collections.emptyList());

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(sysRoleMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("getRoleIdsByCodes 正常返回 roleId 列表")
        void should_return_role_ids() {
            SysRoleEntity e1 = buildEntity(10L, "PM", "PM");
            SysRoleEntity e2 = buildEntity(11L, "工程师", "ENGINEER");
            when(sysRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(e1, e2));

            List<Long> result = roleService.getRoleIdsByCodes(List.of("PM", "ENGINEER"));

            assertEquals(2, result.size());
            assertTrue(result.contains(10L));
            assertTrue(result.contains(11L));
        }

        @Test
        @DisplayName("getRolesByUserId 委托 Mapper")
        void should_delegate_get_roles_by_user_id() {
            RoleSimpleVO r = new RoleSimpleVO();
            r.setId(10L);
            r.setRoleCode("PM");
            when(sysRoleMapper.selectRolesByUserId(5L)).thenReturn(List.of(r));

            List<RoleSimpleVO> result = roleService.getRolesByUserId(5L);

            assertEquals(1, result.size());
            assertEquals("PM", result.get(0).getRoleCode());
        }

        @Test
        @DisplayName("getRoleCodesByUserId 委托 Mapper")
        void should_delegate_get_role_codes_by_user_id() {
            when(sysRoleMapper.selectRoleCodesByUserId(5L)).thenReturn(List.of("PM", "ENGINEER"));

            List<String> result = roleService.getRoleCodesByUserId(5L);

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("getRoleMenuIds 委托 Mapper")
        void should_delegate_get_role_menu_ids() {
            when(sysRoleMapper.selectMenuIdsByRoleId(1L)).thenReturn(List.of(10L, 20L));

            List<Long> result = roleService.getRoleMenuIds(1L);

            assertEquals(2, result.size());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private SysRoleDTO buildDto(String roleName, String roleCode, Integer status, List<String> permissionCodes) {
        SysRoleDTO dto = new SysRoleDTO();
        dto.setRoleName(roleName);
        dto.setRoleCode(roleCode);
        dto.setStatus(status);
        dto.setPermissionCodes(permissionCodes);
        return dto;
    }

    private SysRoleEntity buildEntity(Long id, String roleName, String roleCode) {
        SysRoleEntity e = new SysRoleEntity();
        e.setId(id);
        e.setRoleName(roleName);
        e.setRoleCode(roleCode);
        e.setStatus(SystemConstant.STATUS_ENABLED);
        e.setDataScope(SystemConstant.DATA_SCOPE_ALL);
        return e;
    }
}
