package romever.scan.oasisscan.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import romever.scan.oasisscan.common.command.ExecuteResult;
import romever.scan.oasisscan.common.command.LocalCommandExecutorImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CommandUtil {

    public static String exeCmd(String commandStr) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(commandStr);
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            log.info("execute command: " + commandStr + "\n" + "result: " + sb.toString());
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String command = "curl -s http://5.252.225.45:3000/ | grep tendermint_consensus_height | grep chain_id |  awk" +
                " -F '} ' '{print$2}'";
//        String command = "pwd";
        LocalCommandExecutorImpl executor = new LocalCommandExecutorImpl();
        String result = executor.executeCommand(command);
        if (StringUtils.isNotBlank(result)) {
            String regEx = "[^0-9]";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(result);
            System.out.println(Long.parseLong(m.replaceAll("").trim()));
        }
        System.out.println(result);
    }
}
