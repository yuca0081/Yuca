package org.yuca.yuca;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.yuca")
@MapperScan({
    "org.yuca.user.mapper",
    "org.yuca.infrastructure.storage.mapper",
    "org.yuca.knowledge.mapper",
    "org.yuca.note.mapper",
    "org.yuca.assistant.mapper"
})
public class YucaApplication {

    public static void main(String[] args) {
        SpringApplication.run(YucaApplication.class, args);
    }

}
