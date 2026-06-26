package spring.abtechzone.modules.user.dto.request;

import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @Size(min = 3, message = "USERNAME_INVALID")
    String username;

    @Size(min = 3, message = "PASSWORD_INVALID")
    String password;

    String firstName;
    String lastName;
    //     boolean delFlag;
    //     boolean status;

}
