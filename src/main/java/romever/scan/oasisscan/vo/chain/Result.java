package romever.scan.oasisscan.vo.chain;

import lombok.Data;

@Data
public class Result<T> {
    private T result;
    private String error;
}
