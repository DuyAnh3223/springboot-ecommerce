package spring.abtechzone.modules.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.modules.user.dto.request.UserCreationRequest;
import spring.abtechzone.modules.user.dto.request.UserUpdateRequest;
import spring.abtechzone.modules.user.dto.response.UserResponse;
import spring.abtechzone.modules.user.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    //    @Mapping(source="firstName", target = "lastName",ignore = true)
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
