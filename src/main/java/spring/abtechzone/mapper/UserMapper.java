package spring.abtechzone.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.dto.request.UserCreationRequest;
import spring.abtechzone.dto.request.UserUpdateRequest;
import spring.abtechzone.dto.response.UserResponse;
import spring.abtechzone.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    //    @Mapping(source="firstName", target = "lastName",ignore = true)
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
