package code

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

object 共享 {
    @JvmStatic
    fun main(args: Array<String>) {
        main()
    }

    /**
     * 最终值不是真实的执行次数
     */
    //1.加 Volatile 注解
    //这段代码运行速度更慢了,counter 的结果稍微接近 真实值一点
    @Volatile // 在 Kotlin 中 `volatile` 是一个注解
//    var counter = 0
    //2.
//    var counter = 0
    //3.
    var counter = AtomicInteger()
    fun main() = runBlocking {
        withContext(Dispatchers.Default) {
            massiveRun {
//                counter++
                counter.incrementAndGet()
            }
        }
        println("Counter = $counter")
    }

    suspend fun massiveRun(action: suspend () -> Unit) {
        val n = 100  // 启动的协程数量
        val k = 1000 // 每个协程重复执行同一动作的次数
        val time = measureTimeMillis {
            coroutineScope {
                // 协程的作用域
                repeat(n) {
                    launch {
                        repeat(k) { action() }
                    }
                }
            }
        }
        println("Completed ${n * k} actions in $time ms")
    }

}