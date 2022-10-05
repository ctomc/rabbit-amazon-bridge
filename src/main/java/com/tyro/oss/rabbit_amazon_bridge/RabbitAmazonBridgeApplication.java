package com.tyro.oss.rabbit_amazon_bridge;

import io.awspring.cloud.autoconfigure.context.ContextInstanceDataAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(exclude = {ContextInstanceDataAutoConfiguration.class})
public class RabbitAmazonBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitAmazonBridgeApplication.class, args);
    }

}
