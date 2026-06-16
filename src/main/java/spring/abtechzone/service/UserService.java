package spring.abtechzone.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.constant.PredefinedRole;
import spring.abtechzone.dto.request.UserCreationRequest;
import spring.abtechzone.dto.request.UserUpdateRequest;
import spring.abtechzone.dto.response.UserResponse;
import spring.abtechzone.entity.Role;
import spring.abtechzone.entity.User;
import spring.abtechzone.exception.AppException;
import spring.abtechzone.exception.ErrorCode;
import spring.abtechzone.mapper.UserMapper;
import spring.abtechzone.repository.RoleRepository;
import spring.abtechzone.repository.UserRepository;

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
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);
        user.setRoles(roles);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex){
            throw new AppException(ErrorCode.USER_EXISTS);
        }


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

    private User findUserById(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostAuthorize("returnObject.username == authentication.name") // Chạy xong rồi mới ktra quyền => Đúng thì return
    public UserResponse getUser(String userId) {
        return userMapper.toUserResponse(findUserById(userId));
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = findUserById(userId);

        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }
}
