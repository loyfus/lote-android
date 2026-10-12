package br.com.lote.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

// What leaves the app. Every one is a plain system intent started from the activity, so
// the reader's own browser, mail client, dialler and share sheet do the work, in the
// same task: Back returns here. A device with nothing to handle one of them ignores the
// tap instead of crashing.

private fun Context.start(intent: Intent) {
    runCatching { startActivity(intent) }
}

/** The bid itself always happens at the auctioneer, never inside the app: the source opens in the browser. */
fun Context.openUrl(url: String) = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

fun Context.writeTo(email: String) = start(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))

fun Context.dial(phone: String) =
    start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))

/** The system share sheet with the lot's title and link in one line: "title — link". */
fun Context.share(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    start(Intent.createChooser(send, null))
}
