package romever.scan.oasisscan;

import com.alibaba.fastjson.parser.ParserConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OasisscanApplication {

    public static void main(String[] args) {
        //fast json safe mode
        ParserConfig.getGlobalInstance().setSafeMode(true);

        SpringApplication.run(OasisscanApplication.class, args);
    }

}
