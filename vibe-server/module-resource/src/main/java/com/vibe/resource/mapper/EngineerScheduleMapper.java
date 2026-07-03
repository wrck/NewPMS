package com.vibe.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.resource.dto.EngineerScheduleQueryDTO;
import com.vibe.resource.entity.EngineerScheduleEntity;
import com.vibe.resource.vo.EngineerScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工程师排期 Mapper
 *
 * @author vibe
 */
@Mapper
public interface EngineerScheduleMapper extends BaseMapper<EngineerScheduleEntity> {

    /**
     * 分页查询排期（含工程师姓名/任务名称/项目名称）
     */
    IPage<EngineerScheduleVO> selectSchedulePage(IPage<EngineerScheduleVO> page,
                                                 @Param("query") EngineerScheduleQueryDTO query);

    /**
     * 日历查询：按工程师 + 时间范围查询排期列表
     *
     * @param engineerId 工程师ID（可为空，表示查询全部）
     * @param taskId     任务ID（可为空）
     * @param startTime  时间范围开始
     * @param endTime    时间范围结束
     */
    List<EngineerScheduleVO> selectCalendar(@Param("engineerId") Long engineerId,
                                            @Param("taskId") Long taskId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("scheduleType") String scheduleType);

    /**
     * 冲突检测查询：查询工程师在某时段内重叠的 TASK 类型排期记录
     *
     * <p>重叠定义：existing.start_time &lt; new.end_time AND existing.end_time &gt; new.start_time</p>
     *
     * @param engineerId 工程师ID
     * @param startTime  待检测时段开始
     * @param endTime    待检测时段结束
     * @param excludeId  排除的排期ID（编辑场景排除自身）
     * @param scheduleTypes 检测的排期类型列表（默认只检测 TASK）
     */
    List<EngineerScheduleEntity> selectConflicts(@Param("engineerId") Long engineerId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime,
                                                 @Param("excludeId") Long excludeId,
                                                 @Param("scheduleTypes") List<String> scheduleTypes);

    /**
     * 按任务ID查询排期
     */
    EngineerScheduleEntity selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 按任务ID逻辑删除排期（退回/转派时释放原排期）
     */
    int deleteByTaskId(@Param("taskId") Long taskId);
}
