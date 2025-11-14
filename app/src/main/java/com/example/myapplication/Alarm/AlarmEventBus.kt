package com.example.myapplication.Alarm

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AlarmEventBus {
    // SharedFlow는 이벤트가 발생했을 때 여러 옵저버에게 전달하는 데 적합합니다.
    private val _events = MutableSharedFlow<String>(
        replay = 0, // 이전에 발생한 이벤트는 재전송하지 않음
        extraBufferCapacity = 1
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    suspend fun emitAlarmEvent(timeString: String) {
        _events.emit(timeString)
    }
}