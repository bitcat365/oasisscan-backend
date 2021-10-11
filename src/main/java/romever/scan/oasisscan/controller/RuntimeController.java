package romever.scan.oasisscan.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romever.scan.oasisscan.common.ApiResult;
import romever.scan.oasisscan.service.RuntimeService;

@Slf4j
@RestController
@RequestMapping("/runtime")
public class RuntimeController {

    @Autowired
    private RuntimeService runtimeService;

    @GetMapping("/round/list")
    public ApiResult roundList(
            @RequestParam(value = "id") String id,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        return runtimeService.roundList(id, size, page);
    }

    @GetMapping("/round/info")
    public ApiResult roundInfo(
            @RequestParam(value = "id") String id,
            @RequestParam(value = "round") long round) {
        return ApiResult.ok(runtimeService.roundInfo(id, round));
    }

    @GetMapping("/list")
    public ApiResult runtimeList() {
        return ApiResult.list(runtimeService.runtimeList());
    }

    @GetMapping("/stats")
    public ApiResult runtimeStats(@RequestParam(value = "id") String id) {
        return ApiResult.list(runtimeService.runtimeStats(id));
    }
}