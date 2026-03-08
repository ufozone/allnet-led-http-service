package com.goernhardt.ledhttpservice

import fi.iki.elonen.NanoHTTPD

/**
 * NanoHTTPD implementation for the LED control API.
 */
class LedHttpServer(port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val params = session.parms

        return when {
            uri == "/health" -> jsonResponse("""{"success": true}""")
            
            uri == "/status" -> jsonResponse("""{
                "success": true,
                "service": "allnet-led-http",
                "lastMode": "${LedController.getLastMode()}"
            }""")
            
            uri == "/wake" -> {
                WakeHelper.wakeDevice()
                jsonResponse("""{"success": true, "action": "wake"}""")
            }
            
            uri.startsWith("/led") -> handleLedCommand(uri, params)
            
            uri == "/modes" -> {
                val modes = LedController.getAvailableModes().joinToString("\", \"")
                jsonResponse("""{"modes": ["$modes"]}""")
            }
            
            uri == "/help" -> jsonResponse("""{
                "endpoints": [
                    "/health", 
                    "/status", 
                    "/wake", 
                    "/led?mode={name}", 
                    "/led/{name}", 
                    "/modes", 
                    "/help"
                ]
            }""")
            
            uri == "/" -> generateHtmlDashboard()
            
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }

    private fun handleLedCommand(uri: String, params: Map<String, String>): Response {
        // Support /led?mode=red or /led/red
        val input = params["mode"] ?: uri.substringAfterLast("/led/").takeIf { it.isNotEmpty() }
        
        if (input == null) {
            return jsonResponse("""{"success": false, "message": "Missing 'mode' parameter"}""", Response.Status.BAD_REQUEST)
        }

        val success = LedController.setLed(input)
        return if (success) {
            jsonResponse("""{"success": true, "mode": "$input"}""")
        } else {
            jsonResponse("""{"success": false, "message": "Failed to set LED for mode: $input"}""", Response.Status.INTERNAL_ERROR)
        }
    }

    private fun generateHtmlDashboard(): Response {
        val modeLinks = LedController.getAvailableModes().joinToString("<br>") { mode ->
            "<a href='/led/$mode' style='font-size: 1.2em; text-decoration: none; color: #007bff;'>$mode</a>"
        }
        
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>LED Control Dashboard</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body { font-family: sans-serif; padding: 20px; line-height: 1.6; }
                    .container { max-width: 600px; margin: auto; }
                    h1 { color: #333; }
                    .modes { columns: 2; -webkit-columns: 2; -moz-columns: 2; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>LED Control Panel</h1>
                    <p>Status: <a href="/status">View JSON Status</a></p>
                    <div class="modes">
                        $modeLinks
                    </div>
                    <hr>
                    <p><a href="/wake">Wake Device</a></p>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun jsonResponse(json: String, status: Response.IStatus = Response.Status.OK): Response {
        return newFixedLengthResponse(status, "application/json", json)
    }
}
