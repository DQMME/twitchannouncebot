package de.dqmme.twitchannouncebot.util

import com.netflix.hystrix.HystrixCommand
import hu.akarnokd.rxjava.interop.RxJavaInterop
import kotlinx.coroutines.rx2.awaitFirst

@Suppress("UnstableApiUsage")
suspend fun <R> HystrixCommand<R>.executeAsync(): R {
    val v1Observable = observe()
    val v2Observable = RxJavaInterop.toV2Observable(v1Observable)

    return v2Observable.awaitFirst()
}
