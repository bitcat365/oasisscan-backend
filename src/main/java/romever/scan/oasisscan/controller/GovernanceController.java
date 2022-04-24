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

    @Deprecated
    @GetMapping("/proposals")
    public ApiResult proposals() {
        return ApiResult.list(governanceService.proposals());
    }

    @GetMapping("/proposal")
    public ApiResult proposal(
            @RequestParam(value = "id") long id
    ) {
        return ApiResult.ok(governanceService.proposal(id));
    }

    @GetMapping("/votes")
    public ApiResult votes(
            @RequestParam(value = "id") long id
    ) {
        return ApiResult.list(governanceService.votes(id));
    }

    @GetMapping("/proposalwithvotes")
    public ApiResult proposalWithVotes(
            @RequestParam(value = "id") long id
    ) {
        return ApiResult.ok(governanceService.proposalWithVotes(id));
    }

    @GetMapping("/proposallist")
    public ApiResult proposalList() {
        return ApiResult.list(governanceService.proposalList());
    }
}
