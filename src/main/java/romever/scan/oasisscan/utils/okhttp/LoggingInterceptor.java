package romever.scan.oasisscan.utils.okhttp;

import okhttp3.*;
import okhttp3.internal.platform.Platform;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ihsan on 09/02/2017.
 */

public class LoggingInterceptor implements Interceptor {

    private final boolean isDebug;
    private final Builder builder;

    private LoggingInterceptor(Builder builder) {
        this.builder = builder;
        this.isDebug = builder.isDebug;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HashMap<String, String> headerMap = builder.getHeaders();
        if (headerMap.size() > 0) {
            Request.Builder requestBuilder = request.newBuilder();
            for (String key : headerMap.keySet()) {
                String value = headerMap.get(key);
                requestBuilder.addHeader(key, value);
            }
            request = requestBuilder.build();
        }

        HashMap<String, String> queryMap = builder.getHttpUrl();
        if (queryMap.size() > 0) {
            HttpUrl.Builder httpUrlBuilder = request.url().newBuilder(request.url().toString());
            for (String key : queryMap.keySet()) {
                String value = queryMap.get(key);
                httpUrlBuilder.addQueryParameter(key, value);
            }
            request = request.newBuilder().url(httpUrlBuilder.build()).build();
        }

        // if (!isDebug || builder.getLevel() == Level.NONE) {
        // return chain.proceed(request);
        // }

        final RequestBody requestBody = request.body();

        String rSubtype = null;
        if (requestBody != null && requestBody.contentType() != null) {
            rSubtype = requestBody.contentType().subtype();
        }

        // Executor executor = builder.executor;

        // if (isNotFileRequest(rSubtype)) {
        //// if (executor != null) {
        //// executor.execute(createPrintJsonRequestRunnable(builder, request));
        //// } else {
        // Printer.printJsonRequest(builder, request);
        //// }
        // } else {
        //// if (executor != null) {
        //// executor.execute(createFileRequestRunnable(builder, request));
        //// } else {
        // Printer.printFileRequest(builder, request);
        //// }
        // }

        final long st = System.nanoTime();
        final Response response = chain.proceed(request);
        final long chainMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - st);

        final List<String> segmentList = request.url().encodedPathSegments();
        final String header = response.headers().toString();
        final int code = response.code();
        final boolean isSuccessful = response.isSuccessful();
        final String message = response.message();
        final ResponseBody responseBody = response.body();
        final MediaType contentType = responseBody.contentType();

        String subtype = null;
        final ResponseBody body;

        if (contentType != null) {
            subtype = contentType.subtype();
        }

        if (isNotFileRequest(subtype)) {
            final String bodyString = Printer.getJsonString(responseBody.string());
            final String url = response.request().url().toString();

            // if (executor != null) {
            // executor.execute(createPrintJsonResponseRunnable(builder, chainMs, isSuccessful, code, header,
            // bodyString,
            // segmentList, message, url));
            // } else {
            Printer.printJsonResponse(builder, request, chainMs, isSuccessful, code, header, bodyString, segmentList,
                    message, url);
            // }
            body = ResponseBody.create(contentType, bodyString);
        } else {
            // if (executor != null) {
            // executor.execute(createFileResponseRunnable(builder, chainMs, isSuccessful, code, header, segmentList,
            // message));
            // } else {
            Printer.printFileResponse(builder, chainMs, isSuccessful, code, header, segmentList, message);
            // }
            return response;
        }

        return response.newBuilder().body(body).build();
    }

    private boolean isNotFileRequest(final String subtype) {
        return subtype != null && (subtype.contains("json") || subtype.contains("xml") || subtype.contains("plain")
                || subtype.contains("html"));
    }

    @SuppressWarnings({ "unused", "SameParameterValue" })
    public static class Builder {

        private static String TAG = "LoggingI";
        private final HashMap<String, String> headers;
        private final HashMap<String, String> queries;
        private boolean isDebug;
        private int type = Platform.INFO;
        private String requestTag;
        private String responseTag;

        // private Executor executor;

        public Builder() {
            headers = new HashMap<>();
            queries = new HashMap<>();
        }

        int getType() {
            return type;
        }

        HashMap<String, String> getHeaders() {
            return headers;
        }

        HashMap<String, String> getHttpUrl() {
            return queries;
        }

        //
        // Executor getExecutor() {
        // return executor;
        // }

        /**
         * @param name
         *            Filed
         * @param value
         *            Value
         * @return Builder Add a field with the specified value
         */
        public Builder addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /**
         * @param name
         *            Filed
         * @param value
         *            Value
         * @return Builder Add a field with the specified value
         */
        public Builder addQueryParam(String name, String value) {
            queries.put(name, value);
            return this;
        }

        /**
         * Set request and response each log tag
         *
         * @param tag
         *            general log tag
         * @return Builder
         */
        public Builder tag(String tag) {
            TAG = tag;
            return this;
        }

        /**
         * Set request log tag
         *
         * @param tag
         *            request log tag
         * @return Builder
         */
        public Builder request(String tag) {
            this.requestTag = tag;
            return this;
        }

        /**
         * Set response log tag
         *
         * @param tag
         *            response log tag
         * @return Builder
         */
        public Builder response(String tag) {
            this.responseTag = tag;
            return this;
        }

        /**
         * @param isDebug
         *            set can sending log output
         * @return Builder
         */
        public Builder loggable(boolean isDebug) {
            this.isDebug = isDebug;
            return this;
        }

        /**
         * @param type
         *            set sending log output type
         * @return Builder
         * @see Platform
         */
        public Builder log(int type) {
            this.type = type;
            return this;
        }

        public LoggingInterceptor build() {
            return new LoggingInterceptor(this);
        }
    }

}
