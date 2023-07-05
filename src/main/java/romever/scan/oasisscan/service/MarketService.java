package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import romever.scan.oasisscan.common.client.CoinGeckoClient;
import romever.scan.oasisscan.utils.Numeric;
import romever.scan.oasisscan.utils.Times;
import romever.scan.oasisscan.vo.response.ChartResponse;
import romever.scan.oasisscan.vo.response.market.ChartsResponse;
import romever.scan.oasisscan.vo.response.market.MarketResponse;

import java.time.LocalDateTime;
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
    public ChartsResponse chart() {
        ChartsResponse response = new ChartsResponse();
        try {
            CoinGeckoClient.MarketChart chart = coinGeckoClient.marketChart(10);
            if (chart != null) {
                List<ChartResponse> prices = Lists.newArrayList();
                for (List<Number> price : chart.getPrices()) {
                    ChartResponse c = new ChartResponse();
                    c.setKey(Times.format(Times.toLocalDateTime(price.get(0).longValue()), Times.DATETIME_FORMATTER));
                    c.setValue(price.get(1));
                    prices.add(c);
                }
                response.setPrice(prices);
                List<ChartResponse> marketCaps = Lists.newArrayList();
                for (List<Number> marketCap : chart.getMarket_caps()) {
                    ChartResponse c = new ChartResponse();
                    c.setKey(Times.format(Times.toLocalDateTime(marketCap.get(0).longValue()), Times.DATETIME_FORMATTER));
                    c.setValue(marketCap.get(1));
                    marketCaps.add(c);
                }
                response.setMarketCap(marketCaps);
                List<ChartResponse> volumes = Lists.newArrayList();
                for (List<Number> volume : chart.getTotal_volumes()) {
                    ChartResponse c = new ChartResponse();
                    c.setKey(Times.format(Times.toLocalDateTime(volume.get(0).longValue()), Times.DATETIME_FORMATTER));
                    c.setValue(volume.get(1));
                    volumes.add(c);
                }
                response.setVolume(volumes);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return response;
    }

    @Cached(expire = 5, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.MINUTES)
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
            ChartsResponse chart = marketService.chart();
            List<ChartResponse> volumes = chart.getVolume();
            if (!CollectionUtils.isEmpty(volumes)) {
                double today = volumes.get(volumes.size() - 1).getValue().doubleValue();
                double yesterday = volumes.get(volumes.size() - 2).getValue().doubleValue();
                double pct = Numeric.divide(today - yesterday, yesterday, 7) * 100;
                response.setVolumeChangePct24h(pct);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return response;
    }

}
