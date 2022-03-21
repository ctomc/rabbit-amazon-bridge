package com.tyro.oss.rabbit_amazon_bridge.generator;

import com.bazaarvoice.jolt.JoltTransform;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public record Bridge(FromDefinition from,
                     List<JoltTransform> transformationSpecs,
                     ToDefinition to,
                     Boolean shouldForwardMessages,
                     String description) {


    @JsonIgnore
    public boolean isForwardingMessagesEnabled() {
        return shouldForwardMessages != null && shouldForwardMessages;
    }

    public record FromDefinition(RabbitFromDefinition rabbit, SqsDefinition sqs) {
    }

    public record ToDefinition(SnsDefinition sns, SqsDefinition sqs, RabbitToDefinition rabbit) {

    }

    public record RabbitFromDefinition(
            String exchange,
            String exchangeType,/* = "topic"*/
            String queueName,
            String routingKey,
            String deadLetter /*"dead-letter"*/) {
        public RabbitFromDefinition(
                String exchange,
                String queueName,
                String routingKey
        ) {
            this(exchange, "topic", queueName, routingKey, "dead-letter");
        }
    }

    public record RabbitToDefinition(
            String exchange,
            String exchangeType,/*= "topic"*/
            String routingKey,
            String deadLetter/*"dead-letter"*/) {
        public RabbitToDefinition(String exchange, String routingKey) {
            this(exchange, "topic", routingKey, "dead-letter");
        }
    }

    public record SnsDefinition(String name) {
    }

    public record SqsDefinition(String name) {
    }


    public static List<Bridge> fromRabbit(List<Bridge> bridges) {
        return bridges.stream().filter(bridge -> bridge.from.rabbit != null).toList();
    }

    public static List<Bridge> fromSqs(List<Bridge> bridges) {
        return bridges.stream().filter(bridge -> bridge.from.sqs != null).toList();
    }
}



