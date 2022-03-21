package com.tyro.oss.rabbit_amazon_bridge.poller;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class SQSMessageConverter {
    private final ObjectMapper jsonParser = new ObjectMapper();

    @NotNull
    public final String convert(@NotNull Message message, @NotNull String prefix, @Nullable String messageIdKey) throws JsonProcessingException {
        var incomingMessageId = message.getMessageId();
        var bodyJsonObject = asJson(message.getBody());
        var payload = extractPayload(bodyJsonObject);

        ObjectNode jsonPayload;
        if (payload.isContainerNode()) {
            jsonPayload = (ObjectNode) payload;
        } else {
            jsonPayload = asJson(payload.textValue());
        }
        if (messageIdKey == null) {
            return jsonPayload.toString();
        } else {
            return payloadWithMessageId(jsonPayload, messageIdKey, prefix, incomingMessageId);
        }

    }

    private String payloadWithMessageId(ObjectNode jsonPayload, String messageIdKey, String prefix, String incomingMessageId) {
        jsonPayload.put(messageIdKey, prefix + '/' + incomingMessageId);
        return jsonPayload.toString();
    }

    private JsonNode extractPayload(JsonNode jsonNode) {
        return this.isFromSNS(jsonNode) ? jsonNode.get("Message") : jsonNode;
    }

    private ObjectNode asJson(String json) throws JsonProcessingException {
        return (ObjectNode) this.jsonParser.readTree(json);

    }

    private boolean isFromSNS(JsonNode $this$isFromSNS) {
        return $this$isFromSNS.has("Message");
    }
}
