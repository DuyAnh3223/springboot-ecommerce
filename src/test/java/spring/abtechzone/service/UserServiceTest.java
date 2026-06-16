package spring.abtechzone.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import spring.abtechzone.dto.request.UserCreationRequest;
import spring.abtechzone.dto.response.UserResponse;
import spring.abtechzone.entity.User;
import spring.abtechzone.exception.AppException;
import spring.abtechzone.repository.UserRepository;


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource("/test.properties")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;


    private UserCreationRequest request;
    private UserResponse response;
    private User user;

    @BeforeEach
    void initData(){
        request = UserCreationRequest.builder()
                .username("username")
                .password("password")
                .firstName("firstName")
                .lastName("lastName")
                .build();

        response = UserResponse.builder()
                .id("dwqsadsadg563433")
                .username("username")
                .firstName("firstName")
                .lastName("lastName")
                .build();

        user = User.builder()
                .id("dwqsadsadg563433")
                .username("username")
                .firstName("firstName")
                .lastName("lastName")
                .build();
    }

    @Test
    void createUser_validRequest_success(){
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);

        //WHEN
        var response = userService.createUser(request);

        //THEN
        assertThat(response.getId()).isEqualTo("dwqsadsadg563433");
        assertThat(response.getUsername()).isEqualTo("username");

    }

    @Test
    void createUser_userExisted_fail(){
        // GIVEN
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        when(userRepository.save(any())).thenReturn(user);

        //WHEN
        var exception = assertThrows(AppException.class, () -> userService.createUser(request));

        assertThat(exception.getErrorCode().getCode()).isEqualTo(1001);
    }

    @Test
    @WithMockUser(username = "username" )
    void getMyInfo_valid_success(){
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        var response = userService.getMyInfo();

        assertThat(response.getId()).isEqualTo("dwqsadsadg563433");
        assertThat(response.getUsername()).isEqualTo("username");
    }

    @Test
    @WithMockUser(username = "username" )
    void getMyInfo_userNotFound_error(){
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.ofNullable(null));

        var exception = assertThrows(AppException.class,
                () -> userService.getMyInfo());

        assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }


}
