package com.yuzheng.kairoweather.data

import kotlinx.coroutines.CancellationException

/**
 * 安全捕获:行为与 [kotlin.Result.runCatching] 一致,但会把 [CancellationException] 原样抛出,
 * 避免协程取消信号被吞掉,导致外层结构化并发(structured concurrency)失效。
 * 仅捕获普通 [Exception],不捕获 [Error]。
 */
internal inline fun <T> resultCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
