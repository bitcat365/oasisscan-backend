package romever.scan.oasisscan.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.common.client.ApiClient;
import romever.scan.oasisscan.service.*;

@Slf4j
@RestController
@RequestMapping("/chain")
public class ChainController {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private ScanChainService scanChainService;

    @GetMapping("/transactions")
    public ApiResult latestTransactions(
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "height", required = false) Long height,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "method", required = false) String method) {
        return transactionService.transactions(size, page, height, address, method);
    }

    @GetMapping("/blocks")
    public ApiResult latestBlocks(
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        return blockService.latestBlocks(size, page);
    }

    @GetMapping("/block/{height}")
    public ApiResult blockDetail(@PathVariable("height") Long height) {
        return ApiResult.ok(blockService.detail(height));
    }

    @GetMapping("/transaction/{hash}")
    public ApiResult transactionDetail(@PathVariable("hash") String hash) {
        return ApiResult.ok(transactionService.detail(hash));
    }

    @GetMapping("/methods")
    public ApiResult transactionMethod() {
        return ApiResult.list(transactionService.methods());
    }

    @GetMapping("/transactionhistory")
    public ApiResult transactionhistory() {
        return ApiResult.list(transactionService.transactionHistory());
    }

    @GetMapping("/getBlockByProposer")
    public ApiResult getBlockByProposer(
            @RequestParam(value = "proposer", required = false) String proposer,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page
    ) {
        return blockService.proposerBlocks(proposer, address, size, page);
    }

    @GetMapping("/powerevent")
    public ApiResult latestTransactions(
            @RequestParam(value = "size", required = false, defaultValue = "5") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "address", required = false) String address) {
        return transactionService.powerEvent(size, page, address);
    }

    @GetMapping("/account/info/{account}")
    public ApiResult accountInfo(@PathVariable("account") String account) {
        return ApiResult.ok(accountService.accountInfo(account));
    }

    @GetMapping("/account/list")
    public ApiResult accountInfo(
            @RequestParam(value = "size", required = false, defaultValue = "100") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        return accountService.accountList(page, size);
    }

    @GetMapping("/account/debonding")
    public ApiResult accountDebonding(
            @RequestParam(value = "size", required = false, defaultValue = "5") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "address") String address) {
        return accountService.debondings(address, page, size);
    }

    @GetMapping("/account/delegations")
    public ApiResult accountDelegations(
            @RequestParam(value = "size", required = false, defaultValue = "5") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "all", required = false, defaultValue = "false") boolean all,
            @RequestParam(value = "address") String address) {
        return accountService.delegations(address, all, page, size);
    }

    @GetMapping("/search")
    public ApiResult search(@RequestParam("key") String key) {
        return ApiResult.ok(searchService.search(key));
    }

    @GetMapping("/sync/block")
    public ApiResult syncBlock(@RequestParam("start") long start, @RequestParam("end") long end) {
        scanChainService.syncBlock(start, end);
        return ApiResult.ok();
    }
}
