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
        main6()
    }
    fun main6() = runBlocking {
        val channel = Channel<String>()
        launch { sendString(channel, "foo", 200L) }
        launch { sendString(channel, "BAR!", 500L) }
        repeat(6) { // 接收前六个
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