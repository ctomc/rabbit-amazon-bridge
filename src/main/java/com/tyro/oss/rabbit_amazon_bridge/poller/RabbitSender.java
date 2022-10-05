package com.tyro.oss.rabbit_amazon_bridge.poller;

import com.tyro.oss.rabbit_amazon_bridge.generator.Bridge;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

public class RabbitSender {
    private final String exchangeName;
    private final String routingKey;
    private final AsyncRabbitTemplate asyncRabbitTemplate;

    public RabbitSender(@NotNull Bridge bridge, @NotNull AsyncRabbitTemplate asyncRabbitTemplate) {
        this.asyncRabbitTemplate = asyncRabbitTemplate;
        Bridge.RabbitToDefinition toRabbit = bridge.to().rabbit();
        this.exchangeName = toRabbit.exchange();
        this.routingKey = toRabbit.routingKey();
    }

    public void send(@NotNull String payload) {
        Message m = MessageBuilder.withBody(payload.getBytes(StandardCharsets.UTF_8)).build();
        asyncRabbitTemplate.sendAndReceive(exchangeName, routingKey, m);
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
