package net.example.online.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ParseException
import android.widget.Toast
import androidx.core.net.MailTo
import androidx.core.net.toUri
import net.example.online.ContextHelper

actual object ActivityUtils {

    private val context: Context
        get() = ContextHelper.context ?: error("Context is not initialized")

    // http://sberpay://invoicing/v2?bankInvoiceId=dce389134...ddd47&operationType=Web2App"
    actual fun openBrowser(data: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, data.toUri())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Browser not found", Toast.LENGTH_SHORT).show()
        }
    }

    actual fun startEmailActivity(email: String, subject: String?, text: String?) {
        try {
            val builder = "mailto:$email"
            val intent = Intent(Intent.ACTION_SENDTO, builder.toUri())
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "can't start activity: $text", Toast.LENGTH_LONG).show()
        }
    }

    actual fun startCallActivity(url: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, url.toUri())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "can't start activity: $url", Toast.LENGTH_LONG).show()
        }
    }

    actual fun startSmsActivity(url: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, url.toUri())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "can't start activity: $url", Toast.LENGTH_LONG).show()
        }
    }

    actual fun startMapSearchActivity(url: String) {
        starDefault(url)
    }

    actual fun startMapYandex(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "can't start activity: $url", Toast.LENGTH_LONG).show()
        }
    }

    actual fun startyandexnavi(url: String) {
        starDefault(url)
    }

    actual fun starDefault(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "can't start activity: $url", Toast.LENGTH_LONG).show()
        }
    }

    actual fun startShareActivity(subject: String?, text: String) {
        val activity = context as? Activity ?: return
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, text)
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, "can't start activity: $text", Toast.LENGTH_LONG).show()
        }
    }

    actual fun starttg(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "can't start activity: $url", Toast.LENGTH_LONG).show()
        }
    }

    actual fun startViber(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=com.viber.voip".toUri()
                )
            )
        }
    }

    actual fun sendWhatsappText(url: String) {
        val uri = url.toUri()
        val msg = uri.getQueryParameter("text")
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, msg)
        sendIntent.type = "text/plain"
        sendIntent.setPackage("com.whatsapp")
        try {
            context.startActivity(sendIntent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=com.whatsapp".toUri()
                )
            )
        }
    }

    actual fun sendWhatsappPhone(url: String) {

        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.addFlags(
            Intent.FLAG_ACTIVITY_FORWARD_RESULT
                    or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                    or Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
        )
            .setPackage("com.whatsapp")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=com.whatsapp".toUri()
                )
            )
        }
    }

    actual fun mailTo(url: String) {
        try {
            val mailTo = MailTo.parse(url)
            startEmailActivity(
                mailTo.to ?: "", mailTo.subject, mailTo.body
            )
        } catch (ignored: ParseException) {
        }
    }
}
