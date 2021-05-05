package romever.scan.oasisscan;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.db.DataAccess;
import romever.scan.oasisscan.service.ScanValidatorService;
import romever.scan.oasisscan.vo.BlockStats;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
class OasisscanApplicationTests {

    @Autowired
    private DataAccess dataAccess;

    @Autowired
    private ScanValidatorService scanValidatorService;

    private static List<BlockStats> formatStatsTable(String result) {
        List<BlockStats> vs = Lists.newArrayList();
        try {
            if (StringUtils.isNotBlank(result)) {
                List<String> list = Arrays.asList(result.split("\n"));
                list = list.subList(2, list.size());
                for (String line : list) {
                    String[] arr = line.split("\\|");
                    BlockStats bs = new BlockStats();
                    bs.setRank(Integer.parseInt(arr[1].trim()));
                    bs.setEntityId(arr[2].trim());
                    bs.setNodes(Integer.parseInt(arr[3].trim()));
                    bs.setSignatures(Integer.parseInt(arr[4].trim()));
                    bs.setProposals(Integer.parseInt(arr[5].trim()));
                    bs.setAvailabilityScore(Long.parseLong(arr[6].trim()));
                    vs.add(bs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vs;
    }
}
