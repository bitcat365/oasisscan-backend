package romever.scan.oasisscan.utils.okhttp;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okio.Buffer;
import org.apache.commons.lang3.StringUtils;
import romever.scan.oasisscan.utils.Mappers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * @author ihsan on 09/02/2017.
 */
@Slf4j
class Printer {

    private static final int JSON_INDENT = 3;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final String DOUBLE_SEPARATOR = LINE_SEPARATOR + LINE_SEPARATOR;

    private static final String[] OMITTED_RESPONSE = { LINE_SEPARATOR, "Omitted response body" };
    private static final String[] OMITTED_REQUEST = { LINE_SEPARATOR, "Omitted request body" };

    private static final String N = "\n";
    private static final String T = "\t";
    private static final String REQUEST_UP_LINE = "┌────── Request ────────────────────────────────────────────────────────────────────────";
    private static final String END_LINE = "└───────────────────────────────────────────────────────────────────────────────────────";
    private static final String RESPONSE_UP_LINE = "┌────── Response ───────────────────────────────────────────────────────────────────────";
    private static final String BODY_TAG = "Body:";
    private static final String URL_TAG = "URL: ";
    private static final String METHOD_TAG = "Method: @";
    private static final String HEADERS_TAG = "Headers:";
    private static final String STATUS_CODE_TAG = "Status Code: ";
    private static final String RECEIVED_TAG = "Received in: ";
    private static final String CORNER_UP = "┌ ";
    private static final String CORNER_BOTTOM = "└ ";
    private static final String CENTER_LINE = "├ ";
    private static final String DEFAULT_LINE = "│ ";

    protected Printer() {
        throw new UnsupportedOperationException();
    }

    private static boolean isEmpty(String line) {
        return StringUtils.isEmpty(line) || N.equals(line) || T.equals(line) || StringUtils.isEmpty(line.trim());
    }

    // static void printJsonRequest(LoggingInterceptor.Builder builder, Request request) {
    // StringBuffer sb = new StringBuffer();
    // String requestBody = LINE_SEPARATOR + BODY_TAG + LINE_SEPARATOR + bodyToString(request);
    // sb.append(LINE_SEPARATOR + REQUEST_UP_LINE);
    // logLines(sb, new String[]{URL_TAG + request.url()}, false);
    // logLines(sb, getRequest(request), true);
    // logLines(sb, requestBody.split(LINE_SEPARATOR), true);
    // sb.append(LINE_SEPARATOR + END_LINE);
    // log.info(sb.toString());
    // }

    static void printJsonResponse(LoggingInterceptor.Builder builder, Request request, long chainMs,
            boolean isSuccessful, int code, String headers, String bodyString, List<String> segments, String message,
            final String responseUrl) {
        StringBuffer sb = new StringBuffer();

        String requestBody = LINE_SEPARATOR + BODY_TAG + LINE_SEPARATOR + bodyToString(request);
        sb.append(LINE_SEPARATOR + REQUEST_UP_LINE);
        logLines(sb, new String[] { URL_TAG + request.url() }, false);
        logLines(sb, getRequest(request), true);
        logLines(sb, requestBody.split(LINE_SEPARATOR), true);
        sb.append(LINE_SEPARATOR + END_LINE);

        final String responseBody = LINE_SEPARATOR + BODY_TAG + LINE_SEPARATOR + getJsonString(bodyString);
        final String[] urlLine = { URL_TAG + responseUrl, N };
        final String[] response = getResponse(headers, chainMs, code, isSuccessful, segments, message);

        sb.append(LINE_SEPARATOR + RESPONSE_UP_LINE);

        logLines(sb, urlLine, true);
        logLines(sb, response, true);

        logLines(sb, responseBody.split(LINE_SEPARATOR), true);
        sb.append(END_LINE);
        log.info(sb.toString());
    }

    static void printFileRequest(LoggingInterceptor.Builder builder, Request request) {
        // String tag = builder.getTag(true);
        // if (builder.getLogger() == null)
        // log.info(REQUEST_UP_LINE);
        // logLines(new String[]{URL_TAG + request.url()}, false);
        // logLines(getRequest(request), true);
        // logLines(OMITTED_REQUEST, true);
        // log.info(END_LINE);
    }

    static void printFileResponse(LoggingInterceptor.Builder builder, long chainMs, boolean isSuccessful, int code,
            String headers, List<String> segments, String message) {
        // log.info(RESPONSE_UP_LINE);
        // logLines(getResponse(headers, chainMs, code, isSuccessful
        // , segments, message), true);
        // logLines(OMITTED_RESPONSE, true);
        // log.info(END_LINE);
    }

    private static String[] getRequest(Request request) {
        String log;
        String header = request.headers().toString();
        boolean loggableHeader = true;
        log = METHOD_TAG + request.method() + DOUBLE_SEPARATOR
                + (isEmpty(header) ? "" : loggableHeader ? HEADERS_TAG + LINE_SEPARATOR + dotHeaders(header) : "");
        return log.split(LINE_SEPARATOR);
    }

    private static String[] getResponse(String header, long tookMs, int code, boolean isSuccessful,
            List<String> segments, String message) {
        String log;
        boolean loggableHeader = true;
        String segmentString = slashSegments(segments);
        log = ((!StringUtils.isEmpty(segmentString) ? segmentString + " - " : "") + "is success : " + isSuccessful
                + " - " + RECEIVED_TAG + tookMs + "ms" + DOUBLE_SEPARATOR + STATUS_CODE_TAG + code + " / " + message
                + DOUBLE_SEPARATOR
                + (isEmpty(header) ? "" : loggableHeader ? HEADERS_TAG + LINE_SEPARATOR + dotHeaders(header) : ""));
        return log.split(LINE_SEPARATOR);
    }

    private static String slashSegments(List<String> segments) {
        StringBuilder segmentString = new StringBuilder();
        for (String segment : segments) {
            segmentString.append("/").append(segment);
        }
        return segmentString.toString();
    }

    private static String dotHeaders(String header) {
        String[] headers = header.split(LINE_SEPARATOR);
        StringBuilder builder = new StringBuilder();
        String tag = "─ ";
        if (headers.length > 1) {
            for (int i = 0; i < headers.length; i++) {
                if (i == 0) {
                    tag = CORNER_UP;
                } else if (i == headers.length - 1) {
                    tag = CORNER_BOTTOM;
                } else {
                    tag = CENTER_LINE;
                }
                builder.append(tag).append(headers[i]).append("\n");
            }
        } else {
            for (String item : headers) {
                builder.append(tag).append(item).append("\n");
            }
        }
        return builder.toString();
    }

    private static void logLines(StringBuffer sb, String[] lines, boolean withLineSize) {
        for (String line : lines) {
            int lineLength = line.length();
            int MAX_LONG_SIZE = withLineSize ? 110 : lineLength;
            for (int i = 0; i <= lineLength / MAX_LONG_SIZE; i++) {
                int start = i * MAX_LONG_SIZE;
                int end = (i + 1) * MAX_LONG_SIZE;
                end = end > line.length() ? line.length() : end;

                sb.append(LINE_SEPARATOR + DEFAULT_LINE + line.substring(start, end));

            }
        }
    }

    private static String bodyToString(final Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            if (copy.body() == null)
                return "";
            copy.body().writeTo(buffer);
            return getJsonString(buffer.readUtf8());
        } catch (final IOException e) {
            return "{\"err\": \"" + e.getMessage() + "\"}";
        }
    }

    static String getJsonString(final String msg) {
        String message;
        try {
            if (msg.startsWith("{") || msg.startsWith("{")) {
                Optional<Object> json = Mappers.parseJson(msg, Object.class);
                if (json.isPresent()) {
                    message = Mappers.prettyJson(json.get());
                } else {
                    message = msg;
                }

                // JSONObject jsonObject = new JSONObject(msg);
                // message = jsonObject.toString(JSON_INDENT);
                // } else if (msg.startsWith("[")) {
                // JSONArray jsonArray = new JSONArray(msg);
                // message = jsonArray.toString(JSON_INDENT);
            } else {
                message = msg;
            }
        } catch (Exception e) {
            message = msg;
        }
        return message;
    }
}
