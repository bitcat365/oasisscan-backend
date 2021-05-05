package romever.scan.oasisscan.common;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Data
@Configuration
@ConfigurationProperties("elasticsearch")
public class ElasticsearchConfig {

    private static final int ADDRESS_LENGTH = 2;
    private static final String HTTP_SCHEME = "http";

    private List<String> hosts;
    private int connectionTimeout;
    private int socketTimeout;
    private int requestTimeout;
    private String blockIndex;
    private String transactionIndex;

    @Bean
    public RestClientBuilder restClientBuilder() {
        HttpHost[] httpHosts = hosts.stream()
                .map(this::makeHttpHost)
                .filter(Objects::nonNull)
                .toArray(HttpHost[]::new);
        log.debug("hosts:{}", Arrays.toString(httpHosts));
        return RestClient.builder(httpHosts);
    }


    @Bean
    public RestHighLevelClient elasticsearchClient(@Autowired RestClientBuilder restClientBuilder) {
        restClientBuilder.setRequestConfigCallback(
                requestConfigBuilder -> requestConfigBuilder
                        .setConnectionRequestTimeout(requestTimeout)
                        .setConnectTimeout(connectionTimeout)
                        .setSocketTimeout(socketTimeout));
        return new RestHighLevelClient(restClientBuilder);
    }


    private HttpHost makeHttpHost(String s) {
        assert StringUtils.isNotEmpty(s);
        String[] address = s.split(":");
        if (address.length == ADDRESS_LENGTH) {
            String ip = address[0];
            int port = Integer.parseInt(address[1]);
            return new HttpHost(ip, port, HTTP_SCHEME);
        } else {
            return null;
        }
    }
}
