package com.vibe.lowcode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.base.PageQuery;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.constant.LowcodeConstant;
import com.vibe.lowcode.dto.LowcodeRelationConfigDTO;
import com.vibe.lowcode.entity.LowcodeRelationConfigEntity;
import com.vibe.lowcode.entity.LowcodeTemplateEntity;
import com.vibe.lowcode.mapper.LowcodeRelationConfigMapper;
import com.vibe.lowcode.mapper.LowcodeTemplateMapper;
import com.vibe.lowcode.service.LowcodeRelationConfigService;
import com.vibe.lowcode.util.JsonSchemaValidator;
import com.vibe.lowcode.vo.LowcodeRelationConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码关联页配置 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class LowcodeRelationConfigServiceImpl implements LowcodeRelationConfigService {

    private final LowcodeRelationConfigMapper relationConfigMapper;
    private final LowcodeTemplateMapper templateMapper;

    @Override
    public PageResult<LowcodeRelationConfigVO> page(PageQuery query, String keyword) {
        LambdaQueryWrapper<LowcodeRelationConfigEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(LowcodeRelationConfigEntity::getConfigCode, keyword)
                    .or().like(LowcodeRelationConfigEntity::getConfigName, keyword));
        }
        wrapper.orderByDesc(LowcodeRelationConfigEntity::getCreateTime);

        Page<LowcodeRelationConfigEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<LowcodeRelationConfigEntity> result = relationConfigMapper.selectPage(page, wrapper);

        List<LowcodeRelationConfigVO> records = new ArrayList<>();
        for (LowcodeRelationConfigEntity e : result.getRecords()) {
            LowcodeRelationConfigVO vo = new LowcodeRelationConfigVO();
            BeanUtils.copyProperties(e, vo);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public LowcodeRelationConfigVO getById(Long id) {
        LowcodeRelationConfigEntity entity = relationConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("关联页配置");
        }
        LowcodeRelationConfigVO vo = new LowcodeRelationConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(LowcodeRelationConfigDTO dto) {
        validateConfigCodeUnique(dto.getConfigCode());
        validateSchema(dto.getSchemaJson());

        LowcodeRelationConfigEntity entity = new LowcodeRelationConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        relationConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, LowcodeRelationConfigDTO dto) {
        LowcodeRelationConfigEntity entity = relationConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("关联页配置");
        }
        validateConfigCodeUniqueExcludeSelf(dto.getConfigCode(), id);
        validateSchema(dto.getSchemaJson());

        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        relationConfigMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LowcodeRelationConfigEntity entity = relationConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("关联页配置");
        }
        relationConfigMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        LowcodeRelationConfigEntity source = relationConfigMapper.selectById(id);
        if (source == null) {
            throw BusinessException.notFound("关联页配置");
        }
        LowcodeRelationConfigEntity copy = new LowcodeRelationConfigEntity();
        BeanUtils.copyProperties(source, copy);
        copy.setId(null);
        copy.setConfigCode(source.getConfigCode() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setConfigName(source.getConfigName() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setTemplateId(null);
        copy.setCreatorId(UserContextHolder.getUserId());
        relationConfigMapper.insert(copy);
        return copy.getId();
    }

    @Override
    public String exportJson(Long id) {
        LowcodeRelationConfigEntity entity = relationConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("关联页配置");
        }
        return entity.getSchemaJson();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importJson(LowcodeRelationConfigDTO dto) {
        validateSchema(dto.getSchemaJson());
        validateConfigCodeUnique(dto.getConfigCode());

        LowcodeRelationConfigEntity entity = new LowcodeRelationConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        relationConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long instantiateFromTemplate(Long templateId, String configName) {
        LowcodeTemplateEntity template = templateMapper.selectById(templateId);
        if (template == null) {
            throw BusinessException.notFound("低代码模板");
        }
        if (!LowcodeConstant.TEMPLATE_TYPE_RELATION.equals(template.getTemplateType())) {
            throw BusinessException.stateNotAllowed("模板类型不是 RELATION，无法实例化为关联页配置");
        }
        if (LowcodeConstant.STATUS_DISABLED.equals(template.getStatus())) {
            throw BusinessException.stateNotAllowed("模板已禁用，无法实例化");
        }

        LowcodeRelationConfigEntity entity = new LowcodeRelationConfigEntity();
        entity.setConfigCode(generateConfigCode(template.getTemplateCode()));
        entity.setConfigName(LowcodeConstant.INSTANTIATE_NAME_PREFIX + configName);
        entity.setSchemaJson(template.getSchemaJson());
        entity.setTemplateId(templateId);
        entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        entity.setDescription(template.getDescription());
        entity.setCreatorId(UserContextHolder.getUserId());
        relationConfigMapper.insert(entity);

        Integer currentCount = template.getUsageCount() == null ? 0 : template.getUsageCount();
        template.setUsageCount(currentCount + 1);
        templateMapper.updateById(template);

        return entity.getId();
    }

    /* ============ 校验辅助方法 ============ */

    private void validateConfigCodeUnique(String configCode) {
        Long count = relationConfigMapper.selectCount(
                new LambdaQueryWrapper<LowcodeRelationConfigEntity>()
                        .eq(LowcodeRelationConfigEntity::getConfigCode, configCode));
        if (count > 0) {
            throw BusinessException.conflict("配置编码已存在：" + configCode);
        }
    }

    private void validateConfigCodeUniqueExcludeSelf(String configCode, Long id) {
        Long count = relationConfigMapper.selectCount(
                new LambdaQueryWrapper<LowcodeRelationConfigEntity>()
                        .eq(LowcodeRelationConfigEntity::getConfigCode, configCode)
                        .ne(LowcodeRelationConfigEntity::getId, id));
        if (count > 0) {
            throw BusinessException.conflict("配置编码已存在：" + configCode);
        }
    }

    private void validateSchema(String schemaJson) {
        if (!JsonSchemaValidator.isValid(schemaJson)) {
            List<String> errors = JsonSchemaValidator.validate(schemaJson);
            String detail = errors.isEmpty() ? "JSON Schema 格式不合法" : String.join("; ", errors);
            throw BusinessException.of(40000, "JSON Schema Draft 7 校验失败：" + detail);
        }
    }

    private String generateConfigCode(String templateCode) {
        return templateCode + "_" + System.currentTimeMillis();
    }
}
