package spring.abtechzone.modules.auth.dto.request;

import java.util.Set;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleRequest {
    Long id;
    String name;
    String description;
    String scope;
    Set<String> permissions;
}
