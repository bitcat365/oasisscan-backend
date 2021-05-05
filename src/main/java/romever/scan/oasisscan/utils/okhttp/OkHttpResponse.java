package romever.scan.oasisscan.utils.okhttp;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import romever.scan.oasisscan.utils.Mappers;

import java.util.Optional;

@Data
@AllArgsConstructor
public class OkHttpResponse {
    private boolean successful;
    private String body;

    public <T> T of(Class<T> c) {
        if (!successful) {
            return null;
        }
        if (StringUtils.isBlank(body)) {
            return null;
        }

        Optional<T> op = Mappers.parseJson(body, c);
        return op.get();
    }

    public <T> T of(TypeReference<T> typeReference) {
        if (!successful) {
            return null;
        }
        if (StringUtils.isBlank(body)) {
            return null;
        }

        return Mappers.parseJson(body, typeReference);
    }

}
