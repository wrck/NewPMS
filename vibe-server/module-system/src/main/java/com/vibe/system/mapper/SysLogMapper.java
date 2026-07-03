package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysLogQueryDTO;
import com.vibe.system.entity.SysLogEntity;
import com.vibe.system.vo.SysLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 操作日志 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLogEntity> {

    /**
     * 分页查询操作日志（含操作人姓名）
     */
    IPage<SysLogVO> selectLogPage(IPage<SysLogVO> page, @Param("query") SysLogQueryDTO query);
}
