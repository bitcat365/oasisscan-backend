package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "delegator")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class Delegator extends BaseEntity {
    private String validator;
    private String delegator;
    private String shares;
}
