package spring.abtechzone.modules.auth.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

import lombok.*;
import spring.abtechzone.modules.user.entity.User;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_role")
public class UserRole {
    @EmbeddedId
    UserRoleId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    Role role;

    @Column(name = "created_at", insertable = false, updatable = false)
    OffsetDateTime createdAt;
}
