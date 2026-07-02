package spring.abtechzone.modules.auth.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.auth.dto.request.RoleRequest;
import spring.abtechzone.modules.auth.dto.response.RoleResponse;
import spring.abtechzone.modules.auth.mapper.PermissionMapper;
import spring.abtechzone.modules.auth.mapper.RoleMapper;
import spring.abtechzone.modules.auth.repository.PermissionRepository;
import spring.abtechzone.modules.auth.repository.RoleRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;
    RoleMapper roleMapper;

    public RoleResponse create(RoleRequest request) {
        var role = roleMapper.toRole(request);

        var permissions = permissionRepository.findByNameIn(request.getPermissions());
        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream().map(roleMapper::toRoleResponse).toList();
    }

    public void delete(String role) {
        roleRepository.findByName(role).ifPresent(r -> roleRepository.deleteById(r.getId()));
    }
}
