package com.tyro.oss.rabbit_amazon_bridge.config;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test | docker-integration-test")
public class AWSSqsTestConfig {
    @Value("${aws.sqs.endpoint.url}")
    public String sqsEndpointUrl;
    @Value("${aws.sqs.aws.region}")
    public String sqsRegion;

    @Bean(
            destroyMethod = "shutdown"
    )
    @NotNull
    public AmazonSQSAsync amazonSQS() throws Exception {
        return new AmazonSQSBufferedAsyncClient(
                AmazonSQSAsyncClientBuilder.standard()
                                           .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
                                           .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(sqsEndpointUrl, sqsRegion))
                                           .withClientConfiguration(new ClientConfiguration())
                                           .build());
    }

    @Bean
    @NotNull
    public QueueMessagingTemplate queueMessagingTemplate(@Autowired @NotNull AmazonSQSAsync amazonSQS) {
        Intrinsics.checkNotNullParameter(amazonSQS, "amazonSQS");
        return new QueueMessagingTemplate(amazonSQS);
    }
}
