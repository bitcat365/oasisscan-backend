package romever.scan.oasisscan.common.client;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import romever.scan.oasisscan.common.command.LocalCommandExecutorImpl;
import romever.scan.oasisscan.vo.BlockStats;
import romever.scan.oasisscan.vo.Entity;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Data
@RequiredArgsConstructor
public class CommandClient {

    private final String url;
    private final String node;
    private final String stats;
    private final String internal;
    private final LocalCommandExecutorImpl commandExecutor;

    public List<BlockStats> getBlockNodeStats(long start, long end) {
        String command = String.format("%s entity-signatures -a unix:%s --start-block %s --end-block %s", getStats(),
                getInternal(), start, end);
        try {
            String result = commandExecutor.executeCommand(command);
            return formatStatsTable(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Lists.newArrayList();
    }

    public long getCurHeight() {
        String command = String.format("curl -s %s | grep tendermint_consensus_height | grep " +
                "chain_id |  awk -F '} ' '{print$2}'", getUrl());
        try {
            String result = commandExecutor.executeCommand(command);
            log.info("height command result: " + result);
            if (StringUtils.isNotBlank(result)) {
                String regEx = "[^0-9]";
                Pattern p = Pattern.compile(regEx);
                Matcher m = p.matcher(result);
                return Long.parseLong(m.replaceAll("").trim());
            }
        } catch (Exception e) {
            log.error("error", e);
        }
        return -1;
    }

    public List<Entity> getEntityNode() {
        String command = String.format(
                "%s registry entity list -v -a unix:%s", getNode(), getInternal());
        String result = commandExecutor.executeCommand(command);
        try {
            if (StringUtils.isNotBlank(result)) {
                List<Entity> entities = Lists.newArrayList();
                result = result.replace("\n", "").replace("}{", "};{");
                String[] list = result.split(";");
                for (String r : list) {
                    Entity e = JSONObject.parseObject(r, Entity.class);
                    entities.add(e);
                }
                return entities;
            }
        } catch (Exception e) {
            log.error(result, e);
        }
        return null;
    }

    private static List<BlockStats> formatStatsTable(String result) {
        List<BlockStats> vs = Lists.newArrayList();
        try {
            if (StringUtils.isNotBlank(result)) {
                List<String> list = Arrays.asList(result.split("\n"));
                if (list.size() > 2) {
                    list = list.subList(2, list.size());
                    for (String line : list) {
                        String[] arr = line.split("\\|");
                        BlockStats bs = new BlockStats();
                        bs.setRank(Integer.parseInt(arr[1].trim()));
                        bs.setEntityId(arr[2].trim());
                        bs.setNodes(Integer.parseInt(arr[3].trim()));
                        bs.setSignatures(Integer.parseInt(arr[5].trim()));
                        bs.setProposals(Integer.parseInt(arr[7].trim()));
                        bs.setAvailabilityScore(Long.parseLong(arr[8].trim()));
                        vs.add(bs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vs;
    }


    public static void main(String[] args) throws IOException {
        String filePath = "entity.res";
        String result = FileUtils.readFileToString(new File(filePath), "UTF-8");
        if (StringUtils.isNotBlank(result)) {
            List<Entity> entities = Lists.newArrayList();
            result = result.replace("\n", "").replace("}{", "};{");
            String[] list = result.split(";");
            for (String r : list) {
                Entity e = JSONObject.parseObject(r, Entity.class);
                entities.add(e);
            }
            System.out.println(entities.size());
        }
    }
}
