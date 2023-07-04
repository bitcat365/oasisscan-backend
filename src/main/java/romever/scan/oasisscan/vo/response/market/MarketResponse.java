package romever.scan.oasisscan.vo.response.market;

import lombok.Data;

@Data
public class MarketResponse {
    private double price;
    private double priceChangePct24h;
    private int rank;
    private long marketCap;
    private double marketCapChangePct24h;
    private long volume;
    private double volumeChangePct24h;
}
