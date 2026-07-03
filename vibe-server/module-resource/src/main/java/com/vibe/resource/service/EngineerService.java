package com.vibe.resource.service;

import com.vibe.common.result.PageResult;
import com.vibe.resource.dto.EngineerDTO;
import com.vibe.resource.dto.EngineerQueryDTO;
import com.vibe.resource.dto.EngineerSkillDTO;
import com.vibe.resource.vo.EngineerSkillVO;
import com.vibe.resource.vo.EngineerVO;

import java.util.List;

/**
 * 工程师资源池服务
 *
 * @author vibe
 */
public interface EngineerService {

    /**
     * 分页查询工程师
     */
    PageResult<EngineerVO> page(EngineerQueryDTO query);

    /**
     * 工程师详情（含技能列表）
     */
    EngineerVO getDetail(Long id);

    /**
     * 按用户ID查询工程师档案（供其他模块使用）
     */
    EngineerVO getByUserId(Long userId);

    /**
     * 新增工程师
     */
    Long create(EngineerDTO dto);

    /**
     * 编辑工程师
     */
    void update(EngineerDTO dto);

    /**
     * 删除工程师
     */
    void delete(Long id);

    /**
     * 变更工程师状态（在职/离职）
     */
    void changeStatus(Long id, String status);

    /**
     * 保存工程师技能列表（先删后插）
     */
    void saveSkills(Long engineerId, List<EngineerSkillDTO> skills);

    /**
     * 查询工程师技能列表
     */
    List<EngineerSkillVO> listSkills(Long engineerId);

    /**
     * 按技能/区域/负荷查询可用工程师（含当前负荷数）
     */
    List<EngineerVO> listAvailable(List<String> skillTags, String region);
}
