package romever.scan.oasisscan.common;

import ch.qos.logback.access.servlet.TeeFilter;
import ch.qos.logback.access.tomcat.LogbackValve;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

/**
 * 配置fastjson
 *
 * @author beykery
 */
@Configuration
public class WebMvcConfigurer implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer,
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    }

    @Bean
    public FilterRegistrationBean teeFilterRegistration( ) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new TeeFilter( ));
        registration.addUrlPatterns("/*");
        return registration;
    }
    @Bean
    public FilterRegistrationBean requestTracerFilterRegistration( ) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RequestTracer( ));
        registration.addUrlPatterns("/*");
        return registration;
    }

//    @Bean
//    public FilterRegistrationBean teeFilterRegistration() {
//        String[] excludePaths = {"/api"};
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        registration.setFilter(new AccessLogFilter(excludePaths));
//        registration.addUrlPatterns("/*");
//        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
//        return registration;
//    }

    @Bean
    public ObjectMapper customJackson(Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer) {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        if (factory instanceof TomcatServletWebServerFactory) {
            LogbackValve valve = new LogbackValve();
            valve.setFilename("logback-access.xml");
            valve.setQuiet(true);
            ((TomcatServletWebServerFactory) factory).addEngineValves(valve);

        }
    }



}
