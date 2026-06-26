package spring.abtechzone.modules.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import spring.abtechzone.modules.auth.dto.request.RoleRequest;
import spring.abtechzone.modules.auth.dto.response.RoleResponse;
import spring.abtechzone.modules.auth.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
