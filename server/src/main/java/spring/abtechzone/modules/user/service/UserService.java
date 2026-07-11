package spring.abtechzone.modules.user.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.constant.PredefinedRole;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.entity.Role;
import spring.abtechzone.modules.auth.entity.UserRole;
import spring.abtechzone.modules.auth.entity.UserRoleId;
import spring.abtechzone.modules.auth.repository.RoleRepository;
import spring.abtechzone.modules.user.dto.request.UserCreationRequest;
import spring.abtechzone.modules.user.dto.request.UserUpdateRequest;
import spring.abtechzone.modules.user.dto.response.UserResponse;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.mapper.UserMapper;
import spring.abtechzone.modules.user.repository.UserRepository;

@Service
@RequiredArgsConstructor // Auto generate constructor but required "final"
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // simplify "private final string" -> "string"
public class UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request) {

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.USER_EXISTS);
        }

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findByName(PredefinedRole.USER_ROLE).ifPresent(roles::add);

        final User finalUser = user;
        UUID globalScopeId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Set<UserRole> userRoles = roles.stream()
                .map(role -> UserRole.builder()
                        .id(new UserRoleId(finalUser.getId(), role.getId(), globalScopeId))
                        .user(finalUser)
                        .role(role)
                        .build())
                .collect(Collectors.toSet());
        user.setRoles(userRoles);

        user = userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')") // Ktra quyền trước khi chạy ~~ @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(UUID userId) {
        return userMapper.toUserResponse(findUserById(userId));
    }

    @PostAuthorize("returnObject.username == authentication.name") // Chạy xong rồi mới ktra quyền => Đúng thì return
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        User user = findUserById(userId);

        userMapper.updateUser(user, request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        var roles = roleRepository.findByNameIn(request.getRoles());
        UUID globalScopeId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Set<UserRole> userRoles = roles.stream()
                .map(role -> UserRole.builder()
                        .id(new UserRoleId(user.getId(), role.getId(), globalScopeId))
                        .user(user)
                        .role(role)
                        .build())
                .collect(Collectors.toSet());

        if (user.getRoles() == null) {
            user.setRoles(userRoles);
        } else {
            user.getRoles().clear();
            user.getRoles().addAll(userRoles);
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(UUID userId) {
        userRepository.deleteById(userId);
    }
}
