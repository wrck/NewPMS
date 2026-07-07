package com.vibe.project.service.impl;

import com.vibe.common.exception.BusinessException;
import com.vibe.project.constant.ProjectConstant;
import com.vibe.project.dto.ProjectCreateDTO;
import com.vibe.project.dto.ProjectStatusDTO;
import com.vibe.project.entity.ProjectEntity;
import com.vibe.project.enums.ProjectStatusEnum;
import com.vibe.project.mapper.ProjectIssueMapper;
import com.vibe.project.mapper.ProjectMapper;
import com.vibe.project.mapper.ProjectMemberMapper;
import com.vibe.project.mapper.ProjectMilestoneMapper;
import com.vibe.project.mapper.ProjectPhaseMapper;
import com.vibe.project.mapper.ProjectRiskMapper;
import com.vibe.project.mapper.ProjectTaskMapper;
import com.vibe.project.mapper.ProjectTemplateMapper;
import com.vibe.project.mapper.ProjectTemplatePhaseMapper;
import com.vibe.project.mapper.ProjectTemplateTaskMapper;
import com.vibe.event.DomainEventPublisher;
import com.vibe.utils.RedisUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目服务实现单元测试
 *
 * <p>覆盖项目编号生成（PRJ-YYYYMM-XXX）、状态流转校验、结项检查等核心业务逻辑。</p>
 *
 * @author vibe
 */
@DisplayName("项目服务 ProjectServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectPhaseMapper projectPhaseMapper;
    @Mock
    private ProjectTaskMapper projectTaskMapper;
    @Mock
    private ProjectMilestoneMapper projectMilestoneMapper;
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private ProjectRiskMapper projectRiskMapper;
    @Mock
    private ProjectIssueMapper projectIssueMapper;
    @Mock
    private ProjectTemplateMapper projectTemplateMapper;
    @Mock
    private ProjectTemplatePhaseMapper projectTemplatePhaseMapper;
    @Mock
    private ProjectTemplateTaskMapper projectTemplateTaskMapper;
    @Mock
    private RedisUtils redisUtils;
    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private ProjectServiceImpl projectService;

    /** 项目编号格式 PRJ-YYYYMM-XXX */
    private static final Pattern PROJECT_CODE_PATTERN = Pattern.compile("^PRJ-\\d{6}-\\d{3}$");

    @Nested
    @DisplayName("项目编号生成 PRJ-YYYYMM-XXX")
    class GenerateProjectCodeTest {

        @Test
        @DisplayName("首次生成序号 1 → 编号 PRJ-YYYYMM-001 并设置过期")
        void should_generate_code_with_seq_1_and_set_expire() {
            when(redisUtils.increment(anyString())).thenReturn(1L);
            when(projectMapper.insert(any(ProjectEntity.class))).thenReturn(1);

            ProjectCreateDTO dto = new ProjectCreateDTO();
            dto.setProjectName("测试项目");
            dto.setPmId(1001L);

            projectService.create(dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).insert(captor.capture());
            ProjectEntity saved = captor.getValue();

            String code = saved.getProjectCode();
            assertNotNull(code, "项目编号不能为空");
            assertTrue(PROJECT_CODE_PATTERN.matcher(code).matches(),
                    "项目编号格式应为 PRJ-YYYYMM-XXX，实际: " + code);

            // 验证月份为当前月份
            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            assertTrue(code.contains("-" + currentMonth + "-"),
                    "项目编号应包含当前月份 " + currentMonth + "，实际: " + code);

            // 首次序号应为 001
            assertTrue(code.endsWith("-001"), "首次序号应为 001，实际: " + code);

            // 首次序号应设置过期
            verify(redisUtils, times(1)).expire(anyString(), any());
        }

        @Test
        @DisplayName("序号 42 → 编号 PRJ-YYYYMM-042（3 位补零）")
        void should_generate_code_with_seq_42_zero_padded() {
            when(redisUtils.increment(anyString())).thenReturn(42L);
            when(projectMapper.insert(any(ProjectEntity.class))).thenReturn(1);

            ProjectCreateDTO dto = new ProjectCreateDTO();
            dto.setProjectName("测试项目");

            projectService.create(dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).insert(captor.capture());
            String code = captor.getValue().getProjectCode();

            assertTrue(code.endsWith("-042"), "序号 42 应补零为 042，实际: " + code);
            // 非首次不设置过期
            verify(redisUtils, never()).expire(anyString(), any());
        }

        @Test
        @DisplayName("序号 1234 → 编号 PRJ-YYYYMM-1234（超过 3 位不截断）")
        void should_generate_code_with_seq_1234() {
            when(redisUtils.increment(anyString())).thenReturn(1234L);
            when(projectMapper.insert(any(ProjectEntity.class))).thenReturn(1);

            ProjectCreateDTO dto = new ProjectCreateDTO();
            dto.setProjectName("测试项目");

            projectService.create(dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).insert(captor.capture());
            String code = captor.getValue().getProjectCode();

            assertTrue(code.endsWith("-1234"), "序号 1234 应为 1234（不截断），实际: " + code);
        }

        @Test
        @DisplayName("新建项目初始状态为 INIT")
        void should_set_init_status_for_new_project() {
            when(redisUtils.increment(anyString())).thenReturn(1L);
            when(projectMapper.insert(any(ProjectEntity.class))).thenReturn(1);

            ProjectCreateDTO dto = new ProjectCreateDTO();
            dto.setProjectName("测试项目");

            projectService.create(dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).insert(captor.capture());
            ProjectEntity saved = captor.getValue();

            assertEquals(ProjectConstant.STATUS_INIT, saved.getStatus(), "新项目状态应为 INIT");
            assertEquals(0, saved.getProgressPct(), "新项目进度应为 0");
        }

        @Test
        @DisplayName("未指定执行模式默认 SELF，未指定优先级默认 P2")
        void should_use_default_execute_mode_and_priority() {
            when(redisUtils.increment(anyString())).thenReturn(1L);
            when(projectMapper.insert(any(ProjectEntity.class))).thenReturn(1);

            ProjectCreateDTO dto = new ProjectCreateDTO();
            dto.setProjectName("测试项目");

            projectService.create(dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).insert(captor.capture());
            ProjectEntity saved = captor.getValue();

            assertAll("默认值",
                    () -> assertEquals(ProjectConstant.EXECUTE_MODE_SELF, saved.getExecuteMode(),
                            "默认执行模式应为 SELF"),
                    () -> assertEquals(ProjectConstant.PRIORITY_P2, saved.getPriority(),
                            "默认优先级应为 P2")
            );
        }
    }

    @Nested
    @DisplayName("项目状态流转")
    class TransitionTest {

        @Test
        @DisplayName("INIT → PLAN 合法流转成功")
        void should_transition_init_to_plan() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_INIT);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectMapper.updateById(any(ProjectEntity.class))).thenReturn(1);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_PLAN);

            projectService.transition(dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).updateById(captor.capture());
            assertEquals(ProjectConstant.STATUS_PLAN, captor.getValue().getStatus(),
                    "状态应更新为 PLAN");
        }

        @Test
        @DisplayName("PLAN → EXECUTE 设置实际开始日期")
        void should_set_actual_start_when_plan_to_execute() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_PLAN);
            exist.setActualStart(null);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectMapper.updateById(any(ProjectEntity.class))).thenReturn(1);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_EXECUTE);

            projectService.transition(dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).updateById(captor.capture());
            assertEquals(LocalDate.now(), captor.getValue().getActualStart(),
                    "进入执行阶段应设置实际开始日期为今天");
        }

        @Test
        @DisplayName("ACCEPT → CLOSE 设置实际结束日期")
        void should_set_actual_end_when_accept_to_close() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_ACCEPT);
            exist.setActualEnd(null);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectTaskMapper.countUnfinishedByProject(1L)).thenReturn(0);
            when(projectMapper.updateById(any(ProjectEntity.class))).thenReturn(1);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_CLOSE);

            projectService.transition(dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).updateById(captor.capture());
            assertEquals(LocalDate.now(), captor.getValue().getActualEnd(),
                    "结项应设置实际结束日期为今天");
        }

        @Test
        @DisplayName("INIT → EXECUTE 跨阶段非法流转抛 BusinessException")
        void should_throw_when_init_to_execute_invalid() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_INIT);
            when(projectMapper.selectById(1L)).thenReturn(exist);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_EXECUTE);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.transition(dto),
                    "INIT → EXECUTE 应抛 BusinessException");
            assertEquals(40902, ex.getCode(), "应为状态流转非法错误码");
            verify(projectMapper, never()).updateById(any(ProjectEntity.class));
        }

        @Test
        @DisplayName("ARCHIVED 终态不可流转")
        void should_throw_when_archived_to_any() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_ARCHIVED);
            when(projectMapper.selectById(1L)).thenReturn(exist);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_INIT);

            assertThrows(BusinessException.class,
                    () -> projectService.transition(dto),
                    "ARCHIVED 终态应抛 BusinessException");
            verify(projectMapper, never()).updateById(any(ProjectEntity.class));
        }

        @Test
        @DisplayName("项目不存在抛 PROJECT_NOT_FOUND")
        void should_throw_when_project_not_found() {
            when(projectMapper.selectById(1L)).thenReturn(null);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_PLAN);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.transition(dto),
                    "项目不存在应抛 BusinessException");
            assertEquals(40401, ex.getCode(), "应为项目不存在错误码");
        }

        @Test
        @DisplayName("无效目标状态抛 PARAM_INVALID")
        void should_throw_when_invalid_target_status() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_INIT);
            when(projectMapper.selectById(1L)).thenReturn(exist);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus("NOT_EXIST");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.transition(dto),
                    "无效状态应抛 BusinessException");
            assertEquals(40000, ex.getCode(), "应为参数校验失败错误码");
        }

        @Test
        @DisplayName("乐观锁版本不匹配抛异常")
        void should_throw_when_version_mismatch() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_INIT);
            exist.setVersion(2);
            when(projectMapper.selectById(1L)).thenReturn(exist);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_PLAN);
            dto.setVersion(1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.transition(dto),
                    "版本不匹配应抛 BusinessException");
            assertEquals(40902, ex.getCode(), "应为状态流转非法错误码");
            verify(projectMapper, never()).updateById(any(ProjectEntity.class));
        }

        @Test
        @DisplayName("并发更新失败（updateById 返回 0）抛异常")
        void should_throw_when_update_returns_zero() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_INIT);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectMapper.updateById(any(ProjectEntity.class))).thenReturn(0);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_PLAN);

            assertThrows(BusinessException.class,
                    () -> projectService.transition(dto),
                    "并发更新失败应抛 BusinessException");
        }
    }

    @Nested
    @DisplayName("EXECUTE → ACCEPT 前置校验")
    class ExecuteToAcceptPreconditionTest {

        @Test
        @DisplayName("有未完成任务时拒绝进入验收阶段")
        void should_reject_accept_when_unfinished_tasks_exist() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_EXECUTE);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectTaskMapper.countUnfinishedByProject(1L)).thenReturn(3);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_ACCEPT);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.transition(dto),
                    "有未完成任务应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为状态不允许错误码");
            assertTrue(ex.getMessage().contains("3"), "错误消息应包含未完成任务数");
            verify(projectMapper, never()).updateById(any(ProjectEntity.class));
        }

        @Test
        @DisplayName("无未完成任务时允许进入验收阶段")
        void should_allow_accept_when_no_unfinished_tasks() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_EXECUTE);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectTaskMapper.countUnfinishedByProject(1L)).thenReturn(0);
            when(projectMapper.updateById(any(ProjectEntity.class))).thenReturn(1);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_ACCEPT);

            projectService.transition(dto);
            verify(projectMapper).updateById(any(ProjectEntity.class));
        }
    }

    @Nested
    @DisplayName("ACCEPT → CLOSE 结项检查")
    class AcceptToClosePreconditionTest {

        @Test
        @DisplayName("有未完成任务时拒绝结项")
        void should_reject_close_when_unfinished_tasks_exist() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_ACCEPT);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectTaskMapper.countUnfinishedByProject(1L)).thenReturn(5);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_CLOSE);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.transition(dto),
                    "有未完成任务应抛 BusinessException");
            assertEquals(40901, ex.getCode(), "应为状态不允许错误码");
            assertTrue(ex.getMessage().contains("5"), "错误消息应包含未完成任务数");
        }

        @Test
        @DisplayName("无未完成任务时允许结项")
        void should_allow_close_when_no_unfinished_tasks() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_ACCEPT);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectTaskMapper.countUnfinishedByProject(1L)).thenReturn(0);
            when(projectMapper.updateById(any(ProjectEntity.class))).thenReturn(1);

            ProjectStatusDTO dto = new ProjectStatusDTO();
            dto.setId(1L);
            dto.setTargetStatus(ProjectConstant.STATUS_CLOSE);

            projectService.transition(dto);
            verify(projectMapper).updateById(any(ProjectEntity.class));
        }
    }

    @Nested
    @DisplayName("checkClose 结项检查方法")
    class CheckCloseTest {

        @Test
        @DisplayName("有未完成任务返回提示信息")
        void should_return_message_when_unfinished_tasks_exist() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_ACCEPT);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectTaskMapper.countUnfinishedByProject(1L)).thenReturn(2);

            String result = projectService.checkClose(1L);

            assertNotNull(result, "有未完成任务应返回非 null 提示");
            assertTrue(result.contains("2"), "提示应包含未完成任务数");
        }

        @Test
        @DisplayName("无未完成任务返回 null（允许结项）")
        void should_return_null_when_no_unfinished_tasks() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_ACCEPT);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectTaskMapper.countUnfinishedByProject(1L)).thenReturn(0);

            String result = projectService.checkClose(1L);

            assertNull(result, "无未完成任务应返回 null");
        }

        @Test
        @DisplayName("项目不存在抛 PROJECT_NOT_FOUND")
        void should_throw_when_project_not_found() {
            when(projectMapper.selectById(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.checkClose(1L),
                    "项目不存在应抛 BusinessException");
            assertEquals(40401, ex.getCode(), "应为项目不存在错误码");
        }
    }

    @Nested
    @DisplayName("归档")
    class ArchiveTest {

        @Test
        @DisplayName("CLOSE → ARCHIVED 归档成功并写入复盘记录")
        void should_archive_and_write_review() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_CLOSE);
            when(projectMapper.selectById(1L)).thenReturn(exist);
            when(projectMapper.updateById(any(ProjectEntity.class))).thenReturn(1);

            com.vibe.project.dto.ProjectArchiveDTO dto = new com.vibe.project.dto.ProjectArchiveDTO();
            dto.setReviewSummary("项目按时交付");
            dto.setLessonsLearned("需加强需求评审");

            projectService.archive(1L, dto);

            ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
            verify(projectMapper).updateById(captor.capture());
            ProjectEntity saved = captor.getValue();
            assertEquals(ProjectConstant.STATUS_ARCHIVED, saved.getStatus(), "状态应更新为 ARCHIVED");
            assertTrue(saved.getRemark().contains("复盘记录"), "备注应包含复盘记录");
            assertTrue(saved.getRemark().contains("经验沉淀"), "备注应包含经验沉淀");
        }

        @Test
        @DisplayName("非 CLOSE 状态归档抛异常")
        void should_throw_when_not_close() {
            ProjectEntity exist = buildProject(1L, ProjectConstant.STATUS_EXECUTE);
            when(projectMapper.selectById(1L)).thenReturn(exist);

            assertThrows(BusinessException.class,
                    () -> projectService.archive(1L, null),
                    "非 CLOSE 状态归档应抛 BusinessException");
            verify(projectMapper, never()).updateById(any(ProjectEntity.class));
        }
    }

    /**
     * 构造测试用项目实体
     */
    private ProjectEntity buildProject(Long id, String status) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(id);
        entity.setProjectCode("PRJ-202607-001");
        entity.setProjectName("测试项目");
        entity.setStatus(status);
        entity.setVersion(1);
        return entity;
    }
}
