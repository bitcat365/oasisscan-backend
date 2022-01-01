package romever.scan.oasisscan.db;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author li
 */
@Slf4j
public class JestDao {

    public static IndexResponse index
            (RestHighLevelClient client, String index, Map<String, Object> source, String id) throws IOException {
        IndexRequest request = new IndexRequest(index).id(id).source(source);
        return client.index(request, RequestOptions.DEFAULT);
    }

    public static BulkResponse indexBulk
            (RestHighLevelClient client, String index, Map<String, Map<String, Object>> sourceMap) throws IOException {
        BulkRequest request = new BulkRequest();
        for (Map.Entry<String, Map<String, Object>> entry : sourceMap.entrySet()) {
            String id = entry.getKey();
            Map<String, Object> source = entry.getValue();
            request.add(new IndexRequest(index).id(id).source(source));
        }
        return client.bulk(request, RequestOptions.DEFAULT);
    }

    public static SearchResponse search(
            RestHighLevelClient client, String index, SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(searchSourceBuilder);
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }

    public static SearchResponse searchFromIndices(
            RestHighLevelClient client, String[] indices, SearchSourceBuilder searchSourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indices);
        searchRequest.source(searchSourceBuilder);
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }

    public static CountResponse count(
            RestHighLevelClient client, String index, QueryBuilder queryBuilder) throws IOException {
        CountRequest countRequest = new CountRequest(index);
        countRequest.query(queryBuilder);
        return client.count(countRequest, RequestOptions.DEFAULT);
    }

    public static GetResponse get(
            RestHighLevelClient client, String index, String id) throws IOException {
        GetRequest getRequest = new GetRequest(index, id);
        return client.get(getRequest, RequestOptions.DEFAULT);
    }

    public static MultiGetResponse multiGet(RestHighLevelClient client, String index, List<String> ids) throws IOException {
        MultiGetRequest request = new MultiGetRequest();
        for (String id : ids) {
            request.add(new MultiGetRequest.Item(index, id));
        }
        return client.mget(request, RequestOptions.DEFAULT);
    }
}
