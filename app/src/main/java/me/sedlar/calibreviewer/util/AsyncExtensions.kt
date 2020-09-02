package me.sedlar.calibreviewer.util

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

fun <T> List<Callable<T?>>.await(nThreads: Int): List<Future<T?>> {
    val service = Executors.newFixedThreadPool(nThreads)

    val result = service.invokeAll(this)
    service.shutdown()

    return result
}