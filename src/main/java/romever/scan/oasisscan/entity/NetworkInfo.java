package romever.scan.oasisscan.entity;

import lombok.Data;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Data
public class NetworkInfo implements RowMapper<NetworkInfo> {
    private long height;
    private int onlineNodes;
    private int totalNodes;
    private int entity;
    private String escrow;
    private int activeValidator;

    @Override
    public NetworkInfo mapRow(ResultSet resultSet, int i) throws SQLException {
        NetworkInfo n = new NetworkInfo();
        n.setOnlineNodes(resultSet.getInt("online_nodes"));
        n.setTotalNodes(resultSet.getInt("total_nodes"));
        n.setEntity(resultSet.getInt("entity"));
        n.setEscrow(resultSet.getString("escrow"));
        n.setActiveValidator(resultSet.getInt("active"));
        return n;
    }
}
