package romever.scan.oasisscan.controller;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.entity.NetworkInfo;
import romever.scan.oasisscan.repository.DelegatorRepository;
import romever.scan.oasisscan.service.BlockService;
import romever.scan.oasisscan.service.TransactionService;
import romever.scan.oasisscan.service.ValidatorService;
import romever.scan.oasisscan.vo.response.DashboardNetworkResponse;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private ValidatorService validatorService;
    @Autowired
    private ApiClient apiClient;
    @Autowired
    private DelegatorRepository delegatorRepository;

    @GetMapping("/network")
    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public ApiResult network() {
        DashboardNetworkResponse response = new DashboardNetworkResponse();

        NetworkInfo networkInfo = validatorService.networkInfo();
        Long curHeight = apiClient.getCurHeight();
        long epoch = apiClient.epoch(curHeight);
        long totalTxs = transactionService.totalTransactions();

        response.setCurHeight(networkInfo.getHeight());
        response.setCurEpoch(epoch);
        response.setTotalTxs(totalTxs);
        response.setTotalEscrow(networkInfo.getEscrow());
        response.setActiveValidator(networkInfo.getActiveValidator());
        response.setTotalDelegate(delegatorRepository.countDistinctDelegator());

        return ApiResult.ok(response);
    }
}
