package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "runtime_node")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class RuntimeNode extends BaseEntity {
    private String runtimeId;
    private String nodeId;
    private String entityId;
}
