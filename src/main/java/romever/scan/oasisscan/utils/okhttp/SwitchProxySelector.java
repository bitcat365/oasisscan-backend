package romever.scan.oasisscan.utils.okhttp;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SwitchProxySelector extends ProxySelector {

    @Override
    public List<Proxy> select(URI uri) {
        Proxy proxy = SwitchProxySelector.proxyThreadLocal.get();
        if (proxy == null) {

            proxy = Proxy.NO_PROXY;
        }

        log.debug("{} use proxy {}:{}", uri.toString(), proxy.type().name(), proxy.address());
        SwitchProxySelector.proxyThreadLocal.remove();
        return Collections.singletonList(proxy);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {

    }

    public static ThreadLocal<Proxy> proxyThreadLocal = new ThreadLocal<>();


    /**
     * proxy 模式
     */
    private static final Pattern PROXY_PATTERN = Pattern.compile("(socket|http):(.*):(.*)");

    /**
     * 工厂方法 获取Proxy
     *
     * @param proxyString meta.proxy 中的 proxy 字符串
     * @return Proxy
     */
    static Proxy getProxy(String proxyString) {
        if (proxyString == null || "".equals(proxyString)) {
            return Proxy.NO_PROXY;
        }
        Matcher matcher = PROXY_PATTERN.matcher(proxyString);
        if (matcher.matches()) {

            switch (matcher.group(1)) {
                case "socket":
                    return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(matcher.group(2), Integer.parseInt(matcher.group(3))));
                case "http":
                    return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(matcher.group(2), Integer.parseInt(matcher.group(3))));
                default:
                    return Proxy.NO_PROXY;
            }

        }
        return Proxy.NO_PROXY;

    }
}
