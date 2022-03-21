package com.tyro.oss.rabbit_amazon_bridge.forwarder;

import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.messaging.support.MessageBuilder;

public class ConnectivityTester {
    @NotNull
    private final QueueMessagingTemplate queueMessagingTemplate;
    @NotNull
    private final NotificationMessagingTemplate topicNotificationMessagingTemplate;

    public ConnectivityTester(@Autowired @NotNull QueueMessagingTemplate queueMessagingTemplate,
                              @Autowired @NotNull NotificationMessagingTemplate topicNotificationMessagingTemplate) {
        this.queueMessagingTemplate = queueMessagingTemplate;
        this.topicNotificationMessagingTemplate = topicNotificationMessagingTemplate;
    }

    @ManagedOperation
    public void sendMessageToSqs(@NotNull String queueName, @NotNull String message) {
        queueMessagingTemplate.send(queueName, MessageBuilder.withPayload(message).build());
    }

    @ManagedOperation
    public void sendMessageToSns(@NotNull String topicName, @NotNull String message) {
        topicNotificationMessagingTemplate.send(topicName, MessageBuilder.withPayload(message).build());
    }
}
