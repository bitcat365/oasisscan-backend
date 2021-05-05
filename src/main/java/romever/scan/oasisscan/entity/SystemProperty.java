package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "system_property")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class SystemProperty extends BaseEntity {
    private String property;
    private String value;
}
