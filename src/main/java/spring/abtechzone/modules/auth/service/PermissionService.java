package spring.abtechzone.modules.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.auth.dto.request.PermissionRequest;
import spring.abtechzone.modules.auth.dto.response.PermissionResponse;
import spring.abtechzone.modules.auth.entity.Permission;
import spring.abtechzone.modules.auth.mapper.PermissionMapper;
import spring.abtechzone.modules.auth.repository.PermissionRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    public PermissionResponse create(PermissionRequest request) {
        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    public List<PermissionResponse> getAll() {
        var permissions = permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toPermissionResponse).toList();
    }

    public void delete(String permission) {
        permissionRepository.deleteById(permission);
    }
}
