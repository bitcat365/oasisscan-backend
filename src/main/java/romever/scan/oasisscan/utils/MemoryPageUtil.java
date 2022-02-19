package romever.scan.oasisscan.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MemoryPageUtil {
    public static <T> List<T> pageLimit(List<T> dataList, long pageNum, long pageSize) {
        if (pageNum < 1) {
            return Lists.newArrayList();
        }
        return dataList.stream().skip((pageNum - 1) * pageSize).limit(pageSize).collect(Collectors.toList());
    }
}
