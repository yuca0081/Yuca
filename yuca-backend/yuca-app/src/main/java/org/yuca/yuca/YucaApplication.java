package org.yuca.yuca;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "org.yuca")
@ConfigurationPropertiesScan(basePackages = "org.yuca")
@MapperScan({
    "org.yuca.user.mapper",
    "org.yuca.infrastructure.storage.mapper",
    "org.yuca.knowledge.mapper",
    "org.yuca.note.mapper",
    "org.yuca.assistant.mapper",
    "org.yuca.ai.mapper"
})
public class YucaApplication {

    public static void main(String[] args) {
        SpringApplication.run(YucaApplication.class, args);
    }

}
