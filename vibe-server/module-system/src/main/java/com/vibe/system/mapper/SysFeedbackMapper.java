package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysFeedbackQueryDTO;
import com.vibe.system.entity.SysFeedbackEntity;
import com.vibe.system.vo.SysFeedbackVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 反馈与工单 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysFeedbackMapper extends BaseMapper<SysFeedbackEntity> {

    /**
     * 管理员分页查询（含提交人/处理人姓名）
     *
     * @param page       分页对象
     * @param query      查询条件
     * @param submitterId 可选：限定提交人 ID（个人中心查询时传入）
     */
    IPage<SysFeedbackVO> selectFeedbackPage(IPage<SysFeedbackVO> page,
                                            @Param("query") SysFeedbackQueryDTO query,
                                            @Param("submitterId") Long submitterId);
}
