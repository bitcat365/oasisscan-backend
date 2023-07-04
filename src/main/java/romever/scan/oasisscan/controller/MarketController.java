package romever.scan.oasisscan.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.service.MarketService;

@Slf4j
@RestController
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    @ApiOperation("Market chart")
    @GetMapping("/chart")
    public ApiResult chart() {
        return ApiResult.ok(marketService.chart());
    }

    @ApiOperation("Market info")
    @GetMapping("/info")
    public ApiResult info() {
        return ApiResult.ok(marketService.info());
    }
}
