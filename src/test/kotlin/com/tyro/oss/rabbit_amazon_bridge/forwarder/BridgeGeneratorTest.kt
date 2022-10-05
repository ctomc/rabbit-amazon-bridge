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

import com.bazaarvoice.jolt.JoltTransform
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.tyro.oss.rabbit_amazon_bridge.generator.BridgeGenerator
import com.tyro.oss.rabbit_amazon_bridge.generator.RabbitCreationService
import com.tyro.oss.rabbit_amazon_bridge.generator.fromRabbitToSNSInstance
import com.tyro.oss.rabbit_amazon_bridge.generator.fromRabbitToSQSInstance
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.DoNothingMessageTransformer
import com.tyro.oss.rabbit_amazon_bridge.messagetransformer.JoltMessageTransformer
import com.tyro.oss.randomdata.RandomBoolean.randomBoolean
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.amqp.core.Exchange
import org.springframework.amqp.core.Queue
import java.util.Objects

@RunWith(MockitoJUnitRunner::class)
class BridgeGeneratorTest {

    @Mock
    private lateinit var deadLetteringrabbitCreationService: RabbitCreationService
    @Mock
    private lateinit var queueMessagingTemplate: QueueMessagingTemplate
    @Mock
    private lateinit var topicNotificationMessagingTemplate: NotificationMessagingTemplate

    private lateinit var bridgeGenerator: BridgeGenerator

    private val shouldRetry = randomBoolean()

    companion object {
        const val TRANSFORMATION_SPECS = """[
                  {
                    "operation": "shift",
                    "spec": {
                      "*": {
                        "bid": "[&1].bid2"
                      }
                    }
                  }]"""
    }

    @Before
    fun setUp() {
        bridgeGenerator = BridgeGenerator(
                deadLetteringrabbitCreationService,
                queueMessagingTemplate,
                topicNotificationMessagingTemplate,
                shouldRetry
        )
    }

    @Test
    fun `should generate sns bridge`() {
        val joltSpec = jacksonObjectMapper().readValue<List<Object>>(TRANSFORMATION_SPECS)
        val bridge = fromRabbitToSNSInstance(transformationSpecs = joltSpec)
        val exchange = mock<Exchange>()
        val deadletterExchange = mock<Exchange>()
        val queue = mock<Queue>()
        val deadletterQueue = mock<Queue>()
        val index = 0

        val fromRabbit = bridge.from.rabbit!!
        val deadletter = fromRabbit.deadLetter
        val deadletterPrefixed = "dead.$deadletter"

        whenever(deadLetteringrabbitCreationService.createExchange(
            fromRabbit.exchange,
            fromRabbit.exchangeType,
            deadletterPrefixed
        )).thenReturn(Pair(exchange, deadletterExchange))
        whenever(deadLetteringrabbitCreationService.createQueue(fromRabbit.queueName, fromRabbit.exchange, deadletterPrefixed)).thenReturn(Pair(queue, deadletterQueue))

        val endPoint = bridgeGenerator.generateFromRabbit(index, bridge)

        verify(deadLetteringrabbitCreationService).createExchange(
            fromRabbit.exchange,
            fromRabbit.exchangeType,
            deadletterPrefixed
        )
        verify(deadLetteringrabbitCreationService).createQueue(fromRabbit.queueName, fromRabbit.exchange, deadletterPrefixed)
        verify(deadLetteringrabbitCreationService).bind(queue, exchange, fromRabbit.routingKey)
        verify(deadLetteringrabbitCreationService).bind(deadletterQueue, deadletterExchange, fromRabbit.queueName)

        val deadletteringMessageListener = endPoint.messageListener as DeadletteringMessageListener
        assertThat(endPoint.id).isEqualTo("org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#$index")
        assertThat(endPoint.queueNames).isEqualTo(listOf(fromRabbit.queueName))
        assertThat(deadletteringMessageListener.shouldRetry).isEqualTo(shouldRetry)
        val snsMessageListener = deadletteringMessageListener.messageListener as SnsForwardingMessageListener
        assertThat(snsMessageListener.topicName).isEqualTo(bridge.to.sns?.name)
        assertThat(snsMessageListener.topicNotificationMessagingTemplate).isEqualTo(topicNotificationMessagingTemplate)
        assertThat(((snsMessageListener.messageTransformer as JoltMessageTransformer).chainr)).isNotNull
    }

    @Test
    fun `should generate sqs bridge`() {
        val joltSpec =jacksonObjectMapper().readValue<List<Object>>(TRANSFORMATION_SPECS)
        val bridge = fromRabbitToSQSInstance(transformationSpecs = joltSpec)
        val exchange = mock<Exchange>()
        val deadletterExchange = mock<Exchange>()
        val queue = mock<Queue>()
        val deadletterQueue = mock<Queue>()
        val index = 0

        val fromRabbit = bridge.from.rabbit!!
        val deadletter = fromRabbit.deadLetter
        val deadletterPrefixed = "dead.$deadletter"
        whenever(deadLetteringrabbitCreationService.createExchange(
            fromRabbit.exchange,
            fromRabbit.exchangeType,
            deadletterPrefixed
        )).thenReturn(Pair(exchange, deadletterExchange))
        whenever(deadLetteringrabbitCreationService.createQueue(fromRabbit.queueName, fromRabbit.exchange, deadletterPrefixed)).thenReturn(Pair(queue, deadletterQueue))

        val endPoint = bridgeGenerator.generateFromRabbit(index, bridge)

        verify(deadLetteringrabbitCreationService).createExchange(
            fromRabbit.exchange,
            fromRabbit.exchangeType,
            deadletterPrefixed
        )
        verify(deadLetteringrabbitCreationService).createQueue(fromRabbit.queueName, fromRabbit.exchange, deadletterPrefixed)
        verify(deadLetteringrabbitCreationService).bind(queue, exchange, fromRabbit.routingKey)
        verify(deadLetteringrabbitCreationService).bind(deadletterQueue, deadletterExchange, fromRabbit.queueName)

        val deadletteringMessageListener = endPoint.messageListener as DeadletteringMessageListener
        assertThat(endPoint.id).isEqualTo("org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#$index")
        assertThat(endPoint.queueNames).isEqualTo(listOf(fromRabbit.queueName))
        assertThat(deadletteringMessageListener.shouldRetry).isEqualTo(shouldRetry)
        val sqsMessageListener = deadletteringMessageListener.messageListener as SqsForwardingMessageListener
        assertThat(sqsMessageListener.queueName).isEqualTo(bridge.to.sqs?.name)
        assertThat(sqsMessageListener.queueMessagingTemplate).isEqualTo(queueMessagingTemplate)
        assertThat(((sqsMessageListener.messageTransformer as JoltMessageTransformer).chainr)).isNotNull
    }

    @Test
    fun `should generate sqs bridge with DoNothingMessageTransformer when transformationSpecs is null`() {
        val bridge = fromRabbitToSQSInstance(transformationSpecs = null)
        val exchange = mock<Exchange>()
        val deadletterExchange = mock<Exchange>()
        val queue = mock<Queue>()
        val deadletterQueue = mock<Queue>()
        val index = 0

        val fromRabbit = bridge.from.rabbit!!
        val deadletter = fromRabbit.deadLetter
        val deadletterPrefixed = "dead.$deadletter"
        whenever(deadLetteringrabbitCreationService.createExchange(
            fromRabbit.exchange,
            fromRabbit.exchangeType,
            deadletterPrefixed
        )).thenReturn(Pair(exchange, deadletterExchange))
        whenever(deadLetteringrabbitCreationService.createQueue(fromRabbit.queueName, fromRabbit.exchange, deadletterPrefixed)).thenReturn(Pair(queue, deadletterQueue))

        val endPoint = bridgeGenerator.generateFromRabbit(index, bridge)

        val deadletteringMessageListener = endPoint.messageListener as DeadletteringMessageListener
        val sqsMessageListener = deadletteringMessageListener.messageListener as SqsForwardingMessageListener
        assertThat(sqsMessageListener.messageTransformer).isInstanceOf(DoNothingMessageTransformer::class.java)
    }

    @Test
    fun `should generate sqs bridge with DoNothingMessageTransformer when transformationSpecs is empty`() {
        val bridge = fromRabbitToSQSInstance(transformationSpecs = emptyList())
        val exchange = mock<Exchange>()
        val deadletterExchange = mock<Exchange>()
        val queue = mock<Queue>()
        val deadletterQueue = mock<Queue>()
        val index = 0

        val fromRabbit = bridge.from.rabbit!!
        val deadletter = fromRabbit.deadLetter
        val deadletterPrefixed = "dead.$deadletter"
        whenever(deadLetteringrabbitCreationService.createExchange(
            fromRabbit.exchange,
            fromRabbit.exchangeType,
            deadletterPrefixed
        )).thenReturn(Pair(exchange, deadletterExchange))
        whenever(deadLetteringrabbitCreationService.createQueue(fromRabbit.queueName, fromRabbit.exchange, deadletterPrefixed)).thenReturn(Pair(queue, deadletterQueue))

        val endPoint = bridgeGenerator.generateFromRabbit(index, bridge)

        val deadletteringMessageListener = endPoint.messageListener as DeadletteringMessageListener
        val sqsMessageListener = deadletteringMessageListener.messageListener as SqsForwardingMessageListener
        assertThat(sqsMessageListener.messageTransformer).isInstanceOf(DoNothingMessageTransformer::class.java)
    }
}