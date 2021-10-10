package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Data
@Entity
@Table(name = "runtime_stats")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class RuntimeStats extends BaseEntity {
    private String runtimeId;
    private String entityId;
    private long height;
    private long round;
    @Enumerated(value = EnumType.ORDINAL)
    private RuntimeStatsType statsType;
}
