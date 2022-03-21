package com.tyro.oss.rabbit_amazon_bridge.forwarder;

import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.MessageTransformer;
import org.jetbrains.annotations.NotNull;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.Message;

public class SqsForwardingMessageListener extends MessageTransformingMessageListener {
     final String queueName;
     final QueueMessagingTemplate queueMessagingTemplate;

    public SqsForwardingMessageListener(MessageTransformer messageTransformer, String queueName, QueueMessagingTemplate queueMessagingTemplate) {
        super(messageTransformer);
        this.queueName = queueName;
        this.queueMessagingTemplate = queueMessagingTemplate;
    }

    @Override
    public void forwardMessage(Message<String> message) {
        this.queueMessagingTemplate.send(this.queueName, message);
    }
}
