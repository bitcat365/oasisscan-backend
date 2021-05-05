package romever.scan.oasisscan.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import romever.scan.oasisscan.utils.Mappers;
import romever.scan.oasisscan.utils.Streams;

import java.util.List;
import java.util.function.Function;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public class ApiResult {

    public static final int DEFAULT_SUCCESS_STATUS = 0;
    public static final int DEFAULT_FAILURE_STATUS = 1;
    private int code;
//    @JsonInclude(Include.NON_NULL)
    private Object data;
    @JsonInclude(Include.NON_NULL)
    private String message;

    public static ApiResult list(List<?> list) {
        return ok(ImmutableMap.of("list", list));
    }

    public static ApiResult list(String key, List<?> list) {
        return ok(ImmutableMap.of(key, list));
    }

    public static ApiResult ok(String key, Object data) {
        return ok(ImmutableMap.of(key, data));
    }

    public static ApiResult ok() {
        return ok(null);
    }

    public static ApiResult ok(Object data) {
        return new ApiResult(DEFAULT_SUCCESS_STATUS, data, null);
    }

    public static ApiResult err(String message) {
        return err(DEFAULT_FAILURE_STATUS, message);
    }

    public static ApiResult err(int status, String message) {
        return new ApiResult(status, null, message);
    }

    public static ApiResult err(int status, Object data, String message) {
        return new ApiResult(status, data, message);
    }

    /**
     * 分页结果
     *
     * @param list
     *            集合
     * @param page
     *            页码
     * @param pageSize
     *            页大小
     * @param totalSize
     *            总数
     * @return
     */
    public static ApiResult page(List<?> list, int page, int pageSize, long totalSize) {
        return ok(Page.of(list, page, pageSize, totalSize));
    }


    @Getter
    @Setter
    public static class Page<T> {
        private List<T> list;
        private int page;
        private int size;
        private int maxPage;
        private long totalSize;
        private @JsonInclude(Include.NON_NULL) Object condition;

        public <R> Page<R> map(Function<T, R> mapper) {
            Page<R> result = new Page<R>();
            result.setList(Streams.map(list, mapper));
            result.setPage(page);
            result.setSize(size);
            result.setMaxPage(maxPage);
            result.setTotalSize(totalSize);
            return result;
        }

        public static <T> Page<T> of(org.springframework.data.domain.Page<T> page, Object condition) {
            Page<T> result = new Page<T>();
            result.setList(page.getContent());
            result.setPage(page.getNumber());
            result.setSize(page.getSize());
            result.setMaxPage(page.getTotalPages());
            result.setTotalSize(page.getTotalElements());
            result.setCondition(condition);
            return result;
        }

        public static <T> Page<T> of(JsonNode json, Class<T> clazz) {
            Page<T> result = new Page<T>();
            result.setList(Streams.map(json.path("list"), e -> Mappers.parseJson(e, clazz).orElse(null)));
            result.setPage(json.path("page").asInt());
            result.setSize(json.path("size").asInt());
            result.setMaxPage(json.path("maxPage").asInt());
            result.setTotalSize(json.path("totalSize").asLong());
            return result;
        }

        /**
         * 分页
         *
         * @param list
         *            列表
         * @param page
         *            页码
         * @param pageSize
         *            页大小
         * @param totalSize
         *            总数
         * @param condition
         *            条件
         * @param <T>
         * @return
         */
        public static <T> Page<T> of(List<T> list, int page, int pageSize, long totalSize, Object condition) {
            Page<T> result = new Page<>();
            result.setList(list);
            result.setPage(page);
            result.setSize(pageSize);
            result.setMaxPage((int) (totalSize % pageSize == 0 ? totalSize / pageSize : (totalSize / pageSize + 1)));
            result.setTotalSize(totalSize);
            result.setCondition(condition);
            return result;
        }

        /**
         * 分页
         *
         * @param list
         *            列表
         * @param page
         *            页码
         * @param pageSize
         *            页大小
         * @param totalSize
         *            总数
         * @param <T>
         * @return
         */
        public static <T> Page<T> of(List<T> list, int page, int pageSize, long totalSize) {
            return of(list, page, pageSize, totalSize, null);
        }
    }

}
