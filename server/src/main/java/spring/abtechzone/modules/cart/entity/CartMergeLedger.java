package spring.abtechzone.modules.cart.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.user.entity.User;

@Entity
@Table(
        name = "cart_merge_ledger",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_cart_merge_ledger_user_merge",
                        columnNames = {"user_id", "merge_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartMergeLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "merge_id", nullable = false)
    UUID mergeId;

    @Column(name = "request_hash", nullable = false, length = 64)
    String requestHash;

    @Column(name = "result_json", nullable = false, columnDefinition = "text")
    String resultJson;

    @Column(name = "created_at", nullable = false)
    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
