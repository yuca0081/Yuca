package org.yuca.yuca;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "org.yuca")
@ConfigurationPropertiesScan(basePackages = "org.yuca")
@MapperScan(basePackages = "org.yuca", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class YucaApplication {

    public static void main(String[] args) {
        SpringApplication.run(YucaApplication.class, args);
    }

}
