package com.tyro.oss.rabbit_amazon_bridge.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import io.awspring.cloud.core.region.RegionProvider;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!(test | docker-integration-test)")
public class AWSSqsConfig {

    @NotNull
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSAsync amazonSQS(
            @Autowired @NotNull AWSCredentialsProvider awsCredentialsProvider,
            @Autowired @NotNull RegionProvider regionProvider,
            @Autowired @NotNull ClientConfiguration clientConfiguration) throws Exception {
        return new AmazonSQSBufferedAsyncClient(
                AmazonSQSAsyncClientBuilder.standard()
                                           .withCredentials(awsCredentialsProvider)
                                           .withClientConfiguration(clientConfiguration)
                                           .withRegion(regionProvider.getRegion().getName())
                                           .build());
    }

    @Bean
    @NotNull
    public QueueMessagingTemplate queueMessagingTemplate(@Autowired @NotNull AmazonSQSAsync amazonSQS) {
        Intrinsics.checkNotNullParameter(amazonSQS, "amazonSQS");
        return new QueueMessagingTemplate(amazonSQS);
    }
}
