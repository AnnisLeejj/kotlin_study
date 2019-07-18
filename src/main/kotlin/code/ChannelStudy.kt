package code

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.util.*

object ChannelStudy {
    @JvmStatic
    fun main(args: Array<String>) {
        //简单示例
//        main()
        //关闭与迭代通道
//        main1()

        //管道
//        main3()
        //使用管道的素数
//        main4()
//        扇出
//        main5()
//        扇入
//       println( main6())
        //带缓冲的通道
//        main7()
        //通道是公平的
//        main8()
        //计时器通道
        main9()
    }

    /**
     * 计时器通道
    计时器通道是一种特别的会合通道，每次经过特定的延迟都会从该通道进行消费并产生 Unit。
    虽然它看起来似乎没用，它被用来构建分段来创建复杂的基于时间的 produce 管道和进行窗口化操作以及其它时间相关的处理。 可以在 select 中使用计时器通道来进行“打勾”操作。

    使用工厂方法 ticker 来创建这些通道。 为了表明不需要其它元素，请使用 ReceiveChannel.cancel 方法。
     */
    fun main9() = runBlocking<Unit> {
        val tickerChannel = ticker(initialDelayMillis = 0, delayMillis = 100) //创建计时器通道
        var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
        println("Initial element is available immediately: $nextElement") // 初始尚未经过的延迟

        nextElement = withTimeoutOrNull(50) { tickerChannel.receive() } // 所有随后到来的元素都经过了 100 毫秒的延迟
        println("Next element is not ready in 50 ms: $nextElement")

        nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
        println("Next element is ready in 100 ms: $nextElement")

        // 模拟大量消费延迟
        println("Consumer pauses for 150ms")
        delay(150)
        // 下一个元素立即可用
        nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
        println("Next element is available immediately after large consumer delay: $nextElement")
        // 请注意，`receive` 调用之间的暂停被考虑在内，下一个元素的到达速度更快
        nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
        println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

        tickerChannel.cancel() // 表明不再需要更多的元素
    }

    /**
     * 发送和接收操作是 公平的 并且尊重调用它们的多个协程。
     * 它们遵守先进先出原则，可以看到第一个协程调用 receive 并得到了元素。
     * 在下面的例子中两个协程“乒”和“乓”都从共享的“桌子”通道接收到这个“球”元素。
     */
    fun main8() = runBlocking {
        val table = Channel<Ball>() // 一个共享的 table（桌子）
        launch { player("ping", table) }
        launch { player("pong", table) }
        table.send(Ball(0)) // 乒乓球
        delay(1000) // 延迟 1 秒钟
        coroutineContext.cancelChildren() // 游戏结束，取消它们
    }

    class Ball(hints: Int) {
        var hits: Int = hints
        override fun toString(): String {
            return "Ball hits:$hits"
        }
    }

    suspend fun player(name: String, table: Channel<Ball>) {
        for (ball in table) { // 在循环中接收球
            ball.hits++
            println("$name $ball")
            delay(100) // 等待一段时间
            table.send(ball) // 将球发送回去
        }
    }

    fun main7() = runBlocking<Unit> {
        val channel = Channel<Int>(4) // 启动带缓冲的通道
        val sender = launch {
            // 启动发送者协程
            repeat(10) {
                println("Sending $it") // 在每一个元素发送前打印它们
                channel.send(it) // 将在缓冲区被占满时挂起
            }
        }
        // 没有接收到东西……只是等待……
        delay(1000)
        sender.cancel() // 取消发送者协程
    }

    /**
     * 无缓冲的通道在发送者和接收者相遇时传输元素（aka rendezvous（这句话应该是个俚语，意思好像是“又是约会”的意思，不知道怎么翻））。
     * 如果发送先被调用，则它将被挂起直到接收被调用， 如果接收先被调用，它将被挂起直到发送被调用。
     */
    fun main6() = runBlocking {
        val channel = Channel<String>()
        launch { sendString(channel, "foo", 200L) }
        launch { sendString(channel, "BAR!", 500L) }
        repeat(6) {
            // 接收前六个
            println(channel.receive())
        }
        coroutineContext.cancelChildren() // 取消所有子协程来让主协程结束
    }

    suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
        while (true) {
            delay(time)
            channel.send(s)
        }
    }

    fun main5() = runBlocking<Unit> {
        val producer = produceNumbers2()
        repeat(5) { launchProcessor(it, producer) }
        delay(10950)
        producer.cancel() // 取消协程生产者从而将它们全部杀死
    }

    private fun CoroutineScope.produceNumbers2() = produce<Int> {
        delay(5000) // 等待 1 秒
        var x = 1 // start from 1
        println("Processor produceNumbers2 ${Date()}")
        while (true) {
            send(x++) // 产生下一个数字
            delay(100) // 等待 0.1 秒
        }
    }

    private fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
        println("Processor #$id received ${Date()}")
        for (msg in channel) {//我认为这是阻塞代码 ->下面日志证明是阻塞的
            delay(50) // 等待 0.05 秒
            println("Processor #$id received $msg")
        }
        //Processor #0 received Wed Jul 17 17:06:03 CST 2019
        //Processor #1 received Wed Jul 17 17:06:03 CST 2019
        //Processor #2 received Wed Jul 17 17:06:03 CST 2019
        //Processor #3 received Wed Jul 17 17:06:03 CST 2019
        //Processor #4 received Wed Jul 17 17:06:03 CST 2019
        //Processor produceNumbers2 Wed Jul 17 17:06:08 CST 2019
        //Processor #0 received 1
        //Processor #1 received 2
        //Processor #2 received 3
        //Processor #3 received 4
    }

    /**
     * 没有看懂 -> 2019/07/17
     *
     */
    fun main4() = runBlocking {
        var cur = numbersFrom(2)
        for (i in 1..100) {
            //第一次    cur = numbersFrom ->2,3,4,5,6.....
            //第二次后  cur = filter
            val prime = cur.receive()
            println(prime)
            //第一次    cur = numbersFrom ->3,4,5,6.....     prime = 2
            //第二次后
            cur = filter(cur, prime)
        }
        coroutineContext.cancelChildren() // 取消所有的子协程来让主协程结束
    }

    //send   2,3,4,5,6.....
    private fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
        var x = start
        while (true) send(x++) // 从 start 开始过滤整数流
    }

    /**
     *
     * prime 质数
     */
    private fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
        //println(" CoroutineScope.filter  prime : $prime")
        //numbers  3,4,5,6,7,8,9,10.....
        for (x in numbers) {
            //println(" CoroutineScope.filter  x : $x")
            if (x % prime != 0)
                send(x)
        }
    }

    fun main3() = runBlocking {
        val numbers = produceNumbers() // 从 1 开始生产整数
        val squares = square(numbers) // 对整数做平方
        for (i in 1..5) println(squares.receive()) // 打印前 5 个数字
        println("Done!") // 我们的操作已经结束了
        /**  emphasis  **/
        coroutineContext.cancelChildren() // 取消子协程
    }

    fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
        for (x in numbers) send(x * x)
    }

    fun CoroutineScope.produceNumbers() = produce<Int> {
        var x = 1
        while (true) send(x++) // 在流中开始从 1 生产无穷多个整数
    }

    private fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
        for (x in 1..5) send(x * x)
    }

    fun main2() = runBlocking {
        val squares = produceSquares()
        squares.consumeEach { println(it) }
        println("Done!")
    }

    /**
     * 从概念上来说，一个 close 操作就像向通道发送了一个特殊的关闭指令。
     * 这个迭代停止就说明关闭指令已经被接收了。所以这里保证所有先前发送出去的元素都在通道关闭前被接收到。
     */
    fun main1() = runBlocking {
        val channel = Channel<Int>()
        launch {
            for (x in 1..10) channel.send(x * x)
            channel.close() // 我们结束发送

            //结束后不能再接收了,会报错
            //kotlinx.coroutines.channels.ClosedSendChannelException: Channel was closed
//                channel.send(1234)
        }
        // 这里我们使用 `for` 循环来打印所有被接收到的元素（直到通道被关闭）

        //以下都是遍历后会移除
        channel.consumeEach {
            println(it)
        }
        for (y in channel) println(y)
        println("Done!")
    }

    fun main() = runBlocking {
        val channel = Channel<Int>()
        launch {
            // 这里可能是消耗大量 CPU 运算的异步逻辑，我们将仅仅做 5 次整数的平方并发送
            for (x in 1..5) channel.send(x * x)
        }
        // 这里我们打印了 5 次被接收的整数：
        repeat(5) { println(channel.receive()) }
        println("Done!")
    }
}