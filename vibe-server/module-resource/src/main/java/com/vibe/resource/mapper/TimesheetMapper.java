package com.vibe.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.resource.dto.TimesheetQueryDTO;
import com.vibe.resource.dto.TimesheetStatsQueryDTO;
import com.vibe.resource.entity.TimesheetEntity;
import com.vibe.resource.vo.TimesheetStatsVO;
import com.vibe.resource.vo.TimesheetVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工时 Mapper
 *
 * @author vibe
 */
@Mapper
public interface TimesheetMapper extends BaseMapper<TimesheetEntity> {

    /**
     * 分页查询工时（含工程师姓名/项目名/任务名/审批人姓名）
     */
    IPage<TimesheetVO> selectTimesheetPage(IPage<TimesheetVO> page,
                                           @Param("query") TimesheetQueryDTO query);

    /**
     * 按ID查询工时详情
     */
    TimesheetVO selectVoById(@Param("id") Long id);

    /**
     * 人天统计多维查询（按工程师/项目/月度）
     */
    List<TimesheetStatsVO> selectStats(@Param("query") TimesheetStatsQueryDTO query);
}
