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

import com.bazaarvoice.jolt.JoltTransform
import com.tyro.oss.arbitrater.arbitraryInstance
import com.tyro.oss.randomdata.RandomBoolean.randomBoolean
import com.tyro.oss.randomdata.RandomString.randomString

fun fromRabbitToSQSInstance(shouldForwardMessages: Boolean? = false, transformationSpecs: List<Object>? = emptyList()
    ) = Bridge(rabbitFromDefinition(), transformationSpecs, sqsToDefinition(), shouldForwardMessages,randomString() )
//fun fromRabbitToSQSInstance() = arbitraryBridge();
//    .copy(from = rabbitFromDefinition())
//    .copy(transformationSpecs = transformationSpecs())
//    .copy(to = sqsToDefinition())

fun fromRabbitToSNSInstance(shouldForwardMessages: Boolean? = false, transformationSpecs: List<Object>? = emptyList()
) = Bridge(rabbitFromDefinition(), transformationSpecs, snsToDefinition(), shouldForwardMessages,randomString()
)
//fun fromRabbitToSNSInstance() = arbitraryBridge();
    /*.copy(from = rabbitFromDefinition())
    .copy(transformationSpecs = transformationSpecs())
    .copy(to = snsToDefinition())*/

fun fromSQSToRabbitInstance(shouldForwardMessages: Boolean) = Bridge(sqsFromDefinition(), transformationSpecs(),
    rabbitToDefinition(),shouldForwardMessages, randomString()
)



private fun arbitraryBridge() =
    Bridge(sqsFromDefinition(), transformationSpecs(), snsToDefinition(), randomBoolean(), randomString())

private fun sqsToDefinition() = Bridge.ToDefinition(null, Bridge.SqsDefinition(randomString()), null)
private fun snsToDefinition() = Bridge.ToDefinition(Bridge.SnsDefinition(randomString()), null, null)
private fun rabbitToDefinition() = Bridge.ToDefinition(
    null,
    null,
    Bridge.RabbitToDefinition(randomString(), randomString())
)

fun rabbitFromDefinition() = Bridge.FromDefinition(
    Bridge.RabbitFromDefinition(
        randomString(),
        randomString(),
        randomString()

    ),
    null
)

private fun sqsFromDefinition() = Bridge.FromDefinition(
    null,
    Bridge.SqsDefinition(randomString())
)

private fun transformationSpecs() = emptyList<JoltTransform>()
