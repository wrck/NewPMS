package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysNoticeQueryDTO;
import com.vibe.system.entity.SysNoticeEntity;
import com.vibe.system.vo.SysNoticeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 站内信 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNoticeEntity> {

    /**
     * 按接收人分页查询站内信
     */
    IPage<SysNoticeVO> selectNoticePage(IPage<SysNoticeVO> page,
                                        @Param("query") SysNoticeQueryDTO query,
                                        @Param("recipientId") Long recipientId);

    /**
     * 统计未读数量
     */
    long countUnread(@Param("recipientId") Long recipientId);
}
