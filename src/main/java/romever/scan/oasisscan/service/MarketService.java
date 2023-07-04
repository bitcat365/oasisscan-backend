package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.client.CoinGeckoClient;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.vo.response.market.MarketResponse;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MarketService {

    @Autowired
    private CoinGeckoClient coinGeckoClient;
    @Autowired
    private MarketService marketService;

    @Cached(expire = 60, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public CoinGeckoClient.MarketChart chart() {
        CoinGeckoClient.MarketChart chart = null;
        try {
            chart = coinGeckoClient.marketChart(10);
        } catch (Exception e) {
            log.error("", e);
        }
        return chart;
    }

    @Cached(expire = 60, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public MarketResponse info() {
        MarketResponse response = null;
        try {
            CoinGeckoClient.CoinInfoResponse cgcInfo = coinGeckoClient.coinInfo();

            response = new MarketResponse();
            response.setPrice(cgcInfo.getMarket_data().getCurrent_price().get("usd"));
            response.setPriceChangePct24h(cgcInfo.getMarket_data().getPrice_change_percentage_24h());
            response.setMarketCap(cgcInfo.getMarket_data().getMarket_cap().get("usd").longValue());
            response.setMarketCapChangePct24h(cgcInfo.getMarket_data().getMarket_cap_change_percentage_24h());
            response.setRank(cgcInfo.getMarket_cap_rank());
            response.setVolume(cgcInfo.getMarket_data().getTotal_volume().get("usd").longValue());

            //volume change
            CoinGeckoClient.MarketChart chart = marketService.chart();
            List<List<Number>> volumes = chart.getTotal_volumes();
            if (!CollectionUtils.isEmpty(volumes)) {
                for (List<Number> item : volumes) {
                    long time = item.get(0).longValue();
                    double volume = item.get(1).doubleValue();
                    if (time >= System.currentTimeMillis() - 24 * 3600 * 1000) {
                        double pct = Numeric.divide(response.getVolume() - volume, volume, 7) * 100;
                        response.setVolumeChangePct24h(pct);
                    }
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return response;
    }

}
