package romever.scan.oasisscan.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.service.ScanValidatorService;
import romever.scan.oasisscan.service.ValidatorService;
import romever.scan.oasisscan.vo.SortEnum;

@Slf4j
@RestController
@RequestMapping("/validator")
public class ValidatorController {

    @Autowired
    private ValidatorService validatorService;

    @Autowired
    private ScanValidatorService scanValidatorService;

    @GetMapping("/network")
    public ApiResult network() {
        return ApiResult.ok(validatorService.networkInfo());
    }

    @GetMapping("/list")
    public ApiResult validators(
            @RequestParam(value = "orderBy", required = false, defaultValue = "escrow") String orderBy,
            @RequestParam(value = "sort", required = false, defaultValue = "desc") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "300") int pageSize) {
        SortEnum sortEnum = SortEnum.getEnumByCode(orderBy);
        return ApiResult.ok(validatorService.validators(sortEnum, sort, page, pageSize));
    }

    @GetMapping("/stats")
    public ApiResult validatorStat(
            @RequestParam(value = "entityId", required = false) String entityId,
            @RequestParam(value = "address", required = false) String address) {
        return ApiResult.ok(validatorService.validatorStats(entityId, address));
    }

    @GetMapping("/info")
    public ApiResult validatorInfo(
            @RequestParam(value = "entityId", required = false) String entityId,
            @RequestParam(value = "address", required = false) String address) {
        return ApiResult.ok(validatorService.validatorInfo(entityId, address));
    }

    @GetMapping("/delegators")
    public ApiResult delegators(
            @RequestParam(value = "size", required = false, defaultValue = "5") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "validator", required = false) String validator,
            @RequestParam(value = "address", required = false) String address) {
        return validatorService.delegators(validator, address, page, size);
    }

    @GetMapping("/escrowstats")
    public ApiResult escrowStats(@RequestParam("address") String address) {
        return ApiResult.list(validatorService.escrowStats(address));
    }
}
