package romever.scan.oasisscan.db;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.entity.NetworkInfo;
import romever.scan.oasisscan.entity.ValidatorInfoRank;
import romever.scan.oasisscan.utils.Texts;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class DataAccess {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public long getDbHeight() {
        String sql = "select height from validator_consensus order by height desc limit 1;";
        try {
            Long height = jdbcTemplate.queryForObject(sql, Long.class);
            if (height != null) {
                return height;
            }
        } catch (Exception e) {
            log.error("error", e);
        }
        return 0;
    }

    public List<Long> getStats(String entityId, long height, int limit, boolean proposal) {
        String table = "block_signs";
        if (proposal) {
            table = "block_proposals";
        }
        String sql = String.format("select height from %s where entity_id='%s' and height<=%s order by height desc limit %s;", table, entityId, height, limit);
        return jdbcTemplate.queryForList(sql, Long.class);
    }

    public List<ValidatorInfoRank> getValidatorList(long uptimeHeight) {
        String sql = "select a.id,a.entity_id,a.name,a.icon,a.escrow,a.balance,a.total_shares,a.nonce,a.nodes,a.score,a.ctime,a.mtime," +
                "count(b.height) as 'signs',count(c.height) as 'proposals',count(d.height) as 'signs_uptime' " +
                "from validator_info a " +
                "left join block_signs b on a.entity_id=b.entity_id " +
                "left join block_proposals c on a.entity_id=c.entity_id " +
                "left join block_signs d on a.entity_id=d.entity_id and d.height>" + uptimeHeight +
                " group by a.id,a.entity_id,a.name,a.icon,a.escrow,a.balance,a.total_shares,a.nonce,a.nodes,a.score,a.ctime,a.mtime;";
        return jdbcTemplate.query(sql, new ValidatorInfoRank());
    }

    public List<ValidatorInfoRank> getValidatorList() {
        String sql = "select * from(select (@rownum:=@rownum+1) as `rank`,c.* from validator_info c, (SELECT @rownum:=0) r " +
                "order by nodes desc,cast(escrow as decimal(32,9)) desc) t;";
        return jdbcTemplate.query(sql, new ValidatorInfoRank());
    }

    public ValidatorInfoRank getValidatorInfo(String entityId, String address) {
        try {
            if (Texts.isNotBlank(entityId)) {
                String sql = String.format(
                        "select * from(select (@rownum:=@rownum+1) as `rank`,c.* from validator_info c, (SELECT @rownum:=0) r " +
                                "order by nodes desc,cast(escrow as decimal(32,9)) desc) t where t.entity_id='%s';", entityId);
                return jdbcTemplate.queryForObject(sql, new ValidatorInfoRank());
            }
            if (Texts.isNotBlank(address)) {
                String sql = String.format(
                        "select * from(select (@rownum:=@rownum+1) as `rank`,c.* from validator_info c, (SELECT @rownum:=0) r " +
                                "order by nodes desc,cast(escrow as decimal(32,9)) desc) t where t.entity_address='%s';", address);
                return jdbcTemplate.queryForObject(sql, new ValidatorInfoRank());
            }
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public NetworkInfo networkInfo() {
        String sql = "select a.online_nodes,a.entity,a.escrow,b.active,c.total_nodes from " +
                "(select sum(nodes) 'online_nodes',count(id) 'entity',sum(escrow) 'escrow' from validator_info) as a," +
                "(select count(id) 'active' from validator_info where nodes=1) as b," +
                "(select count(node_id) 'total_nodes' from validator_consensus) as c;";
        return jdbcTemplate.queryForObject(sql, new NetworkInfo());
    }
}
