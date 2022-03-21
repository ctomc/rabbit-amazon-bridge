package com.tyro.oss.rabbit_amazon_bridge.config;

import com.tyro.oss.rabbit_amazon_bridge.generator.Bridge;
import com.tyro.oss.rabbit_amazon_bridge.generator.BridgeConfigFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;

@Configuration
public class BridgeConfig {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeConfig.class);

    @Bean
    List<Resource> bridgeConfigResources(
            @Value("#{'${bridge.config.location}'.split(',')}") List<String> configPaths,
            @Autowired ResourceLoader resourceLoader) {
        LOG.info("Loading bridge config for {}", configPaths);
        return configPaths.stream()
                          .map(resourceLoader::getResource)
                          .toList();
    }

    @Bean
    List<Bridge> bridges(@Autowired BridgeConfigFileParser bridgeConfigFileParser) {
        return bridgeConfigFileParser.parse();
    }

    @Bean
    List<Bridge> bridgesFromRabbit(@Autowired List<Bridge> bridges) {
        return Bridge.fromRabbit(bridges);
    }

    @Bean
    List<Bridge> bridgesFromSQS(@Autowired List<Bridge> bridges) {
        return Bridge.fromSqs(bridges);
    }
}
