package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "account")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class Account extends BaseEntity {
    private String address;
    private long available;
    private long escrow;
    private long debonding;
    private long total;
}
