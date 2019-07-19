package code

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.*
import java.io.IOException

object 异常处理 {
    @JvmStatic
    fun main(args: Array<String>) {
//        main1()
//        main2()
//        main3()
//        main4()
//        main5()
//        main6()
//        main7()
        main8()
//        main9()
//        main10()
//        main11()

    }
    /**
     * 监督协程中的异常
     */
    fun main8() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        supervisorScope {
            val child = launch(handler) {
                println("Child throws an exception")
                throw AssertionError()
            }
            delay(1)
            println("Scope is completing")
        }
        println("Scope is completed")
    }
    /**
     *
     */
    fun main7() = runBlocking {
        try {
            supervisorScope {
                val child = launch {
                    try {
                        println("Child is sleeping")
                        delay(Long.MAX_VALUE)
                    } finally {
                        println("Child is cancelled")
                    }
                }
                // 使用 yield 来给我们的子作业一个机会来执行打印
                yield()
                println("Throwing exception from scope")
                throw AssertionError()
            }
        } catch(e: AssertionError) {
            println("Caught assertion error")
        }
    }
    /**
     * 监督作业
     */
    fun main6() = runBlocking {
        val supervisor = SupervisorJob()
        with(CoroutineScope(coroutineContext + supervisor)) {
            // 启动第一个子作业——这个示例将会忽略它的异常（不要在实践中这么做！）
            val firstChild = launch(CoroutineExceptionHandler { _, _ ->  }) {
                println("First child is failing")
                throw AssertionError("First child is cancelled")
            }
            // 启动第两个子作业
            val secondChild = launch {
                firstChild.join()
                // 取消了第一个子作业且没有传播给第二个子作业
                println("First child is cancelled: ${firstChild.isCancelled}, but second one is still active")
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    // 但是取消了监督的传播
                    println("Second child is cancelled because supervisor is cancelled")
                }
            }
            // 等待直到第一个子作业失败且执行完成
            firstChild.join()
            println("Cancelling supervisor")
            supervisor.cancel()
            secondChild.join()
        }
    }
    /**
     *取消异常是透明的并且会在默认情况下解包：
     */
    fun main5() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught original $exception")
        }
        val job = GlobalScope.launch(handler) {
            val inner = launch {
                launch {
                    launch {
                        throw IOException()
                    }
                }
            }
            try {
                inner.join()
            } catch (e: CancellationException) {
                println("Rethrowing CancellationException with original cause")
                throw e
            }
        }
        job.join()
    }

    /**
     * 异常聚合
     * 如果一个协程的多个子协程抛出异常将会发生什么？
     * 通常的规则是“第一个异常赢得了胜利”，所以第一个被抛出的异常将会暴露给处理者。
     * 但也许这会是异常丢失的原因，比如说一个协程在 finally 块中抛出了一个异常。 这时，多余的异常将会被压制。
     */
    fun main4() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception with suppressed ${exception.suppressed.contentToString()}")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    throw ArithmeticException()
                }
            }
            launch {
                delay(100)
                throw IOException()
            }
            delay(Long.MAX_VALUE)
        }
        job.join()
    }

    fun main3() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                // 第一个子协程
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    withContext(NonCancellable) {
                        println("Children are cancelled, but exception is not handled until all children terminate")
                        delay(100)
                        println("The first child finished its non cancellable block")
                    }
                }
            }
            launch {
                // 第二个子协程
                delay(10)
                println("Second child throws an exception")
                throw ArithmeticException()
            }
        }
        job.join()
    }

    fun main2() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("Caught $exception")
        }
        val job = GlobalScope.launch(handler) {
            throw AssertionError()
        }
        val deferred = GlobalScope.async(handler) {
            throw ArithmeticException() // 没有打印任何东西，依赖用户去调用 deferred.await()
        }
        joinAll(job, deferred)
    }

    fun main1() = runBlocking {
        val job = GlobalScope.launch {
            println("Throwing exception from launch")
            throw IndexOutOfBoundsException() // 我们将在控制台打印 Thread.defaultUncaughtExceptionHandler
        }
        job.join()
        println("Joined failed job")
        val deferred = GlobalScope.async {
            println("Throwing exception from async")
            throw ArithmeticException() // 没有打印任何东西，依赖用户去调用等待
        }
        try {
            deferred.await()
            println("Unreached")
        } catch (e: ArithmeticException) {
            println("Caught ArithmeticException")
        }
    }
}