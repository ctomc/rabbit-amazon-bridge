package com.tyro.oss.rabbit_amazon_bridge.config;

import com.amazonaws.SdkBaseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.Map;

public class RabbitRetryConfig {
    @Bean(name = {"rabbitListenerContainerFactory"})
    @NotNull
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(SimpleRabbitListenerContainerFactoryConfigurer configurer,
                                                                                     ConnectionFactory connectionFactory,
                                                                                     RabbitProperties properties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        RabbitProperties.SimpleContainer simple = properties.getListener().getSimple();
        RabbitProperties.ListenerRetry retryConfig = simple.getRetry();
        if (retryConfig.isEnabled()) {
            SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(
                    retryConfig.getMaxAttempts(),
                    Map.of(
                            AmqpRejectAndDontRequeueException.class, false,
                            SdkBaseException.class, true,
                            MessageDeliveryException.class, true),
                    true);
            var retryOperationsInterceptor = RetryInterceptorBuilder.stateless()
                                                                    .retryPolicy(simpleRetryPolicy)
                                                                    .backOffOptions(retryConfig.getInitialInterval()
                                                                                               .toMillis(),
                                                                            retryConfig.getMultiplier(),
                                                                            retryConfig.getMaxInterval().toMillis())
                                                                    .recoverer(new RejectAndDontRequeueRecoverer())
                                                                    .build();
            factory.setAdviceChain(retryOperationsInterceptor);
        }

        return factory;

    }
}
