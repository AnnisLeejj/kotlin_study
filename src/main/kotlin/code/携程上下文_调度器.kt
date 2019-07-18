package code

import kotlinx.coroutines.*
import javax.rmi.CORBA.Util
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object 携程上下文_调度器 {
    @JvmStatic
    fun main(args: Array<String>) {
//        main1()
//        main2()
        //调试协程与线程
//        main3()
//调试协程与线程
//        main4()
        //子协程
//        main5()
        //组合上下文中的元素
//        main6()
        main7()
    }

    val threadLocal = ThreadLocal<String?>() // 声明线程局部变量
    fun main7() = runBlocking<Unit> {
        threadLocal.set("main")
        println("Pre-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
        val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
            println("Launch start, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
            yield()
            println("After yield, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
        }
        job.join()
        println("Post-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
    }

    fun main6() = runBlocking<Unit> {
        launch(Dispatchers.Default + CoroutineName("test")) {
            println("I'm working in thread ${Thread.currentThread().name}")
        }
    }

    /**
     * 当一个协程被其它协程在 CoroutineScope 中启动的时候，
     * 它将通过 CoroutineScope.coroutineContext 来承袭上下文，
     * 并且这个新协程的 Job 将会成为父协程作业的 子 作业。
     * 当一个父协程被取消的时候，所有它的子协程也会被递归的取消。
     */
    fun main5() = runBlocking<Unit> {
        // 启动一个协程来处理某种传入请求（request）
        val request = launch {
            // 孵化了两个子作业, 其中一个通过 GlobalScope 启动
            //当 GlobalScope 被用来启动一个协程时，它与作用域无关且是独立被启动的。
            GlobalScope.launch {
                println("job1: I run in GlobalScope and execute independently!")
                delay(1000)
                /**
                 * request.cancel() 后 request上下文停止,而该结构没有使用request上下文
                 *所以 下面代码执行
                 */
                println("job1: I am not affected by cancellation of the request")
            }
            // 另一个则承袭了父协程的上下文
            launch {
                delay(100)
                println("job2: I am a child of the request coroutine")
                delay(1000)
                /**
                 * request.cancel()  父协程 取消了, 所以子协程也被取消
                 *所以 下面代码不执行
                 */
                println("job2: I will not execute this line if my parent request is cancelled")
            }
        }
        delay(500)
        request.cancel() // 取消请求（request）的执行
        delay(1000) // 延迟一秒钟来看看发生了什么
        println("main: Who has survived request cancellation?")
    }

    fun main4() {
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("Started in ctx1")
                    withContext(ctx2) {
                        log("Working in ctx2")
                    }
                    log("Back to ctx1")
                }
            }
        }
    }

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")
    /**
     * 协程可以在一个线程上挂起并在其它线程上恢复。
     * 甚至一个单线程的调度器也是难以弄清楚协程在何时何地正在做什么事情。
     * 使用通常调试应用程序的方法是让线程在每一个日志文件的日志声明中打印线程的名字。
     * 这种特性在日志框架中是普遍受支持的。
     * 但是在使用协程时，单独的线程名称不会给出很多协程上下文信息，所以 kotlinx.coroutines 包含了调试工具来让它更简单。
     */
    fun main3() = runBlocking<Unit> {
        val a = async {
            log("I'm computing a piece of the answer")
            6
        }
        val b = async {
            log("I'm computing another piece of the answer")
            7
        }
        log("The answer is ${a.await() * b.await()}")
    }

    /**
     * 非受限的调度器是一种高级机制，可以在某些极端情况下提供帮助而不需要调度协程以便稍后执行或产生不希望的副作用，
     * 因为某些操作必须立即在协程中执行。 非受限调度器不应该被用在通常的代码中。
     */
    fun main2() = runBlocking<Unit> {
        launch(Dispatchers.Unconfined) {
            //        launch() {
            // 非受限的——将和主线程一起工作
            println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
            delay(500)
            //main 在 delay(1000) 被挂起
            println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
        }
        launch {
            // 父协程的上下文，主 runBlocking 协程
            println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
        }
    }

    fun main1() = runBlocking<Unit> {
        /**
         * 当调用 launch { …… } 时不传参数，它从启动了它的 CoroutineScope 中承袭了上下文（以及调度器）。
         * 在这个案例中，它从 main 线程中的 runBlocking 主协程承袭了上下文。
         */
        launch {
            // 运行在父协程的上下文中，即 runBlocking 主协程
            println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Unconfined) {
            // 不受限的——将工作在主线程中
            println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
        }
        /**
         * 为协程的运行启动了一个线程。
         * 一个专用的线程是一种非常昂贵的资源。
         * 在真实的应用程序中两者都必须被释放，当不再需要的时候，使用 close 函数，或存储在一个顶级变量中使它在整个应用程序中被重用。
         */
        launch(newSingleThreadContext("MyOwnThread")) {
            // 将使它获得一个新的线程
            println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
        }

        launch(Dispatchers.Default) {
            // 将会获取默认调度器
            println("Default               : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Default) {
            // 将会获取默认调度器
            println("Default               : I'm working in thread ${Thread.currentThread().name}")
        }
    }

}