package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.entity.ValidatorInfo;
import romever.scan.oasisscan.repository.ValidatorInfoRepository;
import romever.scan.oasisscan.utils.Texts;
import romever.scan.oasisscan.vo.SearchType;
import romever.scan.oasisscan.vo.chain.AccountInfo;
import romever.scan.oasisscan.vo.response.BlockDetailResponse;
import romever.scan.oasisscan.vo.response.SearchResponse;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SearchService {

    @Autowired
    private ApiClient apiClient;
    @Autowired
    private ValidatorInfoRepository validatorInfoRepository;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TransactionService transactionService;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public SearchResponse search(String key) {
        SearchResponse response = new SearchResponse();
        response.setKey(key);

        String result = null;
        SearchType searchType = SearchType.None;
        if (key.startsWith("oasis")) {
            AccountInfo accountInfo = apiClient.accountInfo(key, null);
            if (accountInfo != null) {
                if (validatorInfoRepository.findByEntityAddress(key).isPresent()) {
                    result = key;
                    searchType = SearchType.Validator;
                } else {
                    Optional<ValidatorInfo> optional = validatorInfoRepository.findByNodeAddress(key);
                    if (optional.isPresent()) {
                        result = optional.get().getEntityAddress();
                        searchType = SearchType.Validator;
                    } else {
                        result = key;
                        searchType = SearchType.Account;
                    }
                }
            }
        } else if (key.endsWith("=")) {
            Optional<ValidatorInfo> optional = validatorInfoRepository.findByEntityId(key);
            if (optional.isPresent()) {
                result = optional.get().getEntityAddress();
                searchType = SearchType.Validator;
            }
        } else if (Texts.isNumeric(key)) {
            long height = Long.parseLong(key);
            if (blockService.detail(height) != null) {
                result = key;
                searchType = SearchType.Block;
            }
        } else {
            String hash = key;
            if (Texts.isHex(hash)) {
                //transaction
                if (transactionService.detail(hash) != null) {
                    result = key;
                    searchType = SearchType.Transaction;
                } else {
                    //block
                    hash = Texts.hexToBase64(hash);
                }
            }
            //block
            if (Texts.isBase64(hash)) {
                BlockDetailResponse blockDetailResponse = blockService.detail(hash);
                if (blockDetailResponse != null) {
                    result = String.valueOf(blockDetailResponse.getHeight());
                    searchType = SearchType.Block;
                }
            }
        }

        response.setResult(result);
        response.setType(searchType.getName());
        return response;
    }
}
