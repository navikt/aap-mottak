package mottak.http

import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import mottak.SECURE_LOG

suspend inline fun <reified T : Any> HttpResponse.tryInto(): T {
    when (status.value) {
        in 200..299 -> return body<T>()
        in 400..499 -> throw logWithError("Client error")
        in 500..599 -> throw logWithError("Server error")
        else -> throw logWithError("Unknown error")
    }
}

fun HttpResponse.logWithError(msg: String): IllegalStateException {
    SECURE_LOG.error(
        """
            $msg
            Request: ${request.method.value} ${request.url}
            Response: $status
            Headers: $headers
            Body: ${runBlocking { bodyAsText() }}
        """.trimIndent()
    )

    return IllegalStateException(
        """
        $msg
        Request: ${request.method.value} ${request.url}
        Response: $status
        Headers: $headers
    """.trimIndent()
    )
}