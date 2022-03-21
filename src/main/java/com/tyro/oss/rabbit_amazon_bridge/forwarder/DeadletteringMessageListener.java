package com.tyro.oss.rabbit_amazon_bridge.forwarder;

import com.amazonaws.SdkBaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.messaging.MessagingException;

public class DeadletteringMessageListener implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(DeadletteringMessageListener.class);
    final MessageListener messageListener;
    final boolean shouldRetry;

    public DeadletteringMessageListener(MessageListener messageListener, boolean shouldRetry) {
        this.messageListener = messageListener;
        this.shouldRetry = shouldRetry;
    }
    public DeadletteringMessageListener(MessageListener messageListener) {
        this(messageListener, false);
    }

    @Override
    public void onMessage(Message rabbitMessage) {
        try {
            LOG.info("Message received on {} / {}",
                    rabbitMessage.getMessageProperties().getReceivedExchange(),
                    rabbitMessage.getMessageProperties().getConsumerQueue()
            );
            messageListener.onMessage(rabbitMessage);
        } catch (Exception e) {
            if (this.shouldRetry && ((e instanceof SdkBaseException) || (e instanceof MessagingException))) {
                LOG.warn("A retryable error occurred.", e);
                throw e;
            } else {
                throw new AmqpRejectAndDontRequeueException(e);
            }
        }
    }
}
