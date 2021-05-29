package romever.scan.oasisscan.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;

@Configuration
public class SwaggerConfig {

    @Bean
    public Docket demoAPI(){
        return new Docket(DocumentationType.SWAGGER_2) //采用 Swagger 2.0 规范
                .select()
                .apis(RequestHandlerSelectors.any()) //所有API接口类都生成文档
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))//方法一：不展示 Spring 自带的 error Controller
                //.apis(RequestHandlerSelectors.basePackage("org.qadoc.demo.web.controller"))//方法二：不展示 Spring 自带的 error Controller
                //.paths(Predicates.not(PathSelectors.regex("/error.*")))//方法三（3.0.0不适用）：不展示 Spring 自带的 error Controller
                .paths(PathSelectors.any())
                .build()
                //.pathMapping("/")
                //.directModelSubstitute(LocalDate.class,String.class)
                //.genericModelSubstitutes(ResponseEntity.class)
                .useDefaultResponseMessages(false)
                //.tags(new Tag("tagName","description"))
                .apiInfo(apiInfo())
                ;
    }

    //接口文档基础信息
    private ApiInfo apiInfo(){
        Contact contact = new Contact("","","");
        return new ApiInfo(
                "Oasisscan API",
                "Oasisscan API document",
                "1.0.0",
                "",
                contact,
                "",
                "",
                new ArrayList<>()
        );
    }

}
