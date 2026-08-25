package spring.abtechzone.modules.order.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.Getter;
import lombok.Setter;
import spring.abtechzone.modules.user.entity.User;

@Getter
@Setter
@Entity
@Table(
        name = "order_status_history",
        indexes = {@Index(name = "idx_order_status_history_order_id", columnList = "order_id")})
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Size(max = 20)
    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Size(max = 20)
    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Size(max = 20)
    @Column(name = "to_status", length = 20)
    private String toStatus;

    @Size(max = 30)
    @Column(name = "actor_type", length = 30)
    private String actorType;

    @Size(max = 100)
    @Column(name = "actor_id", length = 100)
    private String actorId;

    @Size(max = 500)
    @Column(name = "note", length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
