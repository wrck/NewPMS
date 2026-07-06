package com.vibe.lowcode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vibe.common.base.PageQuery;
import com.vibe.common.context.UserContextHolder;
import com.vibe.common.exception.BusinessException;
import com.vibe.common.result.PageResult;
import com.vibe.lowcode.constant.LowcodeConstant;
import com.vibe.lowcode.dto.LowcodeListConfigDTO;
import com.vibe.lowcode.entity.LowcodeListConfigEntity;
import com.vibe.lowcode.entity.LowcodeTemplateEntity;
import com.vibe.lowcode.mapper.LowcodeListConfigMapper;
import com.vibe.lowcode.mapper.LowcodeTemplateMapper;
import com.vibe.lowcode.service.LowcodeListConfigService;
import com.vibe.lowcode.util.JsonSchemaValidator;
import com.vibe.lowcode.vo.LowcodeListConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码列表配置 Service 实现
 *
 * @author vibe
 */
@Service
@RequiredArgsConstructor
public class LowcodeListConfigServiceImpl implements LowcodeListConfigService {

    private final LowcodeListConfigMapper listConfigMapper;
    private final LowcodeTemplateMapper templateMapper;

    @Override
    public PageResult<LowcodeListConfigVO> page(PageQuery query, String keyword) {
        LambdaQueryWrapper<LowcodeListConfigEntity> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(LowcodeListConfigEntity::getConfigCode, keyword)
                    .or().like(LowcodeListConfigEntity::getConfigName, keyword));
        }
        wrapper.orderByDesc(LowcodeListConfigEntity::getCreateTime);

        Page<LowcodeListConfigEntity> page = new Page<>(query.getPage(), query.getSize());
        Page<LowcodeListConfigEntity> result = listConfigMapper.selectPage(page, wrapper);

        List<LowcodeListConfigVO> records = new ArrayList<>();
        for (LowcodeListConfigEntity e : result.getRecords()) {
            LowcodeListConfigVO vo = new LowcodeListConfigVO();
            BeanUtils.copyProperties(e, vo);
            records.add(vo);
        }
        return PageResult.of(records, result.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public LowcodeListConfigVO getById(Long id) {
        LowcodeListConfigEntity entity = listConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("列表配置");
        }
        LowcodeListConfigVO vo = new LowcodeListConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(LowcodeListConfigDTO dto) {
        validateConfigCodeUnique(dto.getConfigCode());
        validateSchema(dto.getSchemaJson());

        LowcodeListConfigEntity entity = new LowcodeListConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        listConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, LowcodeListConfigDTO dto) {
        LowcodeListConfigEntity entity = listConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("列表配置");
        }
        validateConfigCodeUniqueExcludeSelf(dto.getConfigCode(), id);
        validateSchema(dto.getSchemaJson());

        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        listConfigMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LowcodeListConfigEntity entity = listConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("列表配置");
        }
        listConfigMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        LowcodeListConfigEntity source = listConfigMapper.selectById(id);
        if (source == null) {
            throw BusinessException.notFound("列表配置");
        }
        LowcodeListConfigEntity copy = new LowcodeListConfigEntity();
        BeanUtils.copyProperties(source, copy);
        copy.setId(null);
        copy.setConfigCode(source.getConfigCode() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setConfigName(source.getConfigName() + LowcodeConstant.COPY_NAME_SUFFIX);
        copy.setTemplateId(null);
        copy.setCreatorId(UserContextHolder.getUserId());
        listConfigMapper.insert(copy);
        return copy.getId();
    }

    @Override
    public String exportJson(Long id) {
        LowcodeListConfigEntity entity = listConfigMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("列表配置");
        }
        return entity.getSchemaJson();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importJson(LowcodeListConfigDTO dto) {
        validateSchema(dto.getSchemaJson());
        validateConfigCodeUnique(dto.getConfigCode());

        LowcodeListConfigEntity entity = new LowcodeListConfigEntity();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        }
        entity.setCreatorId(UserContextHolder.getUserId());
        listConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long instantiateFromTemplate(Long templateId, String configName) {
        LowcodeTemplateEntity template = templateMapper.selectById(templateId);
        if (template == null) {
            throw BusinessException.notFound("低代码模板");
        }
        if (!LowcodeConstant.TEMPLATE_TYPE_LIST.equals(template.getTemplateType())) {
            throw BusinessException.stateNotAllowed("模板类型不是 LIST，无法实例化为列表配置");
        }
        if (LowcodeConstant.STATUS_DISABLED.equals(template.getStatus())) {
            throw BusinessException.stateNotAllowed("模板已禁用，无法实例化");
        }

        LowcodeListConfigEntity entity = new LowcodeListConfigEntity();
        entity.setConfigCode(generateConfigCode(template.getTemplateCode()));
        entity.setConfigName(LowcodeConstant.INSTANTIATE_NAME_PREFIX + configName);
        entity.setSchemaJson(template.getSchemaJson());
        entity.setTemplateId(templateId);
        entity.setStatus(LowcodeConstant.STATUS_ENABLED);
        entity.setDescription(template.getDescription());
        entity.setCreatorId(UserContextHolder.getUserId());
        listConfigMapper.insert(entity);

        Integer currentCount = template.getUsageCount() == null ? 0 : template.getUsageCount();
        template.setUsageCount(currentCount + 1);
        templateMapper.updateById(template);

        return entity.getId();
    }

    /* ============ 校验辅助方法 ============ */

    private void validateConfigCodeUnique(String configCode) {
        Long count = listConfigMapper.selectCount(
                new LambdaQueryWrapper<LowcodeListConfigEntity>()
                        .eq(LowcodeListConfigEntity::getConfigCode, configCode));
        if (count > 0) {
            throw BusinessException.conflict("配置编码已存在：" + configCode);
        }
    }

    private void validateConfigCodeUniqueExcludeSelf(String configCode, Long id) {
        Long count = listConfigMapper.selectCount(
                new LambdaQueryWrapper<LowcodeListConfigEntity>()
                        .eq(LowcodeListConfigEntity::getConfigCode, configCode)
                        .ne(LowcodeListConfigEntity::getId, id));
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
