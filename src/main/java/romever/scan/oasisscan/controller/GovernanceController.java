package romever.scan.oasisscan.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.service.GovernanceService;

@Slf4j
@RestController
@RequestMapping("/governance")
public class GovernanceController {

    @Autowired
    private GovernanceService governanceService;

    @GetMapping("/proposals")
    public ApiResult proposalList() {
        return ApiResult.ok(governanceService.proposalList());
    }

    @GetMapping("/proposal")
    public ApiResult proposal(
            @RequestParam(value = "id") long id
    ) {
        return ApiResult.ok(governanceService.proposal(id));
    }
}
