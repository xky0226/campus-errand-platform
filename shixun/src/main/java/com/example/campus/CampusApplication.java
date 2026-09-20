package com.example.campus;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

/**
 * 校园帮 - 互助跑腿小程序 启动类
 */
@SpringBootApplication
// 扫描 mapper 包，这样 Mapper 接口不用每个都加 @Mapper 注解
@MapperScan("com.example.campus.mapper")
public class CampusApplication {

    private static final Logger LOG = LoggerFactory.getLogger(CampusApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CampusApplication.class);
        Environment env = app.run(args).getEnvironment();
        LOG.info("启动成功！！");
        LOG.info("地址：\thttp://127.0.0.1:{}", env.getProperty("server.port"));
    }
}
