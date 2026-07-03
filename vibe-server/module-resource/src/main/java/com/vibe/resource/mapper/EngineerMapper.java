package com.vibe.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.vibe.annotation.DataPermission;
import com.vibe.resource.dto.EngineerQueryDTO;
import com.vibe.resource.entity.EngineerEntity;
import com.vibe.resource.vo.EngineerVO;
import com.vibe.resource.vo.WorkloadHeatmapVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工程师 Mapper
 *
 * @author vibe
 */
@Mapper
public interface EngineerMapper extends BaseMapper<EngineerEntity> {

    /**
     * 分页查询工程师（含登录账号、技能关联）
     *
     * <p>数据权限：ENGINEER 角色仅看 user_id = 当前 userId 的本人档案；
     * SUPER_ADMIN / DISPATCHER / PM 由 @PreAuthorize 在 Controller 层放行，
     * 拦截器对管理员角色不追加条件。</p>
     */
    @DataPermission(table = "e", engineerField = "user_id")
    IPage<EngineerVO> selectEngineerPage(IPage<EngineerVO> page,
                                         @Param("query") EngineerQueryDTO query);

    /**
     * 按ID查询工程师详情（含登录账号）
     */
    EngineerVO selectVoById(@Param("id") Long id);

    /**
     * 按用户ID查询工程师
     */
    EngineerEntity selectByUserId(@Param("userId") Long userId);

    /**
     * 按技能/区域/状态查询可用工程师列表
     *
     * @param skillTags 技能标签列表（任一匹配）
     * @param region    区域（精确匹配，可为空）
     * @param status    状态（默认 ACTIVE）
     */
    List<EngineerVO> selectAvailableEngineers(@Param("skillTags") List<String> skillTags,
                                              @Param("region") String region,
                                              @Param("status") String status);

    /**
     * 统计工程师在指定时段内的任务数（用于负荷计算）
     *
     * @param engineerIds 工程师ID列表
     * @param startTime   时段开始
     * @param endTime     时段结束
     */
    List<WorkloadHeatmapVO> selectWorkloadHeatmap(
            @Param("engineerIds") List<Long> engineerIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("region") String region);

    /**
     * 校验用户ID是否已关联工程师
     */
    Long countByUserId(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
}
