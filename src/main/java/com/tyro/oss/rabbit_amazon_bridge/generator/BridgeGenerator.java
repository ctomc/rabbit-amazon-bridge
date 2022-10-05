package com.tyro.oss.rabbit_amazon_bridge.generator;

import com.bazaarvoice.jolt.JoltTransform;
import com.fasterxml.jackson.databind.JsonNode;
import com.tyro.oss.rabbit_amazon_bridge.forwarder.DeadletteringMessageListener;
import com.tyro.oss.rabbit_amazon_bridge.forwarder.MessageTransformingMessageListener;
import com.tyro.oss.rabbit_amazon_bridge.forwarder.SnsForwardingMessageListener;
import com.tyro.oss.rabbit_amazon_bridge.forwarder.SqsForwardingMessageListener;
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.DoNothingMessageTransformer;
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.JoltMessageTransformer;
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.MessageTransformer;
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BridgeGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeGenerator.class);
    private final RabbitCreationService rabbitCreationService;
    private final QueueMessagingTemplate queueMessagingTemplate;
    private final NotificationMessagingTemplate topicNotificationMessagingTemplate;
    private final boolean shouldRetry;

    @Autowired
    public BridgeGenerator(RabbitCreationService rabbitCreationService,
                           QueueMessagingTemplate queueMessagingTemplate,
                           NotificationMessagingTemplate topicNotificationMessagingTemplate,
                           @Value("${spring.rabbitmq.listener.simple.retry.enabled:true}") boolean shouldRetry) {

        this.rabbitCreationService = rabbitCreationService;
        this.queueMessagingTemplate = queueMessagingTemplate;
        this.topicNotificationMessagingTemplate = topicNotificationMessagingTemplate;
        this.shouldRetry = shouldRetry;
    }

    public SimpleRabbitListenerEndpoint generateFromRabbit(int index, @NotNull Bridge bridge) {
        var exchangeName = bridge.from().rabbit().exchange();
        var exchangeType = bridge.from().rabbit().exchangeType();

        var queueName = bridge.from().rabbit().queueName();
        var deadletter = bridge.from().rabbit().deadLetter();

        String deadletterPrefixed = "dead." + deadletter;
        var var10 = rabbitCreationService.createExchange(exchangeName, exchangeType, deadletterPrefixed);
        Exchange exchange = var10.component1();
        Exchange deadletterExchange = var10.component2();
        var var12 = rabbitCreationService.createQueue(queueName, exchangeName, deadletterPrefixed);
        Queue queue = var12.component1();
        Queue deadletterQueue = var12.component2();
        Bridge.FromDefinition from = bridge.from();
        rabbitCreationService.bind(queue, exchange, from.rabbit().routingKey());
        rabbitCreationService.bind(deadletterQueue, deadletterExchange, queueName);
        LOG.info("Creating bridge between exchange: " + exchangeName + '/' + queueName + " to " + this.getDestinationName(bridge));
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId("org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#" + index);
        endpoint.setQueueNames(queueName);
        endpoint.setMessageListener(this.messageListener(bridge));
        return endpoint;
    }


    private String getDestinationName(Bridge bridge) {

        return (bridge.to().sns() != null) ? bridge.to().sns().name() : bridge.to().sqs().name();
    }

    private DeadletteringMessageListener messageListener(Bridge bridge) {
        return new DeadletteringMessageListener(this.amazonSendingListener(bridge), shouldRetry);
    }

    private MessageTransformingMessageListener amazonSendingListener(Bridge bridge) {
        if (bridge.to().sns() != null) {
            return new SnsForwardingMessageListener(
                    createMessageTransformer(bridge.transformationSpecs()),
                    bridge.to().sns().name(),
                    topicNotificationMessagingTemplate

            );
        } else if
        (bridge.to().sqs() != null) {
            return new SqsForwardingMessageListener(
                    createMessageTransformer(bridge.transformationSpecs()),
                    bridge.to().sqs().name(),
                    queueMessagingTemplate

            );
        } else {
            throw new IllegalStateException();
        }

    }

    /*private MessageTransformer createMessageTransformer(List<JoltTransform> transformationSpecs) {
        return (transformationSpecs != null ? transformationSpecs.size() : 0) > 0 ? new JoltMessageTransformer(transformationSpecs) : new DoNothingMessageTransformer();
    }*/
    private MessageTransformer createMessageTransformer(List transformationSpecs) {
        //return (transformationSpecs != null ? transformationSpecs.size() : 0) > 0 ? new JoltMessageTransformer(transformationSpecs) : new DoNothingMessageTransformer();
        return new DoNothingMessageTransformer();
    }

}
