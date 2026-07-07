package com.vibe.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.vibe.system.vo.RoleSimpleVO;
import com.vibe.system.vo.SysUserVO;
import com.vibe.system.vo.UserInfoVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 系统用户服务实现单元测试（Task 3 SubTask 3.7）
 *
 * <p>覆盖范围：</p>
 * <ul>
 *   <li>page：分页查询、每条记录填充角色列表</li>
 *   <li>create：BCrypt 密码加密、唯一性校验、默认密码、角色关联保存</li>
 *   <li>update：id 为空/用户不存在、用户名变更唯一性校验、角色重新分配</li>
 *   <li>delete：超级管理员保护、不存在用户、删除并清理角色关联</li>
 *   <li>getDetail：不存在抛 USER_NOT_FOUND</li>
 *   <li>assignRoles：用户不存在校验、角色覆盖</li>
 *   <li>changeStatus：状态映射（1→ACTIVE / 0→DISABLED）</li>
 *   <li>resetPassword：默认密码兜底、BCrypt 加密</li>
 *   <li>changePassword：旧密码校验</li>
 *   <li>getCurrentUserInfo：未登录拦截、角色与权限填充</li>
 * </ul>
 *
 * <p>说明：PasswordEncoder 使用真实 BCryptPasswordEncoder 实例（避免 Mock 后无法验证加密/匹配逻辑）。</p>
 *
 * @author vibe
 */
@DisplayName("用户服务 SysUserServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysMenuService sysMenuService;

    /** 使用真实 BCrypt 实现（@Spy 让 @InjectMocks 通过构造器注入），验证加密/匹配逻辑 */
    @Spy
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private SysUserServiceImpl userService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /* ============ 分页查询 ============ */

    @Nested
    @DisplayName("page 分页查询")
    class PageTest {

        @Test
        @DisplayName("正常分页：每条记录填充角色列表")
        void should_fill_roles_for_each_user() {
            SysUserQueryDTO query = new SysUserQueryDTO();
            query.setPage(1);
            query.setSize(10);

            SysUserVO u1 = buildVo(1L, "alice");
            SysUserVO u2 = buildVo(2L, "bob");
            Page<SysUserVO> mockPage = new Page<>(1, 10);
            mockPage.setRecords(List.of(u1, u2));
            mockPage.setTotal(2L);
            when(sysUserMapper.selectUserPage(any(IPage.class), eq(query))).thenReturn(mockPage);
            when(sysUserMapper.selectRolesByUserId(1L))
                    .thenReturn(List.of(buildRole(10L, "PM")));
            when(sysUserMapper.selectRolesByUserId(2L))
                    .thenReturn(List.of(buildRole(11L, "ENGINEER")));

            PageResult<SysUserVO> result = userService.page(query);

            assertNotNull(result);
            assertEquals(2, result.getRecords().size());
            assertEquals(1, result.getRecords().get(0).getRoles().size());
            assertEquals("PM", result.getRecords().get(0).getRoles().get(0).getRoleCode());
            assertEquals("ENGINEER", result.getRecords().get(1).getRoles().get(0).getRoleCode());
        }

        @Test
        @DisplayName("page/size 为 null 时默认 1/20")
        void should_use_default_page_size_when_null() {
            SysUserQueryDTO query = new SysUserQueryDTO();
            Page<SysUserVO> mockPage = new Page<>(1, 20);
            mockPage.setRecords(Collections.emptyList());
            mockPage.setTotal(0L);
            when(sysUserMapper.selectUserPage(any(IPage.class), eq(query))).thenReturn(mockPage);

            PageResult<SysUserVO> result = userService.page(query);

            assertNotNull(result);
            assertEquals(0, result.getRecords().size());
            assertEquals(1L, result.getPage());
            assertEquals(20L, result.getSize());
        }
    }

    /* ============ create 创建用户 ============ */

    @Nested
    @DisplayName("create 创建用户")
    class CreateTest {

        @Test
        @DisplayName("正常创建：密码 BCrypt 加密、状态 ACTIVE、写入角色关联")
        void should_create_user_with_bcrypt_password() {
            SysUserDTO dto = buildDto("alice", "rawPassword", 1);
            dto.setRoleCodes(List.of("PM"));
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysRoleService.getRoleIdsByCodes(List.of("PM"))).thenReturn(List.of(10L));
            when(sysUserMapper.insert(any(SysUserEntity.class))).thenAnswer(invocation -> {
                SysUserEntity e = invocation.getArgument(0);
                e.setId(100L);
                return 1;
            });

            Long id = userService.create(dto);

            assertEquals(100L, id);
            ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
            verify(sysUserMapper).insert(captor.capture());
            SysUserEntity saved = captor.getValue();
            assertAll("字段映射",
                    () -> assertEquals("alice", saved.getUsername()),
                    () -> assertNotEquals("rawPassword", saved.getPassword(), "密码应被加密"),
                    () -> assertTrue(saved.getPassword().startsWith("$2a$"), "BCrypt 密文应以 $2a$ 开头"),
                    () -> assertEquals(SystemConstant.USER_STATUS_ACTIVE, saved.getStatus()),
                    () -> assertEquals(SystemConstant.TENANT_INTERNAL, saved.getTenantType())
            );
            // 验证角色关联：先删旧关联，再插入新关联
            verify(sysUserRoleMapper).deleteByUserId(100L);
            ArgumentCaptor<SysUserRoleEntity> roleCaptor = ArgumentCaptor.forClass(SysUserRoleEntity.class);
            verify(sysUserRoleMapper).insert(roleCaptor.capture());
            assertEquals(10L, roleCaptor.getValue().getRoleId());
        }

        @Test
        @DisplayName("密码为空时使用默认密码 vibe@123")
        void should_use_default_password_when_blank() {
            SysUserDTO dto = buildDto("bob", null, 1);
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysUserMapper.insert(any(SysUserEntity.class))).thenAnswer(invocation -> {
                SysUserEntity e = invocation.getArgument(0);
                e.setId(1L);
                return 1;
            });

            userService.create(dto);

            ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
            verify(sysUserMapper).insert(captor.capture());
            // 用 BCrypt 验证默认密码能匹配
            assertTrue(passwordEncoder.matches(SystemConstant.DEFAULT_PASSWORD,
                    captor.getValue().getPassword()));
        }

        @Test
        @DisplayName("用户名重复抛 DATA_DUPLICATED")
        void should_throw_when_username_duplicated() {
            SysUserDTO dto = buildDto("alice", "pwd", 1);
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.create(dto));

            assertEquals(ResultCode.DATA_DUPLICATED.getCode(), ex.getCode());
            verify(sysUserMapper, never()).insert(any(SysUserEntity.class));
        }

        @Test
        @DisplayName("手机号不为空时进行唯一性校验")
        void should_check_phone_unique_when_provided() {
            SysUserDTO dto = buildDto("alice", "pwd", 1);
            dto.setPhone("13800138000");
            // 第一次 selectCount（用户名）返回 0，第二次（手机号）返回 1
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L)
                    .thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.create(dto));

            assertEquals(ResultCode.DATA_DUPLICATED.getCode(), ex.getCode());
            verify(sysUserMapper, never()).insert(any(SysUserEntity.class));
        }

        @Test
        @DisplayName("状态为 0 时映射为 DISABLED")
        void should_map_status_zero_to_disabled() {
            SysUserDTO dto = buildDto("alice", "pwd", 0);
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(sysUserMapper.insert(any(SysUserEntity.class))).thenAnswer(invocation -> {
                SysUserEntity e = invocation.getArgument(0);
                e.setId(1L);
                return 1;
            });

            userService.create(dto);

            ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
            verify(sysUserMapper).insert(captor.capture());
            assertEquals(SystemConstant.USER_STATUS_DISABLED, captor.getValue().getStatus());
        }
    }

    /* ============ update 更新用户 ============ */

    @Nested
    @DisplayName("update 更新用户")
    class UpdateTest {

        @Test
        @DisplayName("id 为空抛 PARAM_MISSING")
        void should_throw_when_id_null() {
            SysUserDTO dto = new SysUserDTO();
            dto.setId(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.update(dto));

            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
            verify(sysUserMapper, never()).updateById(any(SysUserEntity.class));
        }

        @Test
        @DisplayName("用户不存在抛 USER_NOT_FOUND")
        void should_throw_when_user_not_exist() {
            SysUserDTO dto = new SysUserDTO();
            dto.setId(99L);
            when(sysUserMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.update(dto));

            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("用户名变更时执行唯一性校验")
        void should_check_username_unique_when_changed() {
            SysUserDTO dto = buildDto("newname", null, null);
            dto.setId(1L);
            SysUserEntity exist = buildEntity(1L, "oldname");
            when(sysUserMapper.selectById(1L)).thenReturn(exist);
            when(sysUserMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.update(dto));

            assertEquals(ResultCode.DATA_DUPLICATED.getCode(), ex.getCode());
            verify(sysUserMapper, never()).updateById(any(SysUserEntity.class));
        }

        @Test
        @DisplayName("正常更新：用户名与原值相同不校验、roleCodes 不为空时重新分配角色")
        void should_update_and_reassign_roles() {
            SysUserDTO dto = buildDto("alice", null, null);
            dto.setId(1L);
            dto.setRoleCodes(List.of("PM"));
            SysUserEntity exist = buildEntity(1L, "alice");
            when(sysUserMapper.selectById(1L)).thenReturn(exist);
            when(sysRoleService.getRoleIdsByCodes(List.of("PM"))).thenReturn(List.of(10L));

            userService.update(dto);

            verify(sysUserMapper).updateById(any(SysUserEntity.class));
            verify(sysUserRoleMapper).deleteByUserId(1L);
            verify(sysUserRoleMapper).insert(any(SysUserRoleEntity.class));
        }
    }

    /* ============ delete 删除用户 ============ */

    @Nested
    @DisplayName("delete 删除用户")
    class DeleteTest {

        @Test
        @DisplayName("id 为空抛 PARAM_MISSING")
        void should_throw_when_id_null() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.delete(null));

            assertEquals(ResultCode.PARAM_MISSING.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("超级管理员账号（id=1）禁止删除")
        void should_throw_when_delete_super_admin() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.delete(1L));

            assertEquals(ResultCode.BUSINESS_CONFLICT.getCode(), ex.getCode());
            verify(sysUserMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("用户不存在抛 USER_NOT_FOUND")
        void should_throw_when_user_not_exist() {
            when(sysUserMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.delete(99L));

            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("正常删除：删除用户并清理角色关联")
        void should_delete_user_and_clear_roles() {
            SysUserEntity exist = buildEntity(5L, "alice");
            when(sysUserMapper.selectById(5L)).thenReturn(exist);

            userService.delete(5L);

            verify(sysUserMapper).deleteById(5L);
            verify(sysUserRoleMapper).deleteByUserId(5L);
        }
    }

    /* ============ getDetail / assignRoles / getUserRoles ============ */

    @Nested
    @DisplayName("getDetail / assignRoles / getUserRoles")
    class DetailAndRolesTest {

        @Test
        @DisplayName("getDetail 不存在抛 USER_NOT_FOUND")
        void should_throw_when_get_detail_not_found() {
            when(sysUserMapper.selectVoById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.getDetail(99L));

            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("getDetail 正常返回")
        void should_return_detail() {
            SysUserVO vo = buildVo(1L, "alice");
            when(sysUserMapper.selectVoById(1L)).thenReturn(vo);

            SysUserVO result = userService.getDetail(1L);

            assertNotNull(result);
            assertEquals("alice", result.getUsername());
        }

        @Test
        @DisplayName("assignRoles 用户不存在抛 USER_NOT_FOUND")
        void should_throw_when_assign_roles_user_not_found() {
            when(sysUserMapper.selectById(99L)).thenReturn(null);
            SysUserRoleDTO dto = new SysUserRoleDTO();
            dto.setRoleCodes(List.of("PM"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.assignRoles(99L, dto));

            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
            verify(sysRoleService, never()).getRoleIdsByCodes(any());
        }

        @Test
        @DisplayName("getUserRoles 委托 Mapper")
        void should_delegate_get_user_roles() {
            when(sysUserMapper.selectRolesByUserId(1L))
                    .thenReturn(List.of(buildRole(10L, "PM")));

            List<RoleSimpleVO> roles = userService.getUserRoles(1L);

            assertEquals(1, roles.size());
            assertEquals("PM", roles.get(0).getRoleCode());
        }
    }

    /* ============ changeStatus / resetPassword / changePassword ============ */

    @Nested
    @DisplayName("changeStatus / resetPassword / changePassword")
    class PasswordAndStatusTest {

        @Test
        @DisplayName("changeStatus 用户不存在抛 USER_NOT_FOUND")
        void should_throw_when_change_status_user_not_found() {
            when(sysUserMapper.selectById(99L)).thenReturn(null);
            SysUserStatusDTO dto = new SysUserStatusDTO();
            dto.setStatus(0);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changeStatus(99L, dto));

            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("changeStatus 状态 1 → ACTIVE")
        void should_change_status_to_active() {
            SysUserEntity exist = buildEntity(1L, "alice");
            when(sysUserMapper.selectById(1L)).thenReturn(exist);
            SysUserStatusDTO dto = new SysUserStatusDTO();
            dto.setStatus(1);

            userService.changeStatus(1L, dto);

            ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
            verify(sysUserMapper).updateById(captor.capture());
            assertEquals(SystemConstant.USER_STATUS_ACTIVE, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("resetPassword 用户不存在抛 USER_NOT_FOUND")
        void should_throw_when_reset_password_user_not_found() {
            when(sysUserMapper.selectById(99L)).thenReturn(null);
            SysUserPasswordDTO dto = new SysUserPasswordDTO();
            dto.setNewPassword("newpwd");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.resetPassword(99L, dto));

            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("resetPassword 新密码为空时使用默认密码")
        void should_use_default_when_new_password_blank() {
            SysUserEntity exist = buildEntity(1L, "alice");
            when(sysUserMapper.selectById(1L)).thenReturn(exist);
            SysUserPasswordDTO dto = new SysUserPasswordDTO();
            dto.setNewPassword(" ");

            userService.resetPassword(1L, dto);

            ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
            verify(sysUserMapper).updateById(captor.capture());
            assertTrue(passwordEncoder.matches(SystemConstant.DEFAULT_PASSWORD,
                    captor.getValue().getPassword()));
        }

        @Test
        @DisplayName("changePassword userId 为空抛 UNAUTHORIZED")
        void should_throw_when_change_password_user_id_null() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changePassword(null, "old", "new"));

            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("changePassword 旧密码不匹配抛 PASSWORD_ERROR")
        void should_throw_when_old_password_mismatch() {
            SysUserEntity exist = buildEntity(1L, "alice");
            exist.setPassword(passwordEncoder.encode("correctOld"));
            when(sysUserMapper.selectById(1L)).thenReturn(exist);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.changePassword(1L, "wrongOld", "newPwd"));

            assertEquals(ResultCode.PASSWORD_ERROR.getCode(), ex.getCode());
            verify(sysUserMapper, never()).updateById(any(SysUserEntity.class));
        }

        @Test
        @DisplayName("changePassword 旧密码匹配后更新为新密码")
        void should_change_password_when_old_matches() {
            SysUserEntity exist = buildEntity(1L, "alice");
            exist.setPassword(passwordEncoder.encode("correctOld"));
            when(sysUserMapper.selectById(1L)).thenReturn(exist);

            userService.changePassword(1L, "correctOld", "newPwd123");

            ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
            verify(sysUserMapper).updateById(captor.capture());
            assertTrue(passwordEncoder.matches("newPwd123", captor.getValue().getPassword()));
        }
    }

    /* ============ getCurrentUserInfo / findByUsername ============ */

    @Nested
    @DisplayName("getCurrentUserInfo / findByUsername / findByPhone")
    class CurrentUserTest {

        @Test
        @DisplayName("getCurrentUserInfo 未登录抛 UNAUTHORIZED")
        void should_throw_when_not_logged_in() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.getCurrentUserInfo());

            assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("getCurrentUserInfo 用户不存在抛 USER_NOT_FOUND")
        void should_throw_when_current_user_not_found() {
            UserContextHolder.set(UserContext.builder().userId(99L).build());
            when(sysUserMapper.selectVoById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.getCurrentUserInfo());

            assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("getCurrentUserInfo 正常返回：填充角色与权限")
        void should_return_current_user_info_with_roles_and_permissions() {
            UserContextHolder.set(UserContext.builder().userId(1L).build());
            SysUserVO vo = buildVo(1L, "alice");
            vo.setRealName("爱丽丝");
            when(sysUserMapper.selectVoById(1L)).thenReturn(vo);
            when(sysRoleService.getRoleCodesByUserId(1L)).thenReturn(List.of("PM"));
            when(sysMenuService.getPermissionsByUserId(1L)).thenReturn(List.of("project:view"));

            UserInfoVO info = userService.getCurrentUserInfo();

            assertNotNull(info);
            assertAll("用户信息字段",
                    () -> assertEquals(1L, info.getUserId()),
                    () -> assertEquals("alice", info.getUserName()),
                    () -> assertEquals("爱丽丝", info.getRealName()),
                    () -> assertEquals(List.of("PM"), info.getRoles()),
                    () -> assertEquals(List.of("project:view"), info.getPermissions())
            );
        }

        @Test
        @DisplayName("findByUsername 委托 Mapper")
        void should_delegate_find_by_username() {
            SysUserVO vo = buildVo(1L, "alice");
            when(sysUserMapper.selectByUsernameWithRoles("alice")).thenReturn(vo);

            SysUserVO result = userService.findByUsername("alice");

            assertEquals("alice", result.getUsername());
        }

        @Test
        @DisplayName("findByPhone 委托 Mapper")
        void should_delegate_find_by_phone() {
            SysUserVO vo = buildVo(1L, "alice");
            when(sysUserMapper.selectByPhoneWithRoles("13800138000")).thenReturn(vo);

            SysUserVO result = userService.findByPhone("13800138000");

            assertEquals("alice", result.getUsername());
        }
    }

    /* ============ updateLastLoginTime ============ */

    @Nested
    @DisplayName("updateLastLoginTime")
    class UpdateLastLoginTimeTest {

        @Test
        @DisplayName("userId 为 null 时直接返回不调用 Mapper")
        void should_skip_when_user_id_null() {
            userService.updateLastLoginTime(null);

            verify(sysUserMapper, never()).updateById(any(SysUserEntity.class));
        }

        @Test
        @DisplayName("正常更新 lastLoginTime")
        void should_update_last_login_time() {
            userService.updateLastLoginTime(1L);

            ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
            verify(sysUserMapper).updateById(captor.capture());
            assertEquals(1L, captor.getValue().getId());
            assertNotNull(captor.getValue().getLastLoginTime());
        }
    }

    /* ============ 测试辅助方法 ============ */

    private SysUserDTO buildDto(String username, String password, Integer status) {
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setStatus(status);
        dto.setRealName(username + "_real");
        return dto;
    }

    private SysUserEntity buildEntity(Long id, String username) {
        SysUserEntity e = new SysUserEntity();
        e.setId(id);
        e.setUsername(username);
        e.setStatus(SystemConstant.USER_STATUS_ACTIVE);
        return e;
    }

    private SysUserVO buildVo(Long id, String username) {
        SysUserVO vo = new SysUserVO();
        vo.setId(id);
        vo.setUsername(username);
        vo.setStatus(SystemConstant.USER_STATUS_ACTIVE);
        return vo;
    }

    private RoleSimpleVO buildRole(Long id, String code) {
        RoleSimpleVO r = new RoleSimpleVO();
        r.setId(id);
        r.setRoleCode(code);
        return r;
    }
}
