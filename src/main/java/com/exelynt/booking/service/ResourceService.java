package com.exelynt.booking.service;

import com.exelynt.booking.dto.PageResponse;
import com.exelynt.booking.dto.ResourceRequest;
import com.exelynt.booking.dto.ResourceResponse;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getAllResources(String type, Boolean available, Pageable pageable) {
        Page<Resource> resourcePage = resourceRepository.searchResources(type, available, pageable);
        return PageResponse.fromPage(resourcePage.map(ResourceResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = findResourceOrThrow(id);
        return ResourceResponse.fromEntity(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = new Resource(
                request.getName(),
                request.getDescription(),
                request.getType().toUpperCase(),
                request.getPricePerUnit(),
                request.getAvailable()
        );

        Resource savedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(savedResource);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findResourceOrThrow(id);

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType().toUpperCase());
        resource.setPricePerUnit(request.getPricePerUnit());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }

        Resource updatedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(updatedResource);
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = findResourceOrThrow(id);
        resourceRepository.delete(resource);
    }

    private Resource findResourceOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID " + id + " not found"));
    }
}
