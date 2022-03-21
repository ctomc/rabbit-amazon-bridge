package com.tyro.oss.rabbit_amazon_bridge.generator;

import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RabbitCreationService {
    @NotNull
    private final AmqpAdmin amqpAdmin;

    public RabbitCreationService(@Autowired @NotNull AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    @NotNull
    public Pair<Exchange,Exchange> createExchange(@NotNull String exchange, @NotNull String exchangeType, @NotNull String deadletterExchangeName) {
        CustomExchange customExchange = new CustomExchange(exchange, exchangeType, true, false);
        TopicExchange deadletterTopicExchange = new TopicExchange(deadletterExchangeName, true, false);
        amqpAdmin.declareExchange(customExchange);
        amqpAdmin.declareExchange(deadletterTopicExchange);
        return new Pair<>(customExchange, deadletterTopicExchange);
    }

    @NotNull
    public Pair<Queue,Queue> createQueue(@NotNull String queueName, @NotNull String exchange, @NotNull String deadletterQueueAndExchangeName) {
        var args = Map.<String,Object>of(
                "x-dead-letter-exchange", deadletterQueueAndExchangeName,
                "x-dead-letter-routing-key" , "dead.$queueName"
        );
        Queue queue = new Queue(queueName, true, false, false, args);
        Queue deadletterQueue = new Queue(deadletterQueueAndExchangeName, true, false, false, null);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareQueue(deadletterQueue);
        return new Pair<>(queue, deadletterQueue);
    }

    public void bind(@NotNull Queue queue, @NotNull Exchange exchange, @NotNull String routingKey) {
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs());
    }
}
