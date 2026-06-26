package spring.abtechzone.modules.user.entity;

import java.util.Set;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.auth.entity.Role;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "username", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String username;

    String password;
    String email;
    String firstName;
    String lastName;
    boolean isActive;

    @ManyToMany
    Set<Role> roles; // Các phần tử trong Set là unique
}
