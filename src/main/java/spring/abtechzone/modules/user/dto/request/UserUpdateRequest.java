package spring.abtechzone.modules.user.dto.request;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    String password;
    String firstName;
    String lastName;
    //     boolean delFlag;
    //     boolean status;
    List<String> roles;
}
