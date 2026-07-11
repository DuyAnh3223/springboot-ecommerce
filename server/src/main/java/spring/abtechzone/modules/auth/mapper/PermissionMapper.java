package spring.abtechzone.modules.auth.mapper;

import org.mapstruct.Mapper;

import spring.abtechzone.modules.auth.dto.request.PermissionRequest;
import spring.abtechzone.modules.auth.dto.response.PermissionResponse;
import spring.abtechzone.modules.auth.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
