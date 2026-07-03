package com.vibe.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.resource.dto.BusinessTripQueryDTO;
import com.vibe.resource.entity.BusinessTripEntity;
import com.vibe.resource.vo.BusinessTripVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 出差 Mapper
 *
 * @author vibe
 */
@Mapper
public interface BusinessTripMapper extends BaseMapper<BusinessTripEntity> {

    /**
     * 分页查询出差记录（含工程师姓名/项目名/审批人姓名）
     */
    IPage<BusinessTripVO> selectTripPage(IPage<BusinessTripVO> page,
                                         @Param("query") BusinessTripQueryDTO query);

    /**
     * 按ID查询出差详情
     */
    BusinessTripVO selectVoById(@Param("id") Long id);
}
