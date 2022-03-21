package com.tyro.oss.rabbit_amazon_bridge.forwarder;

import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.MessageTransformer;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class MessageTransformingMessageListener implements MessageListener {
    protected final MessageTransformer messageTransformer;

    public MessageTransformingMessageListener(MessageTransformer messageTransformer) {
        this.messageTransformer = messageTransformer;
    }

    public void onMessage(@NotNull Message rabbitMessage) {
        String transformedMessage = messageTransformer.transform(new String(rabbitMessage.getBody(), StandardCharsets.UTF_8));
        this.forwardMessage(toAWSMessage(transformedMessage, Map.of()));
    }

    public abstract void forwardMessage(org.springframework.messaging.Message<String> message);


    protected org.springframework.messaging.Message<String> toAWSMessage(String payload, Map<String, String> attributes) {
        MessageHeaderAccessor accessor = new MessageHeaderAccessor();
        attributes.forEach(accessor::setHeader);

        return MessageBuilder.withPayload(payload).setHeaders(accessor).build();
    }
}
