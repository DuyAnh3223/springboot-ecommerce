package spring.abtechzone.mapper;

import org.mapstruct.Mapper;

import spring.abtechzone.dto.request.PermissionRequest;
import spring.abtechzone.dto.response.PermissionResponse;
import spring.abtechzone.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
