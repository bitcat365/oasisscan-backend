package romever.scan.oasisscan.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.jdbc.core.RowMapper;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.ResultSet;
import java.sql.SQLException;

@Data
@Entity
@Table(name = "validator_info")
@Cacheable
@DynamicUpdate
@DynamicInsert
public class ValidatorInfo extends BaseEntity {
    private String entityId;
    private String entityAddress;
    private String nodeId;
    private String nodeAddress;
    private String consensusId;
    private String tmAddress;
    private String name;
    private String icon;
    private String website;
    private String twitter;
    private String keybase;
    private String email;
    private String description;
    private String escrow;
    private String escrow_24h;
    private String balance;
    private String totalShares;
    private long signs;
    private long proposals;
    private long score;
    private long signsUptime;
    private int nodes;
    private boolean status;
    private long commission;
}
