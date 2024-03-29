package code

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import kotlin.random.Random

object Selector {
    @JvmStatic
    fun main(args: Array<String>) {
        //main1()
//        main2()
//        main3()
//        main4()
        main5()
    }

    fun main5() = runBlocking<Unit> {
        val chan = Channel<Deferred<String>>() // 测试使用的通道
        launch { // 启动打印协程
            for (s in switchMapDeferreds(chan))
                println(s) // 打印每个获得的字符串
        }
        chan.send(asyncString("BEGIN", 100))
        delay(200) // 充足的时间来生产 "BEGIN"
        chan.send(asyncString("Slow", 500))
        delay(100) // 不充足的时间来生产 "Slow"
        chan.send(asyncString("Replace", 100))
        delay(500) // 在最后一个前给它一点时间
        chan.send(asyncString("END", 500))
        delay(1000) // 给执行一段时间
        chan.close() // 关闭通道……
        delay(500) // 然后等待一段时间来让它结束
    }
    fun CoroutineScope.switchMapDeferreds(input: ReceiveChannel<Deferred<String>>) = produce<String> {
        var current = input.receive() // 从第一个接收到的延迟值开始
        while (isActive) { // 循环直到被取消或关闭
            val next = select<Deferred<String>?> { // 从这个 select 中返回下一个延迟值或 null
                input.onReceiveOrNull { update ->
                    println("input.onReceiveOrNull $update") // 打印每个获得的字符串
                    update // 替换下一个要等待的值
                }
                current.onAwait { value ->
                    send(value) // 发送当前延迟生成的值
                    input.receiveOrNull() // 然后使用从输入通道得到的下一个延迟值
                }
            }
            if (next == null) {
                println("Channel was closed")
                break // 跳出循环
            } else {
                current = next
            }
        }
    }

    fun CoroutineScope.asyncString(str: String, time: Long) = async {
        delay(time)
        str
    }
    /**
     * Select 延迟值
     */
    fun main4() = runBlocking<Unit> {
        val list = asyncStringsList()
        val result = select<String> {
            list.withIndex().forEach { (index, deferred) ->
                deferred.onAwait { answer ->
                    "Deferred $index produced answer '$answer'"
                }
            }
        }
        println(result)
        val countActive = list.count { it.isActive }
        println("$countActive coroutines are still active")
    }

    fun CoroutineScope.asyncStringsList(): List<Deferred<String>> {
        val random = Random(3)
        return List(12) { asyncString(random.nextInt(1000)) }
    }

    fun CoroutineScope.asyncString(time: Int) = async {
        delay(time.toLong())
        "Waited for $time ms"
    }

    /**
     * Select 以发送
     */
    fun main3() = runBlocking<Unit> {
        val side = Channel<Int>() // 分配 side 通道
        launch {
            // 对于 side 通道来说，这是一个很快的消费者
            side.consumeEach { println("Side channel has $it") }
        }
        produceNumbers(side).consumeEach {
            println("Consuming $it")
            delay(250) // 不要着急，让我们正确消化消耗被发送来的数字
        }
        println("Done consuming")
        coroutineContext.cancelChildren()
    }

    fun CoroutineScope.produceNumbers(side: SendChannel<Int>) = produce<Int> {
        for (num in 1..10) { // 生产从 1 到 10 的 10 个数值
            delay(100) // 延迟 100 毫秒
            select<Unit> {
                onSend(num) {} // 发送到主通道
                side.onSend(num) {} // 或者发送到 side 通道
            }
        }
    }

    fun main2() = runBlocking<Unit> {
        val a = produce<String> {
            repeat(4) { send("Hello $it") }
        }
        val b = produce<String> {
            repeat(4) { send("World $it") }
        }
        repeat(18) {
            // 打印最早的八个结果
            println(selectAorB(a, b))
        }
        coroutineContext.cancelChildren()
    }

    /**
     * 通道关闭时 select
     * 首先，select 偏向于 第一个子句，当可以同时选到多个子句时， 第一个子句将被选中。
     * 在这里，两个通道都在不断地生成字符串，因此 a 通道作为 select 中的第一个子句获胜。
     * 然而因为我们使用的是无缓冲通道，所以 a 在其调用 send 时会不时地被挂起，进而 b 也有机会发送。

     *第二个观察结果是，当通道已经关闭时， 会立即选择 onReceiveOrNull。
     */
    suspend fun selectAorB(a: ReceiveChannel<String>, b: ReceiveChannel<String>): String =
        select<String> {
            a.onReceiveOrNull { value ->
                if (value == null)
                    "Channel 'a' is closed"
                else
                    "a -> '$value'"
            }
            b.onReceiveOrNull { value ->
                if (value == null)
                    "Channel 'b' is closed"
                else
                    "b -> '$value'"
            }

        }

    fun main1() = runBlocking<Unit> {
        val fizz = fizz()
        val buzz = buzz()
        repeat(7) {
            selectFizzBuzz(fizz, buzz)
//            selectAorB(fizz, buzz)
        }
        coroutineContext.cancelChildren() // 取消 fizz 和 buzz 协程
    }

    fun CoroutineScope.fizz() = produce<String> {
        while (true) { // 每 300 毫秒发送一个 "Fizz"
            delay(300)
            send("Fizz")
        }
    }

    fun CoroutineScope.buzz() = produce<String> {
        while (true) { // 每 500 毫秒发送一个 "Buzz!"
            delay(500)
            send("Buzz!")
        }
    }

    suspend fun selectFizzBuzz(fizz: ReceiveChannel<String>, buzz: ReceiveChannel<String>) {
        select<Unit> {
            // <Unit> 意味着该 select 表达式不返回任何结果
            fizz.onReceiveOrNull { value ->
                // 这是第一个 select 子句
                println("fizz -> '$value'")
            }
            buzz.onReceive { value ->
                // 这是第二个 select 子句
                println("buzz -> '$value'")
            }
        }
    }

}