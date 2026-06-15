package spring.abtechzone.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.entity.Role;

import java.util.Set;

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
