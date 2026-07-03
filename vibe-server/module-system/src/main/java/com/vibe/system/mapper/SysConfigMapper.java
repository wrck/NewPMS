package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.system.dto.SysConfigQueryDTO;
import com.vibe.system.entity.SysConfigEntity;
import com.vibe.system.vo.SysConfigVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfigEntity> {

    /**
     * 分页查询配置
     */
    IPage<SysConfigVO> selectConfigPage(IPage<SysConfigVO> page,
                                        @Param("query") SysConfigQueryDTO query);

    /**
     * 按 configKey 查询配置
     */
    SysConfigEntity selectByConfigKey(@Param("configKey") String configKey);
}
