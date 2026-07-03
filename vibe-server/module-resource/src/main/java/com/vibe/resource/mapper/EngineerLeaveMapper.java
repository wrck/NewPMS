package com.vibe.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.resource.dto.EngineerLeaveQueryDTO;
import com.vibe.resource.entity.EngineerLeaveEntity;
import com.vibe.resource.vo.EngineerLeaveVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 工程师请假 Mapper
 *
 * @author vibe
 */
@Mapper
public interface EngineerLeaveMapper extends BaseMapper<EngineerLeaveEntity> {

    /**
     * 分页查询请假记录（含工程师姓名）
     */
    IPage<EngineerLeaveVO> selectLeavePage(IPage<EngineerLeaveVO> page,
                                           @Param("query") EngineerLeaveQueryDTO query);

    /**
     * 查询工程师在某日期范围内的已批准请假记录（用于不可分配时段标记）
     */
    List<EngineerLeaveEntity> selectApprovedInRange(@Param("engineerId") Long engineerId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);
}
