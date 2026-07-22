package com.vibe.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.delivery.entity.CutoverExecutionLogEntity;
import com.vibe.delivery.vo.CutoverExecutionLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 割接操作日志 Mapper
 *
 * @author vibe
 */
@Mapper
public interface CutoverExecutionLogMapper extends BaseMapper<CutoverExecutionLogEntity> {

    /**
     * 按割接方案ID查询操作日志列表（关联 cutover_step 步骤名）
     */
    List<CutoverExecutionLogVO> selectLogVoListByPlanId(@Param("planId") Long planId);
}
