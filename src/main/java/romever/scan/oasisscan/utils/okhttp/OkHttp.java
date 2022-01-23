package romever.scan.oasisscan.utils.okhttp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.platform.Platform;
import org.springframework.util.StringUtils;
import romever.scan.oasisscan.utils.Mappers;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class OkHttp {
    private static final MediaType MEDIA_TYPE_JSON_UTF8 = MediaType
            .parse(com.google.common.net.MediaType.JSON_UTF_8.toString());
    private static final MediaType MEDIA_TYPE_XML_UTF8 = MediaType
            .parse(com.google.common.net.MediaType.XML_UTF_8.toString());
    private static final MediaType MEDIA_TYPE_FORM_DATA = MediaType
            .parse(com.google.common.net.MediaType.FORM_DATA.toString());

    private Request request;
    private Request.Builder builder = new Request.Builder();
    private FormBody.Builder formBody;
    private RequestBody requestBody;
    private String url;
    private String credential;
    private boolean post = false;
    private boolean put = false;
    private Multimap<String, Object> queries;
    private Multimap<String, Object> params;
    private Multimap<String, Object> headers = LinkedListMultimap.create();
    private Headers responseHeaders;
    private Response response;

    private static final int CONN_TIMEOUT = 120;
    private static final int WRITE_TIMEOUT = 120;
    private static final int READ_TIMEOUT = 120;
    public static final int MAX_IDLE_CONNECTIONS = 50;

    //    private static OkHttpClient client = getOkHttpClient(CONN_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT);
    private boolean del = false;
    private OkHttpClient client;

    private static OkHttpClient httpsClient = getOkHttpsClient(CONN_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT, MAX_IDLE_CONNECTIONS);
    private static OkHttpClient httpClient = getUnsafeOkHttpClient(CONN_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT, MAX_IDLE_CONNECTIONS, 60 * 30);

    public static OkHttp of(String url) {
        return of(url, CONN_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT, false);
    }

    public static OkHttp of(String url, boolean unsafe) {
        return of(url, CONN_TIMEOUT, WRITE_TIMEOUT, READ_TIMEOUT, unsafe);
    }

    public static OkHttp of(String url, int connTimeout, int writeTimeout, int readTimeout) {
        return of(url, connTimeout, writeTimeout, readTimeout, false);
    }

    public static OkHttp of(String url, int connTimeout, int writeTimeout, int readTimeout, boolean unsafe) {
        OkHttp okHttp = new OkHttp();
        String userInfo = "";

        try {
            URL url1 = new URL(url);
            userInfo = url1.getUserInfo();
            if (!StringUtils.isEmpty(userInfo)) {
                url = url1.getProtocol() + "://" + url1.getHost() + ":" + url1.getPort();
                String path = url1.getPath();
                if (!StringUtils.isEmpty(path)) {
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    url = url + path;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        okHttp.setUrl(url);
        if (!StringUtils.isEmpty(userInfo)) {

            String[] split = userInfo.split(":");

            okHttp.authenticate(split[0], split[1]);

        }

        if (unsafe) {
            okHttp.setClient(httpClient);
        } else {
            okHttp.setClient(httpsClient);
        }

        // OkHttpClient client = getOkHttpClient(connTimeout, writeTimeout, readTimeout);
        // okHttp.setClient(client);
        return okHttp;
    }

    private static String getUserInfo(String url) {
        try {
            URL url1 = new URL(url);
            return url1.getUserInfo();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OkHttpClient getOkHttpsClient(int connTimeout, int writeTimeout, int readTimeout, int maxIdleConnections) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().retryOnConnectionFailure(true)
                .connectTimeout(connTimeout, TimeUnit.SECONDS).writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS).connectionPool(new ConnectionPool(maxIdleConnections, 60 * 30, TimeUnit.SECONDS))
                .addInterceptor(new LoggingInterceptor.Builder().loggable(true).log(Platform.INFO).request("OkHttp")
                        .response("OkHttp")
                        // .addQueryParam("query", "0")
                        // .executor(Executors.newSingleThreadExecutor())
                        .build());
        OkHttpClient okHttpClient = builder.build();
        // new RetryAndFollowUpInterceptor(okHttpClient,false);
        return okHttpClient;
    }

    /**
     * 使用POST请求
     *
     * @return
     */
    public OkHttp post() {
        this.post = true;
        return this;
    }

    public OkHttp get() {
        this.post = false;
        return this;
    }

    public OkHttp del() {
        this.del = true;
        return this;
    }

    public OkHttp authenticate(String u, String p) {
        credential = Credentials.basic(u, p);
        header("Authorization", credential);
        return this;
    }

    /**
     * 设置query参数
     *
     * @param key
     * @param value
     * @return
     */
    public OkHttp query(String key, Object value) {
        if (key == null || value == null) {
            return this;
        }
        if (this.queries == null) {
            this.queries = LinkedListMultimap.create();
        }
        this.queries.put(key, String.valueOf(value));
        return this;
    }

    /**
     * 设置query参数
     *
     * @param queries
     * @return
     */
    public OkHttp queries(Map<String, Object> queries) {
        if (queries != null && queries.size() > 0) {
            this.queries = Multimaps.forMap(queries);
        }
        return this;
    }

    /**
     * 设置header参数
     *
     * @param key
     * @param value
     * @return
     */
    public OkHttp header(String key, Object value) {
        if (key == null || value == null) {
            return this;
        }
        // if (this.headers == null) {
        // this.headers = LinkedListMultimap.create();
        // }
        this.headers.put(key, String.valueOf(value));
        return this;
    }

    /**
     * ua
     *
     * @param ua
     */
    public void userAgent(String ua) {
        this.header("User-Agent", ua);
    }

    /**
     * Set-Host
     *
     * @param host
     */
    public void setHost(String host) {
        this.header("Set-Host", host);
    }

    /**
     * 设置header参数
     *
     * @return
     */
    public OkHttp headers(Map<String, Object> headers) {
        if (headers != null && headers.size() > 0) {
            this.headers = Multimaps.forMap(headers);
        }
        return this;
    }

    public OkHttp form(String key, Object value) {
        if (key == null || value == null) {
            return this;
        }
        if (this.formBody == null) {
            this.formBody = new FormBody.Builder();
        }
        this.formBody.add(key, String.valueOf(value));
        this.post = true;
        header("Content-Type", "application/x-www-form-urlencoded");

        return this;
    }

    public OkHttp form(Map<String, String> headers) {
        if (this.formBody == null) {
            this.formBody = new FormBody.Builder();
        }

        if (headers != null && headers.size() > 0) {
            headers.forEach((k, v) -> this.formBody.add(k, v));

        }
        this.post = true;
        header("Content-Type", "application/x-www-form-urlencoded");

        return this;
    }

    /**
     * 设置json请求体
     *
     * @param json
     * @return
     */
    public OkHttp json(String json) {
        requestBody = RequestBody.create(MEDIA_TYPE_JSON_UTF8, json);
        this.post = true;
        header("Content-Type", "application/json");
        return this;
    }

    public OkHttp json(Object obj) {
        requestBody = RequestBody.create(MEDIA_TYPE_JSON_UTF8, Mappers.json(obj));
        this.post = true;
        header("Content-Type", "application/json");
        return this;
    }

    public OkHttp submitFrom() {
        if (this.formBody == null) {
            this.formBody = new FormBody.Builder();
        }
        this.post = true;
        header("Content-Type", "application/x-www-form-urlencoded");
        return this;
    }

    public <T> T exec(TypeReference<T> typeReference) throws IOException {
        String content = exec();
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        return Mappers.parseJson(content, typeReference);

    }

    public <T> T exec(TypeReference<T> typeReference, boolean fail) throws IOException {
        String content = exec(fail);
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        return Mappers.parseJson(content, typeReference);

    }

    /**
     * 请求并获取
     *
     * @param c
     * @param <T>
     * @return
     */
    public <T> T exec(Class<T> c) throws IOException {
        String content = exec();
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        Optional<T> op = Mappers.parseJson(content, c);
        return op.get();
    }

    public <T> T exec(Class<T> c, boolean fail) throws IOException {
        String content = exec(fail);
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        Optional<T> op = Mappers.parseJson(content, c);
        return op.get();
    }

    /**
     * 请求并返回字节内容
     *
     * @return
     */
    public byte[] bytes() {
        perpare();
        request = builder.build();
        Call call = client.newCall(request);
        try (Response response = call.execute()) {

            if (response.body() != null) {
                return response.body().bytes();
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String exec() throws IOException {
        perpare();

        request = builder.build();

        return execute(false);
    }

    public OkHttpResponse execRaw() {
        perpare();

        request = builder.build();
        Call call = client.newCall(request);
        try (Response response = call.execute()) {

            return new OkHttpResponse(response.isSuccessful(), response.body() != null ? response.body().string() : "");
        } catch (IOException e) {
            e.printStackTrace();
            return new OkHttpResponse(false, e.getMessage());
        }

    }

    public String exec(boolean fail) throws IOException {
        perpare();

        request = builder.build();

        return execute(fail);
    }

    public void asyncExec(Callback callback) {
        perpare();

        request = builder.build();

        execute(callback);
    }

    private void perpare() {
        if (queries != null) {
            String query = Joiner.on("&").withKeyValueSeparator("=").join(queries.entries());
            this.url = this.url + "?" + query;

        }
        builder.url(this.url);

        if (!headers.isEmpty()) {

            headers.entries().stream().forEach(e -> builder.addHeader(e.getKey(), String.valueOf(e.getValue())));
            if (!StringUtils.isEmpty(credential) && !headers.containsKey("Authorization")) {
                builder.addHeader("Authorization", credential);
            }
        }

        if (post) {
            if (!StringUtils.isEmpty(requestBody)) {
                builder.post(requestBody);
            } else if (formBody != null) {
                builder.post(formBody.build());
            }
        }
        if (del) {
            if (!StringUtils.isEmpty(requestBody)) {
                builder.delete(requestBody);
            } else {
                builder.delete();
            }
        }
    }

    private String execute(boolean fail) throws IOException {
        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            setResponse(response);
//            if ((response.isSuccessful() || fail) && response.body() != null) {
            if (response.body() != null) {
                if (response.headers() != null) {
                    setResponseHeaders(response.headers());
                }
                return response.body().string();
            } else {
                return null;
            }
        } catch (IOException e) {
            log.error("request {} {} {}", request.url().toString(), request.headers().toString(), request.body() !=
                    null ? request.body().toString() : null, e);
            throw e;
        }
//        return "";
    }

    private void execute(Callback callback) {
        Call call = client.newCall(request);
        call.enqueue(callback);

    }

    private static OkHttpClient getUnsafeOkHttpClient(int connTimeout, int writeTimeout, int readTimeout, int maxIdleConnections, int keepAliveDuration) {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

//            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            OkHttpClient.Builder builder = new OkHttpClient.Builder().retryOnConnectionFailure(true)
                    .connectTimeout(connTimeout, TimeUnit.SECONDS).writeTimeout(writeTimeout, TimeUnit.SECONDS)
                    .readTimeout(readTimeout, TimeUnit.SECONDS).connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS))
                    .addInterceptor(new LoggingInterceptor.Builder().loggable(true).log(Platform.INFO).request("OkHttp")
                            .response("OkHttp")
                            // .addQueryParam("query", "0")
                            // .executor(Executors.newSingleThreadExecutor())
                            .build());
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
