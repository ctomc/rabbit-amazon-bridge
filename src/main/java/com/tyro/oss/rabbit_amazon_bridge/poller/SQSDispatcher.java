package com.tyro.oss.rabbit_amazon_bridge.poller;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQSDispatcher implements Runnable {
    private final AmazonSQSAsync amazonSQS;
    private final SQSReceiver sqsReceiver;
    private final RabbitSender rabbitSender;
    private final String queueUrl;
    private final String queueName;
    private final String messageIdKey;
    private static final SQSMessageConverter messageConverter = new SQSMessageConverter();
    private static final Logger LOG = LoggerFactory.getLogger(SQSDispatcher.class);

    public SQSDispatcher(@NotNull AmazonSQSAsync amazonSQS,
                         @NotNull SQSReceiver sqsReceiver,
                         @NotNull RabbitSender rabbitSender,
                         @NotNull String queueUrl,
                         @NotNull String queueName,
                         @Nullable String messageIdKey) {
        this.amazonSQS = amazonSQS;
        this.sqsReceiver = sqsReceiver;
        this.rabbitSender = rabbitSender;
        this.queueUrl = queueUrl;
        this.queueName = queueName;
        this.messageIdKey = messageIdKey;
    }


    public void run() {
        var messages = this.sqsReceiver.receiveMessage();
        if (messages != null) {
            LOG.info("Thread {} Received {} messages from {}", Thread.currentThread()
                                                                     .getName(), messages.size(), queueName);
            messages.forEach(it -> {
                String receiptHandle = it.getReceiptHandle();
                try {
                    rabbitSender.send(messageConverter.convert(it, this.queueName, this.messageIdKey));
                    amazonSQS.deleteMessageAsync(this.queueUrl, receiptHandle);
                } catch (JsonProcessingException e) {
                    LOG.warn("Received non JSON message - discarding message.", e);
                    amazonSQS.deleteMessageAsync(this.queueUrl, receiptHandle);
                } catch (Exception e) {
                    amazonSQS.changeMessageVisibilityAsync(this.queueUrl, receiptHandle, 0);
                    throw e;
                }
            });

        }
    }

    public RabbitSender getRabbitSender() {
        return rabbitSender;
    }

    public AmazonSQSAsync getAmazonSQS() {
        return amazonSQS;
    }

    public SQSReceiver getSqsReceiver() {
        return sqsReceiver;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getMessageIdKey() {
        return messageIdKey;
    }
}
