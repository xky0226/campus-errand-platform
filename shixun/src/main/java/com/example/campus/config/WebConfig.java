package com.example.campus.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    @Value("${campus.upload.dir:uploads}")
    private String uploadDir;

    @Value("${campus.upload.url-prefix:/files}")
    private String urlPrefix;

    //添加拦截器【注册】
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**");
    }

    /**
     * 把上传目录映射成静态资源。
     *
     * 做这个是因为申诉证据图和学生证照片上传后要能直接在页面上打开：
     * FileController 存盘返回的是 /files/2026/10/15/xxx.png 这种相对路径，
     * 这里把 /files/** 指到磁盘目录，浏览器就能直接访问到那张图。
     *
     * JwtInterceptor 里已经放行了 /files/ 开头的 GET 请求（只放行 GET，
     * 上传接口是 POST，仍然要登录），所以这里不用再单独配白名单。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 一定要转成绝对路径并统一分隔符：Windows 上直接拼 "uploads" 得到的
        // file location 不是合法的 URI，Spring 会找不到目录。
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        // 结尾斜杠必须自己补：Path.toUri() 只有在「这个路径此刻真实存在且是个目录」
        // 时才补斜杠，而 uploads/ 是第一次上传时才 createDirectories 出来的 ——
        // 启动那一刻它还不存在，于是拿到的是 ".../uploads" 少一个斜杠。
        // Spring 拼资源位置是纯字符串相接，少了斜杠就变成 ".../uploads2026/10/15/x.png"，
        // 永远找不到文件（表现为 404 又被 /error 转成 401）。这里无条件补上。
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler(urlPrefix + "/**")
                .addResourceLocations(location);
    }
}
