package com.allnet.ledhttpservice

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Structured result of a shell command execution.
 */
data class ShellResult(
    val success: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)

/**
 * Robust utility for executing root commands via 'su'.
 */
object ShellExecutor {
    private const val TAG = "ShellExecutor"

    /**
     * Executes a command with root privileges and captures output.
     * @param command The shell command to execute.
     * @return A [ShellResult] containing exit code and output streams.
     */
    fun executeRootCommand(command: String): ShellResult {
        var process: Process? = null
        var os: DataOutputStream? = null
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)

            // Write command and exit signal
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            // Read stdout and stderr in parallel-ish (sequentially here as su usually flushes on exit)
            val stdInput = BufferedReader(InputStreamReader(process.inputStream))
            val stdError = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (stdInput.readLine().also { line = it } != null) {
                stdoutBuilder.append(line).append("\n")
            }
            while (stdError.readLine().also { line = it } != null) {
                stderrBuilder.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            val stdout = stdoutBuilder.toString().trim()
            val stderr = stderrBuilder.toString().trim()

            ShellResult(
                success = exitCode == 0,
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr
            ).also {
                if (!it.success) {
                    Log.e(TAG, "Command failed: $command\nExitCode: $exitCode\nStderr: $stderr")
                }
            }

        } catch (e: IOException) {
            Log.e(TAG, "IOException executing root command: ${e.message}")
            ShellResult(false, -1, "", e.message ?: "IOException")
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted executing root command: ${e.message}")
            Thread.currentThread().interrupt()
            ShellResult(false, -1, "", e.message ?: "InterruptedException")
        } finally {
            try {
                os?.close()
                process?.destroy()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
