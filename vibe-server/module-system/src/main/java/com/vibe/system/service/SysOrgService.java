package com.vibe.system.service;

import com.vibe.system.dto.SysOrgDTO;
import com.vibe.system.vo.SysOrgVO;

import java.util.List;

/**
 * 组织架构服务
 *
 * @author vibe
 */
public interface SysOrgService {

    /**
     * 查询组织树
     */
    List<SysOrgVO> listTree();

    /**
     * 查询扁平列表
     */
    List<SysOrgVO> listAll();

    Long create(SysOrgDTO dto);

    void update(SysOrgDTO dto);

    void delete(Long id);

    SysOrgVO getDetail(Long id);
}
