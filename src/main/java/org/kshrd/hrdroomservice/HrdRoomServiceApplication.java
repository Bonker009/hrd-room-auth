package org.kshrd.hrdroomservice;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HrdRoomServiceApplication {

    public static void main(String[] args) {
        Dotenv dotenv =
                Dotenv.configure().directory(".").ignoreIfMalformed().ignoreIfMissing().load();
        Map<String, Object> fromEnv = new HashMap<>();
        dotenv.entries().forEach(e -> fromEnv.put(e.getKey(), e.getValue()));

        SpringApplication app = new SpringApplication(HrdRoomServiceApplication.class);
        app.setDefaultProperties(fromEnv);
        app.run(args);
    }
}
