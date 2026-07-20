package spring.abtechzone.modules.user.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import spring.abtechzone.modules.user.dto.request.UserSearchRequest;
import spring.abtechzone.modules.user.dto.request.UserUpdateRequest;
import spring.abtechzone.modules.user.dto.response.UserResponse;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.mapper.UserMapper;
import spring.abtechzone.modules.user.repository.UserRepository;

@Service
@Transactional
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
        user.setActive(true);

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
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(UserSearchRequest request) {
        Specification<User> spec = Specification.where(hasKeyword(request.getSearch()))
                .and(isActive(request.getIsActive()));
        return userRepository.findAll(spec, request.toPageable()).map(userMapper::toUserResponse);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(UUID userId) {
        return userMapper.toUserResponse(findUserById(userId));
    }

    @PostAuthorize("returnObject.username == authentication.name or hasRole('ADMIN')")
    // Chạy xong rồi mới ktra quyền => Đúng thì return hoặc là ADMIN
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        User user = findUserById(userId);

        userMapper.updateUser(user, request);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

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
        User user = findUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private static Specification<User> hasKeyword(String keyword) {
        return ((root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), likeValue),
                    cb.like(cb.lower(root.get("email")), likeValue),
                    cb.like(cb.lower(root.get("firstName")), likeValue),
                    cb.like(cb.lower(root.get("lastName")), likeValue),
                    cb.like(cb.lower(root.get("phone")), likeValue));
        });
    }

    private static Specification<User> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return null;
            return cb.equal(root.get("isActive"), active);
        };
    }
}
