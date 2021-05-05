package romever.scan.oasisscan.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import romever.scan.oasisscan.common.client.ApiClient;

@Data
@Configuration
@ConfigurationProperties("application")
public class ApplicationConfig {
    private String env;
    private OasisConfig oasis;
    private LevelDbConfig leveldb;
    private ValidatorConfig validator;
    private long upgradeStartHeight;

    public boolean isLocal() {
        return "local".equalsIgnoreCase(getEnv());
    }

    @Bean
    public ApiClient apiClient() {
        OasisConfig.Api api = oasis.getApi();
        return new ApiClient(api.getUrl(), api.getName());
    }

}
