package com.tyro.oss.rabbit_amazon_bridge.config;

import com.amazonaws.ClientConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AWSClientConfig {

    @Bean
    public ClientConfiguration proxiedClientConfiguration(
            @Value("${aws.proxy.host:#{null}}") String proxyHost,
            @Value("${aws.proxy.port:#{null}}") String proxyPort
    ) {
        var clientConfiguration = new ClientConfiguration();
        if (proxyHost != null && proxyPort != null) {
            clientConfiguration
                    .withProxyHost(proxyHost)
                    .withProxyPort(Integer.parseInt(proxyPort));
        }
        return clientConfiguration;
    }
}
