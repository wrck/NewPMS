package com.vibe.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.delivery.dto.CutoverPlanQueryDTO;
import com.vibe.delivery.entity.CutoverPlanEntity;
import com.vibe.delivery.vo.CutoverPlanDetailVO;
import com.vibe.delivery.vo.CutoverPlanVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 割接方案 Mapper
 *
 * @author vibe
 */
@Mapper
public interface CutoverPlanMapper extends BaseMapper<CutoverPlanEntity> {

    /**
     * 分页查询割接方案（关联 project / sys_user 编制人姓名）
     */
    IPage<CutoverPlanVO> selectPlanPage(IPage<CutoverPlanVO> page, @Param("query") CutoverPlanQueryDTO query);

    /**
     * 按 ID 查询割接方案详情（含项目名/编制人名/审批人名）
     */
    CutoverPlanDetailVO selectDetailVoById(@Param("id") Long id);
}
