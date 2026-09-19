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
@Table(name = "address")
public class Address {
    @Id
    @GeneratedValue
    UUID id;

    @Column(nullable = false)
    String recipientName;

    @Column(nullable = false)
    String phone;

    @Column(nullable = false)
    String province;

    @Column
    String district;

    @Column(nullable = false)
    String ward;

    @Column(name = "ghn_province_id")
    Integer ghnProvinceId;

    @Column(name = "ghn_district_id")
    Integer ghnDistrictId;

    @Column(name = "ghn_ward_code", length = 20)
    String ghnWardCode;

    @Column(name = "line1", nullable = false)
    String street;

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
