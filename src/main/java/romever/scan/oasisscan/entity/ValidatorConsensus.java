package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "validator_consensus")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class ValidatorConsensus extends BaseEntity {
    private String entityId;
    private String nodeId;
    private String consensusId;
    private String tmAddress;
    private long height;
}
