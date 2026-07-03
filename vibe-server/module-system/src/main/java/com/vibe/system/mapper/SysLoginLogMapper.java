package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysLoginLogQueryDTO;
import com.vibe.system.entity.SysLoginLogEntity;
import com.vibe.system.vo.SysLoginLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 登录日志 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLogEntity> {

    /**
     * 分页查询登录日志
     */
    IPage<SysLoginLogVO> selectLoginLogPage(IPage<SysLoginLogVO> page,
                                            @Param("query") SysLoginLogQueryDTO query);
}
