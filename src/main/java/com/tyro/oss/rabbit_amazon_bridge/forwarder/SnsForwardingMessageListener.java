package com.tyro.oss.rabbit_amazon_bridge.forwarder;

import com.bazaarvoice.jolt.JsonUtils;
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.MessageTransformer;
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Message;


import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class SnsForwardingMessageListener extends MessageTransformingMessageListener {

    final String topicName;
    final NotificationMessagingTemplate topicNotificationMessagingTemplate;

    public SnsForwardingMessageListener(MessageTransformer messageTransformer, String topicName, NotificationMessagingTemplate topicNotificationMessagingTemplate) {
        super(messageTransformer);
        this.topicName = topicName;
        this.topicNotificationMessagingTemplate = topicNotificationMessagingTemplate;
    }

    public void onMessage(@NotNull Message rabbitMessage) {
        String transformedMessage = messageTransformer.transform(new String(rabbitMessage.getBody(), StandardCharsets.UTF_8));
        this.forwardMessage(toAWSMessage(transformedMessage, this.generateMessageAttributes(rabbitMessage)));
    }

    public void forwardMessage(@NotNull org.springframework.messaging.Message<String> message) {
        Intrinsics.checkNotNullParameter(message, "message");
        this.topicNotificationMessagingTemplate.send(this.topicName, message);
    }

    private Map<String, String> generateMessageAttributes(Message rabbitMessage) {
        var jsonToMap = JsonUtils.jsonToMap(new String(rabbitMessage.getBody(), StandardCharsets.UTF_8));
        var attributes = new LinkedHashMap<String, String>();
        if (jsonToMap.containsKey("type")) {
            var type = (String) jsonToMap.get("type");
            attributes.put("type", type);
        }

        return attributes;
    }

}
