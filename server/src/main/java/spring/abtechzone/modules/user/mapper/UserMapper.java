package spring.abtechzone.modules.user.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.modules.auth.dto.response.PermissionResponse;
import spring.abtechzone.modules.auth.dto.response.RoleResponse;
import spring.abtechzone.modules.auth.entity.Role;
import spring.abtechzone.modules.auth.entity.UserRole;
import spring.abtechzone.modules.user.dto.request.UserCreationRequest;
import spring.abtechzone.modules.user.dto.request.UserUpdateRequest;
import spring.abtechzone.modules.user.dto.response.UserResponse;
import spring.abtechzone.modules.user.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "passwordHash", source = "password")
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);

    default Set<RoleResponse> mapUserRoles(Set<UserRole> userRoles) {
        if (userRoles == null) return null;
        return userRoles.stream()
                .map(ur -> {
                    Role role = ur.getRole();
                    if (role == null) return null;
                    return RoleResponse.builder()
                            .id(role.getId())
                            .name(role.getName())
                            .description(role.getDescription())
                            .scope(role.getScope())
                            .permissions(
                                    role.getPermissions() != null
                                            ? role.getPermissions().stream()
                                            .map(p -> PermissionResponse.builder()
                                                    .id(p.getId())
                                                    .name(p.getName())
                                                    .description(p.getDescription())
                                                    .build())
                                            .collect(Collectors.toSet())
                                            : null)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }
}
