package com.crgarridos.websocket

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.tinder.scarlet.Event
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.State
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient


const val GREEN = 0xFF008000.toInt()

class MainActivity : AppCompatActivity() {

    val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val httpClient = OkHttpClient()

        val scarlet = Scarlet.Builder()
            .webSocketFactory(httpClient.newWebSocketFactory("wss://echo.websocket.org/"))
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .backoffStrategy(LinearBackoffStrategy(1000))
            .lifecycle(AndroidLifecycle.ofApplicationForeground(application))
            .build()

        val service = scarlet.create(EchoService::class.java)

        submitButton.setOnClickListener {
            val input = editText.text.toString()
            val isSent = service.sendText(input)
            val spanColor = if (isSent) Color.BLUE else Color.RED
            val prefix = if (isSent) "Sent: " else "Unable to send: "

            textView.appendln(spanColor, "$prefix$input")
        }

        disposables += service.observeText()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                textView.appendln(GREEN, "Received: $it")
            }, {
                textView.appendln(Color.RED, "Unable to receive message.")
            })

        disposables += service.observeState()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                textView.appendln(GREEN, "State: ${it::class.java.simpleName}")
            }, {
                textView.appendln(Color.RED, "Unable to receive state.")
            })

        disposables += service.observeEvent()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                textView.appendln(GREEN, "Event: ${it::class.java.simpleName}")
            }, {
                textView.appendln(Color.RED, "Unable to receive event.")
            })

    }


    private fun TextView.appendln(spanColor: Int, message: String) {
        append(buildSpannedString {
            color(spanColor) {
                append("$message\n")
            }
        })
        scrollView.fullScroll(View.FOCUS_DOWN)
    }

}

private operator fun CompositeDisposable.plusAssign(subscribe: Disposable) {
    add(subscribe)
}

interface EchoService {
    @Receive
    fun observeState(): Flowable<State>

    @Receive
    fun observeEvent(): Flowable<Event>

    @Receive
    fun observeText(): Flowable<String>

    @Send
    fun sendText(text: String): Boolean
}