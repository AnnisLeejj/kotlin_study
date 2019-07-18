package code

import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.system.measureTimeMillis

object 组合挂起函数 {

    @JvmStatic
    fun main(args: Array<String>) {
//        main1()
        //使用并发
//        main2()
//惰性启动的 asyn
//        main3()
//        main4()
//        main5()
        main6()
    }
    ///取消始终通过协程的层次结构来进行传递：
    fun main6() = runBlocking<Unit> {
        try {
            failedConcurrentSum()
        } catch(e: ArithmeticException) {
            println("Computation failed with ArithmeticException")
        }
    }

    suspend fun failedConcurrentSum(): Int = coroutineScope {
        val one = async<Int> {
            try {
                delay(Long.MAX_VALUE) // 模拟一个长时间的运算
                42
            } finally {
                println("First child was cancelled")
            }
        }
        val two = async<Int> {
            println("Second child throws an exception")
            throw ArithmeticException()
        }
        one.await() + two.await()
    }
    //使用 async 的结构化并发
    fun main5() = runBlocking<Unit> {
        val time = measureTimeMillis {
            println("The answer is ${concurrentSum()}")
        }
        println("Completed in $time ms")
    }

    suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        one.await() + two.await()
    }
    // 注意，在这个示例中我们在 `main` 函数的右边没有加上 `runBlocking`
    fun main4() {
        val time = measureTimeMillis {
            // 我们可以在协程外面启动异步执行
            val one = somethingUsefulOneAsync()
            val two = somethingUsefulTwoAsync()
            // 但是等待结果必须调用其它的挂起或者阻塞
            // 当我们等待结果的时候，这里我们使用 `runBlocking { …… }` 来阻塞主线程
            runBlocking {
                println("The answer is ${one.await() + two.await()}")
            }
        }
        println("Completed in $time ms")
    }

    fun somethingUsefulOneAsync() = GlobalScope.async {
        doSomethingUsefulOne()
    }

    fun somethingUsefulTwoAsync() = GlobalScope.async {
        doSomethingUsefulTwo()
    }
    fun main3() = runBlocking<Unit> {
        val time = measureTimeMillis {
            launch { }
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            // 执行一些计算
            one.start() // 启动第一个
            two.start() // 启动第二个
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }

    /**
     * 如果 doSomethingUsefulOne 与 doSomethingUsefulTwo 之间没有依赖，并且我们想更快的得到结果，让它们进行 并发 吗？这就是 async 可以帮助我们的地方。

    在概念上，async 就类似于 launch。
    它启动了一个单独的协程，这是一个轻量级的线程并与其它所有的协程一起并发的工作。
    不同之处在于 launch 返回一个 Job 并且不附带任何结果值，而 async 返回一个 Deferred —— 一个轻量级的非阻塞 future，
    这代表了一个将会在稍后提供结果的 promise。你可以使用 .await() 在一个延期的值上得到它的最终结果， 但是 Deferred 也是一个 Job，所以如果需要的话，你可以取消它。
     */
    fun main2() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }

    /**
     * 默认顺序调用
     * 假设我们在不同的地方定义了两个进行某种调用远程服务或者进行计算的挂起函数。
     * 我们只假设它们都是有用的，但是实际上它们在这个示例中只是为了该目的而延迟了一秒钟：
     */
    fun main1() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }
        println("Completed in $time ms")
    }

    suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // 假设我们在这里做了些有用的事
        return 13
    }

    suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // 假设我们在这里也做了一些有用的事
        return 29
    }
}