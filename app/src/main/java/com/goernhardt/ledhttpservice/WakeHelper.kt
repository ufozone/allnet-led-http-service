package com.goernhardt.ledhttpservice

object WakeHelper {
    fun wakeDevice() {
        ShellExecutor.executeRootCommand("input keyevent KEYCODE_WAKEUP")
    }
}
