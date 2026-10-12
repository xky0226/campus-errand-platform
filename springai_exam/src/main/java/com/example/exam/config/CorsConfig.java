package com.example.exam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 任务三-4：全局跨域配置（前后端分离必备）。
 * 一次配置全局生效，不必在每个 Controller 上重复加 @CrossOrigin。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")               // 对所有接口生效
                .allowedOriginPatterns("*")      // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")             // 允许所有请求头
                .allowCredentials(true)          // 允许携带凭证
                .maxAge(3600);                   // 预检请求缓存 1 小时
    }
}
