package spring.abtechzone.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

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
