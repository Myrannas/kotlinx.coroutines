/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.coroutines.experimental.reactive

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.selects.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.*
import org.junit.runners.*

@RunWith(Parameterized::class)
class PublisherSubscriptionSelectTest(val request: Int) : TestBase() {
    companion object {
        @Parameterized.Parameters(name = "request = {0}")
        @JvmStatic
        fun params(): Collection<Array<Any>> = listOf(0, 1, 10).map { arrayOf<Any>(it) }
    }

    @Test
    fun testSelect() = runTest {
        // source with n ints
        val n = 1000 * stressTestMultiplier
        val source = publish(coroutineContext) { repeat(n) { send(it) } }
        var a = 0
        var b = 0
        // open two subs
        source.openSubscription(request).use { channelA ->
            source.openSubscription(request).use { channelB ->
                loop@ while (true) {
                    val done: Int = select {
                        channelA.onReceiveOrNull {
                            if (it != null) assertEquals(a++, it)
                            if (it == null) 0 else 1
                        }
                        channelB.onReceiveOrNull {
                            if (it != null) assertEquals(b++, it)
                            if (it == null) 0 else 2
                        }
                    }
                    when (done) {
                        0 -> break@loop
                        1 -> {
                            val r = channelB.receiveOrNull()
                            if (r != null) assertEquals(b++, r)
                        }
                        2 -> {
                            val r = channelA.receiveOrNull()
                            if (r != null) assertEquals(a++, r)
                        }
                    }
                }
            }
        }
        // should receive one of them fully
        assertTrue(a == n || b == n)
    }
}