package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysNoticeTemplateQueryDTO;
import com.vibe.system.entity.SysNoticeTemplateEntity;
import com.vibe.system.vo.SysNoticeTemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 通知模板 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysNoticeTemplateMapper extends BaseMapper<SysNoticeTemplateEntity> {

    /**
     * 分页查询通知模板
     */
    IPage<SysNoticeTemplateVO> selectTemplatePage(IPage<SysNoticeTemplateVO> page,
                                                  @Param("query") SysNoticeTemplateQueryDTO query);

    /**
     * 按模板编码查询
     */
    SysNoticeTemplateEntity selectByTemplateCode(@Param("templateCode") String templateCode);
}
