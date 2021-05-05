package romever.scan.oasisscan.vo.response;

import lombok.Data;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.chain.Block;

import java.util.List;
import java.util.Map;

@Data
public class BlockDetailResponse {
    private long height;
    private long epoch;
    private long timestamp;
    private long time;
    private String hash;
    private int txs;
    private String proposer;
    private String entityId;
    private String entityAddress;
    private String name;

    public static BlockDetailResponse of(Block block, Map<String, ValidatorInfo> tmAddressMap) {
        BlockDetailResponse response = new BlockDetailResponse();
        response.setHeight(block.getHeight());
        response.setHash(Texts.base64ToHex(block.getHash()));
        Block.MetaData.BlockHeader header = block.getMetadata().getHeader();
        if (header != null) {
            response.setProposer(header.getProposer_address());
        }
        response.setTxs(block.getTxs());
        response.setTimestamp(block.getTime().toEpochSecond());
        response.setTime(System.currentTimeMillis() / 1000 - response.getTimestamp());

        if (!CollectionUtils.isEmpty(tmAddressMap)) {
            ValidatorInfo validatorInfo = tmAddressMap.get(response.getProposer());
            if (validatorInfo != null) {
                response.setEntityId(validatorInfo.getEntityId());
                response.setEntityAddress(validatorInfo.getEntityAddress());
                response.setName(validatorInfo.getName());
            }
        }
        return response;
    }
}
