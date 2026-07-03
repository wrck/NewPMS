package com.vibe.resource.service.impl;

import com.vibe.common.exception.BusinessException;
import com.vibe.resource.dto.TaskRecommendationQueryDTO;
import com.vibe.resource.entity.EngineerSkillEntity;
import com.vibe.resource.mapper.EngineerMapper;
import com.vibe.resource.mapper.EngineerScheduleMapper;
import com.vibe.resource.mapper.EngineerSkillMapper;
import com.vibe.resource.mapper.ProjectTaskRefMapper;
import com.vibe.resource.service.EngineerScheduleService;
import com.vibe.resource.vo.EngineerRecommendationVO;
import com.vibe.resource.vo.EngineerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

/**
 * 任务派发服务智能推荐算法单元测试
 *
 * <p>通过 {@link TaskDispatchServiceImpl#recommend} 公共入口验证：
 * 技能匹配度评分(40%) + 区域就近评分(30%) + 当前负荷评分(30%) 加权综合评分。</p>
 *
 * @author vibe
 */
@DisplayName("智能推荐算法 TaskDispatchServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class TaskDispatchServiceImplTest {

    @Mock
    private EngineerMapper engineerMapper;
    @Mock
    private EngineerSkillMapper engineerSkillMapper;
    @Mock
    private EngineerScheduleMapper scheduleMapper;
    @Mock
    private EngineerScheduleService scheduleService;
    @Mock
    private ProjectTaskRefMapper projectTaskRefMapper;

    @InjectMocks
    private TaskDispatchServiceImpl taskDispatchService;

    /** 测试用时间范围 */
    private static final LocalDateTime START = LocalDateTime.of(2026, 7, 1, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 7, 1, 18, 0);

    @BeforeEach
    void setUp() {
        // 默认无冲突检测调用（recommend 不调用 detectConflict，仅 countWorkload）
    }

    /**
     * 构造推荐查询 DTO
     */
    private TaskRecommendationQueryDTO buildQuery(List<String> requiredSkills, String region) {
        TaskRecommendationQueryDTO query = new TaskRecommendationQueryDTO();
        query.setTaskId(1L);
        query.setRequiredSkills(requiredSkills);
        query.setRegion(region);
        query.setStartTime(START);
        query.setEndTime(END);
        query.setLimit(10);
        return query;
    }

    /**
     * 构造工程师 VO
     */
    private EngineerVO buildEngineer(Long id, String name, String region) {
        EngineerVO vo = new EngineerVO();
        vo.setId(id);
        vo.setName(name);
        vo.setRegion(region);
        vo.setStatus("ACTIVE");
        return vo;
    }

    /**
     * 构造工程师技能实体
     */
    private EngineerSkillEntity buildSkill(Long id, Long engineerId, String skillTag) {
        EngineerSkillEntity entity = new EngineerSkillEntity();
        entity.setId(id);
        entity.setEngineerId(engineerId);
        entity.setSkillTag(skillTag);
        entity.setLevel("SENIOR");
        return entity;
    }

    /**
     * Mock 候选工程师与技能数据
     */
    private void mockCandidates(List<EngineerVO> candidates, List<EngineerSkillEntity> skills) {
        when(engineerMapper.selectAvailableEngineers(nullable(List.class), isNull(), eq("ACTIVE")))
                .thenReturn(candidates);
        when(engineerSkillMapper.selectByEngineerIds(anyList()))
                .thenReturn(skills);
    }

    @Nested
    @DisplayName("技能匹配度评分")
    class SkillScoreTest {

        @Test
        @DisplayName("技能完全匹配评分 100")
        void should_return_skill_score_100_when_full_match() {
            // 候选工程师拥有全部所需技能
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 1L, "交换"),
                            buildSkill(103L, 1L, "安全")
                    ));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Arrays.asList("路由", "交换", "安全"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(1, result.size());
            assertEquals(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP),
                    result.get(0).getSkillScore(),
                    "完全匹配技能评分应为 100");
        }

        @Test
        @DisplayName("技能部分匹配按比例评分（2/3 = 66.67）")
        void should_return_skill_score_proportional_when_partial_match() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 1L, "交换")
                            // 缺少"安全"
                    ));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Arrays.asList("路由", "交换", "安全"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            // 2/3 * 100 = 66.67
            assertEquals(new BigDecimal("66.67"), result.get(0).getSkillScore(),
                    "2/3 匹配度评分应为 66.67");
        }

        @Test
        @DisplayName("技能完全不匹配评分 0")
        void should_return_skill_score_0_when_no_match() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.singletonList(buildSkill(101L, 1L, "无线")));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.singletonList("路由"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    result.get(0).getSkillScore(),
                    "完全不匹配技能评分应为 0");
        }

        @Test
        @DisplayName("未指定所需技能时技能评分 100（满分由区域与负荷决定排序）")
        void should_return_skill_score_100_when_no_required_skills() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.singletonList(buildSkill(101L, 1L, "路由")));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(null, "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(100),
                    result.get(0).getSkillScore(),
                    "未指定所需技能时技能评分应为 100");
        }
    }

    @Nested
    @DisplayName("区域就近评分")
    class RegionScoreTest {

        @Test
        @DisplayName("区域完全匹配评分 100")
        void should_return_region_score_100_when_exact_match() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(100),
                    result.get(0).getRegionScore(),
                    "区域完全匹配评分应为 100");
        }

        @Test
        @DisplayName("区域大小写不敏感匹配评分 100")
        void should_return_region_score_100_when_case_insensitive() {
            EngineerVO engineer = buildEngineer(1L, "张三", "BEIJING");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "beijing");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(100),
                    result.get(0).getRegionScore(),
                    "区域大小写不敏感匹配评分应为 100");
        }

        @Test
        @DisplayName("区域包含关系评分 70（taskRegion=北京-海淀，engineerRegion=北京）")
        void should_return_region_score_70_when_contains() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京-海淀");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(70),
                    result.get(0).getRegionScore(),
                    "区域包含关系评分应为 70");
        }

        @Test
        @DisplayName("区域不匹配评分 40")
        void should_return_region_score_40_when_no_match() {
            EngineerVO engineer = buildEngineer(1L, "张三", "上海");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(40),
                    result.get(0).getRegionScore(),
                    "区域不匹配评分应为 40");
        }

        @Test
        @DisplayName("未指定任务区域评分 80")
        void should_return_region_score_80_when_no_task_region() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), null);
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(80),
                    result.get(0).getRegionScore(),
                    "未指定任务区域评分应为 80");
        }

        @Test
        @DisplayName("工程师区域为空且任务区域非空评分 40")
        void should_return_region_score_40_when_engineer_region_blank() {
            EngineerVO engineer = buildEngineer(1L, "张三", null);
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(40),
                    result.get(0).getRegionScore(),
                    "工程师区域为空时评分应为 40");
        }
    }

    @Nested
    @DisplayName("负荷评分")
    class WorkloadScoreTest {

        @Test
        @DisplayName("0 负荷评分 100")
        void should_return_workload_score_100_when_zero() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(100),
                    result.get(0).getWorkloadScore(),
                    "0 负荷评分应为 100");
        }

        @Test
        @DisplayName("2 负荷评分 50（100 - 2*25）")
        void should_return_workload_score_50_when_2_tasks() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(2);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(50),
                    result.get(0).getWorkloadScore(),
                    "2 负荷评分应为 50");
        }

        @Test
        @DisplayName("4 负荷评分 0（100 - 4*25 = 0）")
        void should_return_workload_score_0_when_4_tasks() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(4);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.ZERO,
                    result.get(0).getWorkloadScore(),
                    "4 负荷评分应为 0");
        }

        @Test
        @DisplayName("高负荷（5+）评分 0（扣到 0 为止，不为负）")
        void should_return_workload_score_0_when_high_workload() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(10);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.ZERO,
                    result.get(0).getWorkloadScore(),
                    "5+ 负荷评分应为 0（不为负）");
            assertTrue(result.get(0).getHasConflict(), "负荷>0 应标记冲突");
        }

        @Test
        @DisplayName("负荷 1 评分 75（100 - 25）")
        void should_return_workload_score_75_when_1_task() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(1);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(BigDecimal.valueOf(75),
                    result.get(0).getWorkloadScore(),
                    "1 负荷评分应为 75");
            assertTrue(result.get(0).getHasConflict(), "负荷 1 应标记冲突");
        }
    }

    @Nested
    @DisplayName("综合评分 = 技能*0.4 + 区域*0.3 + 负荷*0.3")
    class CompositeScoreTest {

        @Test
        @DisplayName("全部满分：技能100/区域100/负荷0负荷100 → 综合 100")
        void should_return_composite_100_when_all_full() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 1L, "交换")
                    ));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Arrays.asList("路由", "交换"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            // 100*0.4 + 100*0.3 + 100*0.3 = 100
            assertEquals(new BigDecimal("100.00"), result.get(0).getScore(),
                    "全部满分综合评分应为 100");
        }

        @Test
        @DisplayName("技能100/区域100/负荷2(50) → 综合 100*0.4+100*0.3+50*0.3 = 85")
        void should_return_composite_85_when_workload_2() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 1L, "交换")
                    ));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(2);

            TaskRecommendationQueryDTO query = buildQuery(Arrays.asList("路由", "交换"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            // 100*0.4 + 100*0.3 + 50*0.3 = 40 + 30 + 15 = 85
            assertEquals(new BigDecimal("85.00"), result.get(0).getScore(),
                    "综合评分应为 85");
        }

        @Test
        @DisplayName("技能66.67/区域100/负荷0(100) → 综合 66.67*0.4+100*0.3+100*0.3 = 96.67")
        void should_return_composite_when_partial_skill_match() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 1L, "交换")
                            // 缺少"安全"
                    ));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Arrays.asList("路由", "交换", "安全"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            // 66.67*0.4 + 100*0.3 + 100*0.3 = 26.668 + 30 + 30 = 86.67
            assertEquals(new BigDecimal("86.67"), result.get(0).getScore(),
                    "部分技能匹配综合评分应为 86.67");
        }

        @Test
        @DisplayName("技能100/区域70/负荷0(100) → 综合 100*0.4+70*0.3+100*0.3 = 91")
        void should_return_composite_when_region_contains() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 1L, "交换")
                    ));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Arrays.asList("路由", "交换"), "北京-海淀");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            // 100*0.4 + 70*0.3 + 100*0.3 = 40 + 21 + 30 = 91
            assertEquals(new BigDecimal("91.00"), result.get(0).getScore(),
                    "区域包含关系综合评分应为 91");
        }

        @Test
        @DisplayName("技能100/区域40/负荷4(0) → 综合 100*0.4+40*0.3+0*0.3 = 52")
        void should_return_composite_when_low_scores() {
            EngineerVO engineer = buildEngineer(1L, "张三", "上海");
            mockCandidates(Collections.singletonList(engineer),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 1L, "交换")
                    ));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(4);

            TaskRecommendationQueryDTO query = buildQuery(Arrays.asList("路由", "交换"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            // 100*0.4 + 40*0.3 + 0*0.3 = 40 + 12 + 0 = 52
            assertEquals(new BigDecimal("52.00"), result.get(0).getScore(),
                    "低分组合综合评分应为 52");
        }
    }

    @Nested
    @DisplayName("排序与 Top N")
    class SortAndLimitTest {

        @Test
        @DisplayName("按综合评分降序排序")
        void should_sort_by_score_desc() {
            // 工程师1：满分；工程师2：负荷2（综合 85）
            EngineerVO eng1 = buildEngineer(1L, "张三", "北京");
            EngineerVO eng2 = buildEngineer(2L, "李四", "北京");
            mockCandidates(Arrays.asList(eng1, eng2),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 1L, "交换"),
                            buildSkill(103L, 2L, "路由"),
                            buildSkill(104L, 2L, "交换")
                    ));
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);
            when(scheduleService.countWorkload(eq(2L), eq(START), eq(END))).thenReturn(2);

            TaskRecommendationQueryDTO query = buildQuery(Arrays.asList("路由", "交换"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(2, result.size(), "应返回 2 个候选人");
            assertTrue(result.get(0).getScore().compareTo(result.get(1).getScore()) >= 0,
                    "应按综合评分降序排序");
            assertEquals(1L, result.get(0).getEngineerId(), "最高分应为工程师1");
            assertEquals(2L, result.get(1).getEngineerId(), "次高分应为工程师2");
        }

        @Test
        @DisplayName("limit 限制返回数量")
        void should_limit_result_count() {
            EngineerVO eng1 = buildEngineer(1L, "张三", "北京");
            EngineerVO eng2 = buildEngineer(2L, "李四", "北京");
            EngineerVO eng3 = buildEngineer(3L, "王五", "北京");
            mockCandidates(Arrays.asList(eng1, eng2, eng3),
                    Arrays.asList(
                            buildSkill(101L, 1L, "路由"),
                            buildSkill(102L, 2L, "路由"),
                            buildSkill(103L, 3L, "路由")
                    ));
            when(scheduleService.countWorkload(any(), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.singletonList("路由"), "北京");
            query.setLimit(2);
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertEquals(2, result.size(), "应只返回 2 个结果");
        }

        @Test
        @DisplayName("无候选工程师返回空列表")
        void should_return_empty_when_no_candidates() {
            when(engineerMapper.selectAvailableEngineers(anyList(), eq(null), eq("ACTIVE")))
                    .thenReturn(Collections.emptyList());

            TaskRecommendationQueryDTO query = buildQuery(Collections.singletonList("路由"), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertTrue(result.isEmpty(), "无候选人应返回空列表");
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParamValidationTest {

        @Test
        @DisplayName("开始时间晚于结束时间抛出参数异常")
        void should_throw_when_start_after_end() {
            TaskRecommendationQueryDTO query = new TaskRecommendationQueryDTO();
            query.setTaskId(1L);
            query.setStartTime(END);
            query.setEndTime(START);

            assertThrows(BusinessException.class, () -> taskDispatchService.recommend(query),
                    "开始时间晚于结束时间应抛 BusinessException");
        }

        @Test
        @DisplayName("开始时间等于结束时间抛出参数异常")
        void should_throw_when_start_equals_end() {
            TaskRecommendationQueryDTO query = new TaskRecommendationQueryDTO();
            query.setTaskId(1L);
            query.setStartTime(START);
            query.setEndTime(START);

            assertThrows(BusinessException.class, () -> taskDispatchService.recommend(query),
                    "开始时间等于结束时间应抛 BusinessException");
        }

        @Test
        @DisplayName("开始时间为空抛出异常")
        void should_throw_when_start_null() {
            TaskRecommendationQueryDTO query = new TaskRecommendationQueryDTO();
            query.setTaskId(1L);
            query.setEndTime(END);

            assertThrows(Exception.class, () -> taskDispatchService.recommend(query),
                    "开始时间为空应抛异常");
        }
    }

    @Nested
    @DisplayName("冲突标记")
    class ConflictFlagTest {

        @Test
        @DisplayName("0 负荷无冲突")
        void should_mark_no_conflict_when_zero_workload() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(0);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertFalse(result.get(0).getHasConflict(), "0 负荷应无冲突");
            assertEquals(0, result.get(0).getCurrentWorkload(), "当前负荷应为 0");
        }

        @Test
        @DisplayName("负荷>0 标记冲突")
        void should_mark_conflict_when_workload_positive() {
            EngineerVO engineer = buildEngineer(1L, "张三", "北京");
            mockCandidates(Collections.singletonList(engineer),
                    Collections.emptyList());
            when(scheduleService.countWorkload(eq(1L), eq(START), eq(END))).thenReturn(3);

            TaskRecommendationQueryDTO query = buildQuery(Collections.emptyList(), "北京");
            List<EngineerRecommendationVO> result = taskDispatchService.recommend(query);

            assertAll("冲突标记",
                    () -> assertTrue(result.get(0).getHasConflict(), "负荷>0 应标记冲突"),
                    () -> assertEquals(3, result.get(0).getCurrentWorkload(), "当前负荷应为 3"),
                    () -> assertTrue(result.get(0).getReason().contains("冲突"),
                            "理由应包含冲突提示")
            );
        }
    }
}
