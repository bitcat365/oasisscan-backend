package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "debonding")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class Debonding extends BaseEntity {
    private String validator;
    private String delegator;
    private String shares;
    private long debondEnd;
}
