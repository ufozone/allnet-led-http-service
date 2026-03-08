package com.allnet.ledhttpservice

import java.io.DataOutputStream
import java.io.IOException

object ShellExecutor {
    fun executeRootCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Thread.currentThread().interrupt()
            false
        }
    }
}
