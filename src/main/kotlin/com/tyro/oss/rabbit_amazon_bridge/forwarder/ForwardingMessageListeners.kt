/*
 * Copyright [2018] Tyro Payments Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tyro.oss.rabbit_amazon_bridge.forwarder

import com.amazonaws.SdkBaseException
import com.bazaarvoice.jolt.JsonUtils
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.MessageTransformer
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.core.MessageListener
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.MessagingException
import org.springframework.messaging.support.MessageHeaderAccessor

abstract class MessageTransformingMessageListener(val messageTransformer: MessageTransformer) : MessageListener {

    override fun onMessage(rabbitMessage: RabbitMessage) {
        val transformedMessage = messageTransformer.transform(String(rabbitMessage.body))
        forwardMessage(transformedMessage.toAWSMessage())
    }

    abstract fun forwardMessage(message: AwsStringMessage)
}

class SnsForwardingMessageListener(
    val topicName: String,
    val topicNotificationMessagingTemplate: NotificationMessagingTemplate,
    messageTransformer: MessageTransformer
) : MessageTransformingMessageListener(messageTransformer) {

    override fun onMessage(rabbitMessage: RabbitMessage) {
        val transformedMessage = messageTransformer.transform(String(rabbitMessage.body))
        forwardMessage(transformedMessage.toAWSMessage(generateMessageAttributes(rabbitMessage)))
    }

    override fun forwardMessage(message: AwsStringMessage) {
        topicNotificationMessagingTemplate.send(topicName, message)
    }

    private fun generateMessageAttributes(rabbitMessage: RabbitMessage): MutableMap<String, String> {
        val jsonToMap = JsonUtils.jsonToMap(String(rabbitMessage.body))

        val attributes = mutableMapOf<String, String>()
        if (jsonToMap.containsKey("type")) {
            attributes["type"] = jsonToMap["type"] as String
        }
        return attributes
    }
}

class SqsForwardingMessageListener(
    val queueName: String,
    val queueMessagingTemplate: QueueMessagingTemplate,
    messageTransformer: MessageTransformer
) : MessageTransformingMessageListener(messageTransformer) {
    override fun forwardMessage(message: AwsStringMessage) {
        queueMessagingTemplate.send(queueName, message)
    }
}

class DeadletteringMessageListener(val messageListener: MessageListener, val shouldRetry: Boolean = false) : MessageListener {

    private val LOG = LoggerFactory.getLogger(DeadletteringMessageListener::class.java)

    override fun onMessage(rabbitMessage: RabbitMessage) {
        try {
            LOG.info("Message received on ${rabbitMessage.messageProperties.receivedExchange} / ${rabbitMessage.messageProperties.consumerQueue}")
            messageListener.onMessage(rabbitMessage)
        } catch (exception: Exception) {
            when {
                shouldRetry && (exception is SdkBaseException || exception is MessagingException) -> {
                    LOG.warn("A retryable error occurred.", exception)
                    throw exception
                }
                else -> throw AmqpRejectAndDontRequeueException(exception)
            }
        }
    }
}

private fun String.toAWSMessage(attributes: Map<String, String>? = emptyMap()): Message<String> {
    val accessor = MessageHeaderAccessor()
    attributes?.forEach {
        accessor.setHeader(it.key, it.value)
    }

    return AWSStringMessageBuilder.withPayload(this).setHeaders(accessor).build()
}
