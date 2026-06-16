package spring.abtechzone.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import spring.abtechzone.dto.request.RoleRequest;
import spring.abtechzone.dto.response.RoleResponse;
import spring.abtechzone.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
