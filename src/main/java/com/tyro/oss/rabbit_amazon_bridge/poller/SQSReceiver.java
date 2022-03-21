package com.tyro.oss.rabbit_amazon_bridge.poller;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.tyro.oss.rabbit_amazon_bridge.generator.Bridge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SQSReceiver {
    private final String exchangeName;
    private final String routingKey;
    private final String sqsQueueName;
    private final AmazonSQSAsync amazonSQS;

    @NotNull
    private final String queueUrl;

    public SQSReceiver(@NotNull Bridge bridge,
                       @NotNull AmazonSQSAsync amazonSQS,
                       @NotNull String queueUrl) {
        this.amazonSQS = amazonSQS;
        this.queueUrl = queueUrl;
        var sqs = bridge.from().sqs();
        var toRabbit = bridge.to().rabbit();
        assert toRabbit != null;
        assert sqs != null;
        this.sqsQueueName = sqs.name();
        this.exchangeName = toRabbit.exchange();
        this.routingKey = toRabbit.routingKey();
    }

    @Nullable
    public List<Message> receiveMessage() {
        ReceiveMessageResult receiveMessageResult = this.amazonSQS
                .receiveMessage((new ReceiveMessageRequest())
                        .withWaitTimeSeconds(20)
                        .withMaxNumberOfMessages(10)
                        .withQueueUrl(this.queueUrl)
                        .withAttributeNames("All")
                        .withMessageAttributeNames("All")
                );
        return receiveMessageResult != null && !receiveMessageResult.getMessages()
                                                                    .isEmpty() ? receiveMessageResult.getMessages() : null;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getSqsQueueName() {
        return sqsQueueName;
    }

    public String getQueueUrl() {
        return queueUrl;
    }
}
