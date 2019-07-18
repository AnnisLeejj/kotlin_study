package code

import kotlinx.coroutines.*
import kotlin.concurrent.thread

object Coroutines {
   @JvmStatic
    fun main(args: Array<String>) {
//        code1()
        //println(code2())
//        code3()
//        code4()
        code5()
//        code6()
//        code7()
        //code8()

        //取消
//        code9()
//        code10()
//        code11()
        //资源释放
//        code12()
//        code13()
        //超时
//        code14()
//        code15()
    }
    fun code15() = runBlocking {
        val result = withTimeoutOrNull(1300L) {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(300L)
            }
            "Done" // 在它运行得到结果之前取消它
        }
        println("Result is $result")
    }
    fun code14() = runBlocking {
        withTimeout(1600L) {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
    }
    /**
     * 在真实的案例中，当你需要挂起一个被取消的协程，你可以将相应的代码包装在 withContext(NonCancellable) {……} 中，
     * 并使用 withContext 函数以及 NonCancellable 上下文
     */
    fun code13() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("job: I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                //只有调用withContext(NonCancellable){}  才能正常执行协程
                withContext(NonCancellable) {
                    println("job: I'm running finally")

                    repeat(5) { i ->
                        println("job: I'm withContext sleeping $i ...")
                        delay(500L)
                    }
                    delay(1000L)
                    println("job: And I've just delayed for 1 sec because I'm non-cancellable")
                }
            }
        }
        delay(1300L) // 延迟一段时间
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // 取消该作业并等待它结束
        println("main: Now I can quit.")
    }

    /**
     * 例子中任何尝试在 finally 块中调用挂起函数的行为都会抛出 CancellationException，因为这里持续运行的代码是可以被取消的。
     */
    fun code12() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("job: I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("job: I'm running finally")
            }
        }
        delay(1300L) // 延迟一段时间
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // 取消该作业并且等待它结束
        println("main: Now I can quit.")
    }

    fun code11() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            var times = 0
            while (isActive) { // 可以被取消的计算循环
                // 每秒打印消息两次
                println("job: while ->$isActive   ${times++}")

                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job: I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }
        delay(1300L) // 等待一段时间
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // 取消该作业并等待它结束
        println("main: Now I can quit.")
    }

    //job.cancelAndJoin() // 取消一个作业并且等待它结束
    //然而，如果协程正在执行计算任务，并且没有检查取消的话，那么它是不能被取消的，就如如下示例代码所示
    fun code10() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5) { // 一个执行计算的循环，只是为了占用 CPU
                // 每秒打印消息两次
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job: I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }

        delay(1300L) // 等待一段时间
        println("main: I'm tired of waiting!")

        //然而，如果协程正在执行计算任务，并且没有检查取消的话，那么它是不能被取消的，就如如下示例代码所示

        job.cancelAndJoin() // 取消一个作业并且等待它结束
        println("main: Now I can quit.")
    }

    //取消协程的执行
    fun code9() = runBlocking {
        val job = launch {
            repeat(1000) { i ->
                println("job: I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(1300L) // 延迟一段时间
        println("main: I'm tired of waiting!")
        job.cancel() // 取消该作业
        job.join() // 等待作业执行结束
        println("main: Now I can quit.")
    }

    fun code8() = runBlocking {
        launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        //在 GlobalScope 中启动的活动协程并不会使进程保活。它们就像守护线程。
        GlobalScope.launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(1300L) // 在延迟后退出
    }

    fun code7() = runBlocking {
        repeat(100_000) {
            // 启动大量的协程
            launch {
                delay(1000L)
                print(".")
            }
        }
    }

    fun code6() = runBlocking {
        launch { doWorld() }
        println("Hello,")
    }

    // 这是你的第一个挂起函数
    suspend fun doWorld() {
        delay(1000L)
        println("World!")
    }

    fun code5() = runBlocking {
        // this: CoroutineScope

        launch {
            delay(200L)
            println("Task from runBlocking")
        }

        //runBlocking 与 coroutineScope 的主要区别在于后者在等待所有子协程执行完毕时不会阻塞当前线程。
        coroutineScope {
            // 创建一个 ->协程作用域
            launch {
                delay(500L)
                println("Task from nested launch")
            }

            delay(100L)
            println("Task from coroutine scope") // 这一行会在内嵌 launch 之前输出
        }

        // 等到  coroutineScope 执行完毕
        println("Coroutine scope is over") // 这一行在内嵌 launch 执行完毕后才输出
    }

    fun code4() = runBlocking {
        // this: CoroutineScope
        launch {
            // 在 runBlocking 作用域中启动一个新协程
            delay(1000L)
            println("World!")
        }
        println("Hello,")
    }

    /*    等待一个作业
        延迟一段时间来等待另一个协程运行并不是一个好的选择。
        让我们显式（以非阻塞方式）等待所启动的后台 Job 执行结束：*/
    fun code3() = runBlocking<Unit> {
        // 开始执行主协程
        val job = GlobalScope.launch {
            // 在后台启动一个新的协程并继续
            delay(1000L)
            println("World!")
        }
        println("Hello,") // 主协程在这里会立即执行
        job.join() // 等待携程完成
        println("Finish,")
    }

    fun code2() = runBlocking<String> {
        // 开始执行主协程
        GlobalScope.launch {
            // 在后台启动一个新的协程并继续
            delay(1000L)
            println("World!")
        }
        println("Hello,") // 主协程在这里会立即执行
        delay(2000L) // 延迟 2 秒来保证 JVM 存活
        "finish"
    }

    private fun code1() {
        GlobalScope.launch {
            // 在后台启动一个新的协程并继续
            //这是因为 delay 是一个特殊的 挂起函数 ，它不会造成线程阻塞，但是会 挂起 协程，并且只能在协程中使用。

            delay(1000L) // 非阻塞的等待 1 秒钟（默认时间单位是毫秒）
            println("World!") // 在延迟后打印输出
        }
        println("Hello,") // 协程已在等待时主线程还在继续
        Thread.sleep(2000L) // 阻塞主线程 2 秒钟来保证 JVM 存活

        //非阻塞的 delay(……) 与 阻塞的 Thread.sleep(……)

        thread {
            Thread.sleep(1000L)
            println("World!2") // 在延迟后打印输出
        }
        println("Hello,2") // 协程已在等待时主线程还在继续
        runBlocking {
            // 但是这个表达式阻塞了主线程
            delay(2000L)  // ……我们延迟 2 秒来保证 JVM 的存活
        }
    }
}
