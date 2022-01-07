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

package com.tyro.oss.rabbit_amazon_bridge.generator

import org.springframework.amqp.core.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RabbitCreationService(@Autowired val amqpAdmin: AmqpAdmin) {
    fun createExchange(exchange: String, exchangeType: String, deadletterExchangeName: String): Pair<Exchange, Exchange> {
        val customExchange = CustomExchange(exchange, exchangeType, true, false)
        val deadletterTopicExchange = TopicExchange(deadletterExchangeName, true, false)

        amqpAdmin.declareExchange(customExchange)
        amqpAdmin.declareExchange(deadletterTopicExchange)

        return Pair(customExchange, deadletterTopicExchange)
    }

    fun createQueue(queueName: String, exchange: String, deadletterQueueAndExchangeName: String): Pair<Queue, Queue>  {
        val args = mapOf(
                "x-dead-letter-exchange" to deadletterQueueAndExchangeName,
                "x-dead-letter-routing-key" to "dead.$queueName"
        )

        val queue = Queue(queueName, true, false, false, args)
        val deadletterQueue = Queue(deadletterQueueAndExchangeName, true, false, false, null)

        amqpAdmin.declareQueue(queue)
        amqpAdmin.declareQueue(deadletterQueue)
        return Pair(queue, deadletterQueue)
    }

    fun bind(queue: Queue, exchange: Exchange, routingKey: String) {
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routingKey).noargs())
    }
}