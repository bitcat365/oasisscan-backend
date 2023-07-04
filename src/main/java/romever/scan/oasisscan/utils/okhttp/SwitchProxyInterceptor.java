package romever.scan.oasisscan.utils.okhttp;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@Slf4j
public class SwitchProxyInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        if(chain.request().header("meta.proxy")!=null){
            String proxyHeader = chain.request().header("meta.proxy");
            log.debug("detect proxy header : {}", proxyHeader);
            SwitchProxySelector.proxyThreadLocal.set(SwitchProxySelector.getProxy(proxyHeader));
            Request newRequest = chain.request().newBuilder().removeHeader("meta.proxy").build();
            return chain.proceed(newRequest);
        }

        return chain.proceed(chain.request());
    }
}
