package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "runtime")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class Runtime extends BaseEntity {
    private String name;
    private String runtimeId;
    private String entityId;
    private long scanRoundHeight;
    private long startRoundHeight;
    private long statsHeight;
    private long scanTxHeight;
}
