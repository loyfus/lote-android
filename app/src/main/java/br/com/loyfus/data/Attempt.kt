package br.com.loyfus.data

import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * `runCatching` for suspend code: a cancelled coroutine is not a failed request. The standard one
 * swallows the cancellation and would show an error screen for a search the reader had replaced.
 */
suspend inline fun <T> attempt(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/** What to tell the reader about a failed request, in the words of the app. */
fun Throwable.userMessage(): String = when {
    this is ApiException && code >= 500 ->
        "O servidor não respondeu como esperado. Tente de novo em instantes."
    this is ApiException -> "O servidor recusou o pedido (${code}). Tente de novo."
    this is IOException -> "Sem conexão com o servidor. Verifique a rede e tente de novo."
    else -> "Algo deu errado. Tente de novo."
}
