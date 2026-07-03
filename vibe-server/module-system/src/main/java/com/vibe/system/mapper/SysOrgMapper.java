package com.vibe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vibe.system.entity.SysOrgEntity;
import com.vibe.system.vo.SysOrgVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 组织架构 Mapper
 *
 * @author vibe
 */
@Mapper
public interface SysOrgMapper extends BaseMapper<SysOrgEntity> {

    /**
     * 查询全部组织（按排序）
     */
    List<SysOrgVO> selectAllOrgVo();
}
