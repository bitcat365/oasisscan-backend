package romever.scan.oasisscan.utils;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.HtmlUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Slf4j
public abstract class Texts {

    private static final String AES_ALGORITHM = "AES/CBC/NoPadding";
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final BaseEncoding BASE32 = BaseEncoding.base32().omitPadding().lowerCase();
    private static final BaseEncoding BASE64URL = BaseEncoding.base64Url().omitPadding();
    public static final String REG_HTTP_HTTPS = "^http[s]?://([-.0-9a-zA-Z]+)(?::\\d+)?/";

    public static boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static String toBigDecimal(String amount, int decimal) {
        BigDecimal realDecimal = BigDecimal.valueOf(Math.pow(10, decimal));
        BigDecimal valueDecimal = new BigDecimal(amount);
        BigDecimal realValue = valueDecimal.divide(realDecimal, decimal, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
        return realValue.toPlainString();
    }


    public static boolean filterVersion(String version, String startVersion, String endVersion) {

        if (StringUtils.isBlank(version)) {
            return false;
        }
        if (StringUtils.isBlank(startVersion) && StringUtils.isBlank(endVersion)) {
            return true;
        }

        if (StringUtils.isBlank(startVersion)) {
            if (compareVersion(version, endVersion) < 0) {
                return true;
            }
            return false;
        }
        if (StringUtils.isBlank(endVersion)) {
            if (compareVersion(version, startVersion) >= 0) {
                return true;
            }
            return false;
        }


        if (compareVersion(version, startVersion) >= 0
                && compareVersion(version, endVersion) < 0) {
            return true;
        }
        return false;
    }

    /**
     * @param version1
     * @param version2
     * @return version1 > version2 返回1，version1 < version2 返回-1
     */
    private static int compareVersion(String version1, String version2) {
        //v2.3.5.006
        version1 = version1.substring(1);
        long mainVersion1 = Texts.safeParseNumberToLong(version1.split("\\.")[0]);
        long midVersion1 = Texts.safeParseNumberToLong(version1.split("\\.")[1]);
        long minorVersion1 = Texts.safeParseNumberToLong(version1.split("\\.")[2]);
        long endVersion1 = Texts.safeParseNumberToLong(version1.split("\\.")[3]);

        version2 = version2.substring(1);
        long mainVersion2 = Texts.safeParseNumberToLong(version2.split("\\.")[0]);
        long midVersion2 = Texts.safeParseNumberToLong(version2.split("\\.")[1]);
        long minorVersion2 = Texts.safeParseNumberToLong(version2.split("\\.")[2]);
        long endVersion2 = Texts.safeParseNumberToLong(version2.split("\\.")[3]);

        if (mainVersion1 > mainVersion2) {
            return 1;
        } else if (mainVersion1 < mainVersion2) {
            return -1;
        } else {

            if (midVersion1 > midVersion2) {
                return 1;
            } else if (midVersion1 < midVersion2) {
                return -1;
            } else {

                if (minorVersion1 > minorVersion2) {
                    return 1;
                } else if (minorVersion1 < minorVersion2) {
                    return -1;
                } else {
                    if (endVersion1 > endVersion2) {
                        return 1;
                    } else if (endVersion1 < endVersion2) {
                        return -1;
                    } else {

                        return 0;
                    }
                }

            }
        }
    }


    public static double safeParseNumber(String amount) {
        if (StringUtils.isBlank(amount)) {
            return 0;
        }
        try {
            double v = Double.parseDouble(amount);
            return v;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long safeParseNumberToLong(String amount) {
        if (StringUtils.isBlank(amount)) {
            return 0;
        }
        try {
            long v = Long.parseLong(amount);
            return v;
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    public static boolean validateDomainName(String url, String validDomain) {
        if (url.startsWith("/"))
            return true; // 本域下的相对路径
        // 校验跳转域名，避免被钓鱼拦截跳转第三方
        String domain = Texts.extract(url, "^https?://([-.0-9a-zA-Z]+)(?::\\d+)?/");
        if (domain == null)
            return false;
        return (domain.equalsIgnoreCase(validDomain)); // 正式环境和开发环境
    }

    public static boolean validateDomainName(String url) {

        return validateDomainName(url, "static.hbwallet.net"); // 正式环境和开发环境
    }

    @Deprecated
    public static String md5(@NonNull String input) {
        return Hashing.md5().hashString(input, Charsets.UTF_8).toString();
    }

    @Deprecated
    public static String sha1(@NonNull String input) {
        return Hashing.sha1().hashString(input, Charsets.UTF_8).toString();
    }

    public static String sha512(@NonNull String input) {
        return Hashing.sha512().hashString(input, Charsets.UTF_8).toString();
    }

    public static String sha256(String seed) {
        return Hashing.sha256().hashString(seed, Charsets.UTF_8).toString();
    }

    public static String sha512Salt(@NonNull String input, String salt) {

        HashCode saltHash = Hashing.sha256().hashString(salt, Charsets.UTF_8);
        HashCode hashCode = Hashing.sha256().hashString(input + saltHash.toString(), Charsets.UTF_8);

        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < saltHash.toString().length(); i++) {
            // sb.append(hashCode.toString().charAt(i)).append(saltHash.toString().charAt(i));
            sb.append(saltHash.toString().charAt(i)).append(hashCode.toString().charAt(i));
        }
        return sb.toString();
    }

    public static boolean sha512SaltVerify(@NonNull String input, @NonNull String hash) {
        StringBuilder salt = new StringBuilder(64);
        StringBuilder hashCode = new StringBuilder(64);
        if (hash.length() != 128) {
            return false;
        }
        for (int i = 0; i < hash.length(); i++) {
            if (i % 2 == 1) {
                hashCode.append(hash.charAt(i));
            } else {
                salt.append(hash.charAt(i));
            }
        }

        String hashVerify = Hashing.sha256().hashString(input + salt.toString(), Charsets.UTF_8).toString();

        return hashVerify.equals(hashCode.toString());
    }

    public static String base64Encode(@NonNull byte[] bytes) {
        return BaseEncoding.base64().encode(bytes);
    }

    public static byte[] base64Decode(@NonNull String input) {
        return BaseEncoding.base64().decode(input.replace(" ", "+").replaceAll("\r?\n", ""));
    }

    public static String base32Encode(@NonNull byte[] bytes) {
        return BASE32.encode(bytes);
    }

    public static String base64UrlEncode(@NonNull byte[] bytes) {
        return BASE64URL.encode(bytes);
    }

    public static byte[] base64UrlDecode(@NonNull String input) {
        return BASE64URL.decode(input);
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    public static String urlEncode(@NonNull String input) {
        return URLEncoder.encode(input, Charsets.UTF_8.name());
    }

    @SneakyThrows(UnsupportedEncodingException.class)
    public static String urldecode(@NonNull String input) {
        return URLDecoder.decode(input, Charsets.UTF_8.name());
    }

    public static String read(InputStream is, boolean gzipped) throws IOException {
        if (gzipped)
            is = new GZIPInputStream(is);
        byte[] bytes = ByteStreams.toByteArray(is);
        return new String(bytes, Charsets.UTF_8);
    }

    public static List<String> split(String input) {
        return split(input, ",");
    }

    public static List<String> split(String input, String separator) {
        return split(input, separator, true);
    }

    public static List<String> split(String input, String separator, boolean trim) {
        if (isBlank(input))
            return Collections.emptyList();
        Splitter splitter = Splitter.on(separator);
        if (trim)
            splitter.trimResults().omitEmptyStrings();
        return Lists.newArrayList(splitter.split(input));
    }

    public static String extract(String input, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        if (matcher.find())
            return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
        return null;
    }

    public static String replace(String input, String pattern, Map<String, String> replacements) {

        return replace(input, Pattern.compile(pattern), replacements);

    }

    public static String replace(String input, Pattern pattern, Map<String, String> replacements) {
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String placeholder = matcher.group();
            if (placeholder != null) {
                String replaceStr = replacements.get(placeholder.substring(1, placeholder.length() - 1));
                input = matcher
                        .replaceFirst(org.springframework.util.StringUtils.isEmpty(replaceStr) ? "" : replaceStr);
                matcher = pattern.matcher(input);
            }
        }
        return input;

    }

    private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
            .toCharArray();
    ;

    /**
     * 产生随机字符串 *
     */
    public static final String randomNumbersAndLettersString(int length) {
        if (length < 1) {
            return null;
        }
        char[] randBuffer = new char[length];
        for (int i = 0; i < randBuffer.length; i++) {
            randBuffer[i] = numbersAndLetters[Texts.RANDOM.nextInt(61)];
        }
        return new String(randBuffer);
    }

    public static String toHex(byte[] bytes) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static final SecureRandom RANDOM = new SecureRandom();

    public static String random(int bits) {
        byte[] bytes = new byte[bits];
        RANDOM.nextBytes(bytes);
        return Hashing.goodFastHash(bits).hashBytes(bytes).toString();
    }

    public static String randomWithLength(int length) {
        String result = "";

        int n = 6;
        if (length > 0) {
            n = length;
        }
        int randInt = 0;
        for (int i = 0; i < n; i++) {
            randInt = RANDOM.nextInt(10);
            result += randInt;
        }
        return result;
    }

    public static String encodeCardNo(String no, String nonceStr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < no.length(); i++) {
            sb.append((char) (no.charAt(i) ^ nonceStr.charAt(i)));
        }
        return base64Encode(sb.toString().getBytes());

    }

    public static String decodeCardNo(String no, String nonceStr) {
        no = new String(base64Decode(no));
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < no.length(); i++) {
            sb.append((char) (no.charAt(i) ^ nonceStr.charAt(i)));
        }
        return sb.toString();

    }

    public static HashCode hash32(String input) {
        return Hashing.murmur3_32().hashString(input, Charsets.UTF_8);
    }

    public static String formatMoney(double money) {

        if (money > 100000000)
            return formatDecimal(money / 100000000, "#0.##") + "亿";
        if (money > 10000000)
            return formatDecimal(money / 10000000, "#0.##") + "千万";
        if (money > 1000000)
            return formatDecimal(money / 1000000, "#0.##") + "百万";

        return formatDecimal(money, "#0.##");
    }

    public static String formatDecimal(double decimal, int scale) {
        if (scale > 0) {
            String format = "#0." + Strings.repeat("#", scale);
            return formatDecimal(decimal, format);
        } else if (scale == 0) {
            String format = "#0";
            return formatDecimal(decimal, format);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static String formatDecimal(double decimal, String format) {

        return new DecimalFormat(format).format(decimal);
    }

    public static double formatDecimal(double decimal, int scale, RoundingMode mode) {
        return new BigDecimal(decimal).setScale(scale, mode).doubleValue();
    }

    public static String divideToStr(String value, int scale) {

        BigDecimal b1 = getBigDecimal(value, scale);
        return b1.stripTrailingZeros().toPlainString();
    }

    public static BigDecimal getBigDecimal(String wei, int scale) {
        BigDecimal bd = new BigDecimal(wei);
        bd.setScale(scale, BigDecimal.ROUND_HALF_UP);
        return bd.divide(BigDecimal.valueOf(10).pow(6), scale, BigDecimal.ROUND_HALF_UP);
    }

    public static double divideTo(String value, int scale) {
        BigDecimal b1 = getBigDecimal(value, scale);
        return b1.doubleValue();
    }

    public static String formatKiloBytes(int kiloBytes) {
        if (kiloBytes <= 0)
            return "0MB";
        if (kiloBytes < 1000)
            return kiloBytes + "KB";
        if (kiloBytes < 1000 * 1024)
            return Texts.formatDecimal(1.0 * kiloBytes / 1024, 1) + "MB";
        return Texts.formatDecimal(1.0 * kiloBytes / 1024 / 1024, 2) + "GB";
    }

    public static String stackTrace(Throwable throwable, boolean abbrev) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        if (abbrev)
            stackTrace = StringUtils.abbreviate(stackTrace, 4096);
        if (!stackTrace.endsWith("\n"))
            stackTrace += "\n";
        return HtmlUtils.htmlEscape(stackTrace);
    }

    public static String toCsv(List<String> src) {
        // return src == null ? null : String.join(", ", src.toArray(new String[0]));
        return join(src, ", ");
    }

    public static String toCsv1(List<?> beans) {
        if (beans.isEmpty())
            return "";
        List<Field> fields = Arrays.asList(beans.get(0).getClass().getDeclaredFields());
        StringBuilder sb = new StringBuilder();
        fields.forEach(field -> {
            ReflectionUtils.makeAccessible(field);
            sb.append('\"').append(field.getName()).append('\"').append(',');
        });
        sb.append("\r\n");
        beans.forEach(bean -> {
            fields.forEach(field -> {
                Object value = ReflectionUtils.getField(field, bean);
                if (value != null)
                    sb.append('\"').append(value).append('\"');
                sb.append(',');
            });
            sb.append("\r\n");
        });
        return sb.toString();
    }

    public static String join(List<String> src, String delimiter) {
        return src == null ? null : String.join(delimiter, src.toArray(new String[0]));
    }

    public static String capitaliseFirstLetter(String string) {
        if (string == null || string.length() == 0) {
            return string;
        } else {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }
    }

    public static String lowercaseFirstLetter(String string) {
        if (string == null || string.length() == 0) {
            return string;
        } else {
            return string.substring(0, 1).toLowerCase() + string.substring(1);
        }
    }

    public static final byte[] input2byte(InputStream inStream) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc = 0;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] in2b = swapStream.toByteArray();
        return in2b;
    }

    public static String zeros(int n) {
        return repeat('0', n);
    }

    public static String repeat(char value, int n) {
        return new String(new char[n]).replace("\0", String.valueOf(value));
    }

    private static byte[] pkcs7Encode(byte[] bytes) {
        int count = 32 - (bytes.length % 32);
        byte[] padding = new byte[count];
        Arrays.fill(padding, (byte) (count & 0xFF));
        return Bytes.concat(bytes, padding);
    }

    private static byte[] pkcs7Decode(byte[] bytes) {
        int count = (int) bytes[bytes.length - 1];
        count = count > 0 && count <= 32 ? count : 0;
        return Arrays.copyOfRange(bytes, 0, bytes.length - count);
    }

    public static String valueOf(Object obj) {
        return (obj == null) ? null : obj.toString();
    }

    public static String cleanXSS(String value, boolean includeUrl) {
        value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        value = value.replaceAll("%3C", "&lt;").replaceAll("%3E", "&gt;");
        value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
        value = value.replaceAll("%28", "&#40;").replaceAll("%29", "&#41;");
        value = value.replaceAll("'", "&#39;");
        value = value.replaceAll("eval\\((.*)\\)", "");
        if (includeUrl) {
            value = value.replaceAll("https://", "");
            value = value.replaceAll("http://", "");
        }

        value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
        value = value.replaceAll("script", "");
        return value;
    }

    public static String urlEndFormat(String url) {
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }


    static String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>"; //定义script的正则表达式{或<script[^>]*?>[\\s\\S]*?<\\/script> }
    static String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>"; //定义style的正则表达式{或<style[^>]*?>[\\s\\S]*?<\\/style> }
    static String regEx_html = "<[^>]+>"; //定义HTML标签的正则表达式
    static Pattern p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
    static Pattern p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
    static Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);


    public static String clearHtml(String inputString) {
        String htmlStr = inputString; //含html标签的字符串
        String textStr = "";
        try {
            htmlStr = p_script.matcher(htmlStr).replaceAll(""); //过滤script标签
            htmlStr = p_style.matcher(htmlStr).replaceAll(""); //过滤style标签
            htmlStr = p_html.matcher(htmlStr).replaceAll(""); //过滤html标签
            textStr = htmlStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textStr;//返回文本字符串
    }

    public static String sha512_256(byte[] bytes) {
        Security.addProvider(new BouncyCastleProvider());
        MessageDigest messageDigest;
        String encodestr = "";
        messageDigest = DigestUtils.getSha512_256Digest();
        messageDigest.update(bytes);
        encodestr = toHex(messageDigest.digest());
        return encodestr;
    }

    public static String numberFromBase64(String base64) {
        if (isNotBlank(base64)) {
            if (Texts.checkNumber(base64)) {
                return base64;
            }
            BigInteger b = new BigInteger(Texts.toHex(base64Decode(base64)), 16);
            return b.toString();
        }
        return null;
    }

    public static String formatDecimals(String amount, int decimal, int scale) {
        if (isBlank(amount)) {
            return "";
        }
        BigDecimal realDecimal = BigDecimal.valueOf(Math.pow(10, decimal));
        BigDecimal valueDecimal = new BigDecimal(amount);
        BigDecimal realValue = valueDecimal.divide(realDecimal, decimal, BigDecimal.ROUND_HALF_UP);
        realValue = realValue.setScale(scale, BigDecimal.ROUND_HALF_UP);
        if (scale > 4) {
            realValue = realValue.setScale(scale, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
        }
        return realValue.toPlainString();
    }

    public static String passMiddle(String str, int length) {
        return str.substring(0, length) + "..." + str.substring(str.length() - length);
    }

    public static boolean checkNumber(String value) {
        String regex = "^(-?[1-9]\\d*\\.?\\d*)|(-?0\\.\\d*[1-9])|(-?[0])|(-?[0]\\.\\d*)$";
        return value.matches(regex);
    }

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    public static boolean isBase64(String str) {
        String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
        return Pattern.matches(base64Pattern, str);
    }

    public static boolean isHex(String str) {
        if (str.startsWith("0x")) {
            str = str.replace("0x", "");
        }
        String pattern = "^[0-9A-Fa-f]+$";
        return Pattern.compile(pattern).matcher(str).matches();
    }

    public static String base64ToHex(String base64) {
        return toHex(base64Decode(base64));
    }

    public static String hexToBase64(String hex) {
        return base64Encode(hexStringToByteArray(hex));
    }

    private static final ECParameterSpec SPEC = ECNamedCurveTable.getParameterSpec("secp256k1");

    public static byte[] compressedToUncompressed(byte[] compKey) throws IOException {
        ECPoint point = SPEC.getCurve().decodePoint(compKey);
        byte[] x = point.getXCoord().getEncoded();
        byte[] y = point.getYCoord().getEncoded();
        return concat(x, y);
    }

    public static byte[] concat(byte[] a, byte[] b) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(a);
        outputStream.write(b);
        return outputStream.toByteArray();
    }

    public static byte[] slice(byte[] b, int from, int to) {
        return Arrays.copyOfRange(b, 5, 10);
    }
}
