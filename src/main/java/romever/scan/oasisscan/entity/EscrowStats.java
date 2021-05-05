package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "escrow_stats")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class EscrowStats extends BaseEntity {
    private String entityAddress;
    private String escrow;
    private long height;
    private String date;
}
