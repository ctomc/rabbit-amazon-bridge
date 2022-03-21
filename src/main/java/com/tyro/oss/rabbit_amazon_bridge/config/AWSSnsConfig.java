package com.tyro.oss.rabbit_amazon_bridge.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AWSSnsConfig {

    @Bean
    @NotNull
    public AmazonSNSAsync amazonSNS(@Autowired @NotNull AWSCredentialsProvider awsCredentialsProvider,
                                    @Autowired @NotNull RegionProvider regionProvider,
                                    @Autowired @NotNull ClientConfiguration clientConfiguration) {

        return AmazonSNSAsyncClientBuilder.standard()
                                          .withCredentials(awsCredentialsProvider)
                                          .withClientConfiguration(clientConfiguration)
                                          .withRegion(regionProvider.getRegion().getName())
                                          .build();
    }

    @Bean
    @NotNull
    public NotificationMessagingTemplate topicMessagingTemplate(@Autowired @NotNull AmazonSNS amazonSNS) {
        return new NotificationMessagingTemplate(amazonSNS);
    }
}
