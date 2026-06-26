package spring.abtechzone.modules.user.dto.response;

import java.util.Set;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.auth.dto.response.RoleResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String username;
    String firstName;
    String lastName;
    //     boolean delFlag;
    //     boolean status;
    Set<RoleResponse> roles;
}
