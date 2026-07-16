package com.notifsvc.template;

import com.notifsvc.common.ConflictException;
import com.notifsvc.common.NotFoundException;
import com.notifsvc.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TenantRepository tenantRepository;

    public TemplateService(TemplateRepository templateRepository, TenantRepository tenantRepository) {
        this.templateRepository = templateRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Template create(Long tenantId, TemplateCreateRequest request) {
        boolean exists = templateRepository.findTopByTenantIdAndNameOrderByVersionDesc(tenantId, request.name()).isPresent();
        if (exists) {
            throw new ConflictException("A template named '" + request.name() + "' already exists for this tenant; use revise to create a new version");
        }
        Template template = new Template();
        template.setTenant(tenantRepository.getReferenceById(tenantId));
        template.setName(request.name());
        template.setChannelType(request.channelType());
        template.setSubject(request.subject());
        template.setBody(request.body());
        template.setVersion(1);
        template.setActive(true);
        return templateRepository.save(template);
    }

    /** Creates a new version of an existing template, deactivating the prior active version. */
    @Transactional
    public Template revise(Long tenantId, Long templateId, TemplateReviseRequest request) {
        Template current = getByIdForTenant(tenantId, templateId);
        current.setActive(false);
        templateRepository.save(current);

        Template next = new Template();
        next.setTenant(current.getTenant());
        next.setName(current.getName());
        next.setChannelType(current.getChannelType());
        next.setSubject(request.subject());
        next.setBody(request.body());
        next.setVersion(current.getVersion() + 1);
        next.setActive(true);
        return templateRepository.save(next);
    }

    public List<Template> listActiveForTenant(Long tenantId) {
        return templateRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    public List<Template> listVersionHistory(Long tenantId, String name) {
        return templateRepository.findByTenantIdAndNameOrderByVersionDesc(tenantId, name);
    }

    public Template getByIdForTenant(Long tenantId, Long templateId) {
        return templateRepository.findByIdAndTenantId(templateId, tenantId)
                .orElseThrow(() -> new NotFoundException("No template with id " + templateId + " for this tenant"));
    }

    @Transactional
    public void deactivate(Long tenantId, Long templateId) {
        Template template = getByIdForTenant(tenantId, templateId);
        template.setActive(false);
        templateRepository.save(template);
    }
}
