package romever.scan.oasisscan.common.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import romever.scan.oasisscan.utils.okhttp.OkHttp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class CoinGeckoClient {

    private static final String BASE_URL = "https://api.coingecko.com";
    private static final String COINGECKO_ID = "oasis-network";

    @Data
    public static class CoinInfoResponse {
        private int market_cap_rank;
        private MarketData market_data;

        @Data
        public static class MarketData {
            private Map<String, Double> current_price;
            private Map<String, Number> total_volume;
            private Map<String, Number> market_cap;
            private double price_change_percentage_24h;
            private double market_cap_change_percentage_24h;
        }
    }

    @Data
    public static class MarketChart {
        private List<List<Number>> prices;
        private List<List<Number>> market_caps;
        private List<List<Number>> total_volumes;
    }

    public CoinInfoResponse coinInfo() throws IOException {
        Map<String, Object> params = Maps.newHashMap();
        params.put("localization", false);
        params.put("tickers", false);
        params.put("market_data", true);
        params.put("community_data", false);
        params.put("developer_data", false);
        params.put("sparkline", false);

        String url = String.format("%s/api/v3/coins/%s", BASE_URL, COINGECKO_ID);

        CoinInfoResponse result = OkHttp.of(url).queries(params).exec(new TypeReference<CoinInfoResponse>() {
        });
        return result;
    }

    public MarketChart marketChart(int days) throws IOException {
        Map<String, Object> params = Maps.newHashMap();
        params.put("vs_currency", "usd");
        params.put("days", days);

        String url = String.format("%s/api/v3/coins/%s/market_chart", BASE_URL, COINGECKO_ID);

        MarketChart result = OkHttp.of(url).queries(params).exec(new TypeReference<MarketChart>() {
        });
        return result;
    }
}
