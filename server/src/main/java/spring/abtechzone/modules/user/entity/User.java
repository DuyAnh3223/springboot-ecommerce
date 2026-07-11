package spring.abtechzone.modules.user.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.auth.entity.UserRole;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue
    UUID id;

    @Column(name = "username", unique = true, columnDefinition = "citext")
    String username;

    String passwordHash;

    @Column(unique = true, columnDefinition = "citext")
    String email;

    String firstName;
    String lastName;
    boolean isActive;
    String phone;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<UserRole> roles; // Các phần tử trong Set là unique

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<UserAddress> userAddresses = new ArrayList<>();
}
