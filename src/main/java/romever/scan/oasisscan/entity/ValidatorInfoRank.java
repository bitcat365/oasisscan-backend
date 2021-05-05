package romever.scan.oasisscan.entity;

import lombok.Data;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Data
public class ValidatorInfoRank extends ValidatorInfo implements RowMapper<ValidatorInfoRank> {
    private int rank;

    @Override
    public ValidatorInfoRank mapRow(ResultSet resultSet, int i) throws SQLException {
        ValidatorInfoRank v = new ValidatorInfoRank();
        v.setRank(resultSet.getInt("rank"));
        v.setEntityId(resultSet.getString("entity_id"));
        v.setEntityAddress(resultSet.getString("entity_address"));
        v.setNodeId(resultSet.getString("node_id"));
        v.setNodeAddress(resultSet.getString("node_address"));
        v.setName(resultSet.getString("name"));
        v.setIcon(resultSet.getString("icon"));
        v.setWebsite(resultSet.getString("website"));
        v.setTwitter(resultSet.getString("twitter"));
        v.setKeybase(resultSet.getString("keybase"));
        v.setEmail(resultSet.getString("email"));
        v.setDescription(resultSet.getString("description"));
        v.setEscrow(resultSet.getString("escrow"));
        v.setEscrow_24h(resultSet.getString("escrow_24h"));
        v.setBalance(resultSet.getString("balance"));
        v.setTotalShares(resultSet.getString("total_shares"));
        v.setSigns(resultSet.getLong("signs"));
        v.setProposals(resultSet.getLong("proposals"));
        v.setScore(resultSet.getLong("score"));
        v.setSignsUptime(resultSet.getInt("signs_uptime"));
        v.setNodes(resultSet.getInt("nodes"));
        v.setStatus(resultSet.getBoolean("status"));
        v.setCommission(resultSet.getInt("commission"));
        return v;
    }
}
