package spring.abtechzone.modules.user.dto.response;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.auth.dto.response.RoleResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String username;
    String firstName;
    String lastName;
    Boolean active;
    String email;
    String phone;
    OffsetDateTime createdAt;

    Set<RoleResponse> roles;
}
