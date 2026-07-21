package spring.abtechzone.modules.user.entity;

import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "user_address")
public class UserAddress {
    @Id
    @GeneratedValue
    UUID id;

    @Column(nullable = false)
    String recipientName;

    @Column(nullable = false)
    String phone;

    @Column(nullable = false)
    String province;

    @Column(nullable = false)
    String district;

    @Column(nullable = false)
    String ward;

    @Column(name = "line1", nullable = false)
    String streetAddress;

    String line2;

    @Builder.Default
    @Column(nullable = false)
    String country = "VN";

    @Builder.Default
    Boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}
