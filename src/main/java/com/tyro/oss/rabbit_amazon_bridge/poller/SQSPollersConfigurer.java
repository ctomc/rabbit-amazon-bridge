package com.tyro.oss.rabbit_amazon_bridge.poller;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.tyro.oss.rabbit_amazon_bridge.generator.Bridge;
import com.tyro.oss.rabbit_amazon_bridge.generator.RabbitCreationService;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.List;

@Configuration
@EnableScheduling
public class SQSPollersConfigurer implements SchedulingConfigurer {
    private final Logger LOG;
    @NotNull
    private final AmazonSQSAsync amazonSQS;
    @NotNull
    private final List<Bridge> bridgesFromSQS;
    @NotNull
    private final RabbitTemplate rabbitTemplate;
    @NotNull
    private final RabbitCreationService rabbitCreationService;
    @Nullable
    private final String messageIdKey;

    public SQSPollersConfigurer(@Autowired @NotNull AmazonSQSAsync amazonSQS,
                                @Autowired @NotNull List<Bridge> bridgesFromSQS,
                                @Autowired @NotNull RabbitTemplate rabbitTemplate,
                                @Autowired @NotNull RabbitCreationService rabbitCreationService,
                                @Value("${default.incoming.message.id.key:#{null}}") @Nullable String messageIdKey) {
        this.amazonSQS = amazonSQS;
        this.bridgesFromSQS = bridgesFromSQS;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitCreationService = rabbitCreationService;
        this.messageIdKey = messageIdKey;
        this.LOG = LoggerFactory.getLogger(SchedulingConfigurer.class);
    }

    @Bean
    @NotNull
    public AsyncRabbitTemplate asyncRabbitTemplate() {
        return new AsyncRabbitTemplate(rabbitTemplate);
    }

    public void configureTasks(@NotNull ScheduledTaskRegistrar taskRegistrar) {
        Intrinsics.checkNotNullParameter(taskRegistrar, "taskRegistrar");
        bridgesFromSQS
                .stream()
                .filter(Bridge::isForwardingMessagesEnabled)
                .forEach(it -> {
                    String queueName = it.from().sqs().name();
                    var queueUrl = amazonSQS.getQueueUrl(queueName).getQueueUrl();
                    String exchange = it.to().rabbit().exchange();
                    String routingKey = it.to().rabbit().routingKey();
                    String deadletter = it.to().rabbit().routingKey();
                    String deadletterQueueAndExchangeName = "dead." + deadletter;
                    rabbitCreationService.createExchange(exchange, it.to()
                                                                     .rabbit()
                                                                     .exchangeType(), deadletterQueueAndExchangeName);
                    SQSReceiver sqsReceiver = new SQSReceiver(it, amazonSQS, queueUrl);
                    RabbitSender rabbitSender = new RabbitSender(it, this.asyncRabbitTemplate());
                    SQSDispatcher sqsDispatcher = new SQSDispatcher(this.amazonSQS, sqsReceiver, rabbitSender, queueUrl, queueName, messageIdKey);
                    taskRegistrar.addFixedDelayTask(sqsDispatcher, 20L);
                    this.LOG.info("Configured bridge from SQS " + queueName + " to Rabbit " + exchange + '/' + routingKey);

                });
    }


}
