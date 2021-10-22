package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Data
@Entity
@Table(name = "runtime_stats_info")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class RuntimeStatsInfo extends BaseEntity {
    private String runtimeId;
    private String entityId;
    @Enumerated(value = EnumType.ORDINAL)
    private RuntimeStatsType statsType;
    private long count;
}
