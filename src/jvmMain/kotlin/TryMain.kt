import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.connection.RealConnection
import okio.Sink
import okio.Source
import java.io.*
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

val sourceProperty = RealConnection::class.memberProperties.first { it.name == "source" }.apply { isAccessible = true }
val sinkProperty = RealConnection::class.memberProperties.first { it.name == "sink" }.run {
    val self = this as KMutableProperty<Sink>
    isAccessible = true
    self.setter.isAccessible = true
    self
}

val socketAsyncTimeoutClass = Class.forName("okio.SocketAsyncTimeout").kotlin
val socketAsyncTimeoutConstructor = socketAsyncTimeoutClass.primaryConstructor!!.apply {
    isAccessible = true
}
val socketAsyncTimeoutClassSinkMethod = socketAsyncTimeoutClass.memberFunctions.first { it.name == "sink" }.apply { isAccessible = true }
val outputStreamSinkConstructor = Class.forName("okio.OutputStreamSink").kotlin.primaryConstructor!!.apply {
    isAccessible = true
}
val socketAsyncTimeoutClassSourceMethod = socketAsyncTimeoutClass.memberFunctions.first { it.name == "source" }.apply { isAccessible = true }
val inputStreamSourceConstructor = Class.forName("okio.InputStreamSource").kotlin.primaryConstructor!!.apply {
    isAccessible = true
}

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

fun main() = application {
    test()
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

fun test() {
//    val requestWriterFactory = DefaultHttpRequestWriterFactory
//
//    val connectionFactory = ManagedHttpClientConnectionFactory(
//        Http1Config.DEFAULT,
//        CharCodingConfig.custom().setMalformedInputAction(CodingErrorAction.REPORT).build(),
//
//    )
//
//    val connectManager = PoolingHttpClientConnectionManagerBuilder.create()
//        .setConnectionFactory {
//
//        }
//        .build()

//    val httpClient = HttpClients.createDefault()
//    httpClient.execute(HttpHost("https://www.google.com"), HttpRequest)

    val outgoingBytesChannel = Channel<ByteArray>()
    val incomingBytesChannel = Channel<ByteArray>()

    CoroutineScope(Dispatchers.IO).launch {
        println("b4 collect")
        outgoingBytesChannel.receiveAsFlow()
            .onEach {
                println("Outgoing: " + it.decodeToString())
            }
            .flowOn(Dispatchers.IO)
            .collect()
        println("after collect")
    }

    CoroutineScope(Dispatchers.IO).launch {
        incomingBytesChannel.receiveAsFlow()
//            .buffer(12)
            .onEach {
                println("Incoming: " + it.decodeToString())
            }
            .flowOn(Dispatchers.IO)
            .collect()
    }

    val httpClient = OkHttpClient.Builder()/*.sslSocketFactory()*/
        .protocols(listOf(Protocol.HTTP_1_1))
        .eventListener(object : EventListener() {
            override fun callStart(call: Call) {
                println("event callStart")
            }

            override fun connectionAcquired(call: Call, connection: Connection) {
                println("event connectionAcquired")
//                if (connection is RealConnection) {
//                    val socket = connection.socket()
//
//
//                    val timeout = socketAsyncTimeoutConstructor.call(socket)
//                    val sink = outputStreamSinkConstructor.call(InspectOutputStream(socket.getOutputStream(), outgoingBytesChannel), timeout) as Sink
//                    val socketSink = socketAsyncTimeoutClassSinkMethod.call(timeout, sink) as Sink
//                    sinkProperty.setter.call(connection, socketSink.buffer())
//                }
            }

            override fun connectEnd(
                call: Call,
                inetSocketAddress: InetSocketAddress,
                proxy: Proxy,
                protocol: Protocol?
            ) {
                println("event connectEnd")
            }

            override fun connectFailed(
                call: Call,
                inetSocketAddress: InetSocketAddress,
                proxy: Proxy,
                protocol: Protocol?,
                ioe: IOException
            ) {
                println("event connectFailed")
            }

            override fun secureConnectEnd(call: Call, handshake: Handshake?) {
                println("event secureConnectEnd")
            }
        })
        .socketSourceSinkTransformer(SocketSourceSinkTransformer(
            mapSink = { socket, _ ->
                val timeout = socketAsyncTimeoutConstructor.call(socket)
                val sink = outputStreamSinkConstructor.call(InspectOutputStream(socket.getOutputStream(), outgoingBytesChannel), timeout) as Sink
                socketAsyncTimeoutClassSinkMethod.call(timeout, sink) as Sink
            },

            mapSource = { socket, _ ->
                val timeout = socketAsyncTimeoutConstructor.call(socket)
                val source = inputStreamSourceConstructor.call(InspectInputStream(socket.getInputStream(), incomingBytesChannel), timeout) as Source
                socketAsyncTimeoutClassSourceMethod.call(timeout, source) as Source
            }
        ))
        .build()

    httpClient.newCall(
        Request.Builder()
            .url("https://www.google.com")
            .post("{\"abc\":\"def\"}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
    )
        .execute().use {
            println("resp = ${it.code}; body = ${it.body}")
        }
}

//class Ince : Interceptor {
//    override fun intercept(chain: Interceptor.Chain): Response {
//        chain.request().headers
//        chain.connection()
//        chain.request().body
//    }
//
//}

class InspectOutputStream(val stream: OutputStream, val channel: Channel<ByteArray>) : FilterOutputStream(stream) {
    var ignoringRecursiveCall = 0

    override fun write(b: Int) {
        synchronized(this) {
            if (ignoringRecursiveCall > 0) return
            println("InspectOutputStream write 1")
            ++ignoringRecursiveCall
            super.write(b)
            --ignoringRecursiveCall
            runBlocking {
                channel.send(byteArrayOf(b.toByte()))
            }
        }
    }

    override fun write(b: ByteArray) {
//        synchronized(this) {
//            if (ignoringRecursiveCall > 0) return
            println("InspectOutputStream write 2")
//            ++ignoringRecursiveCall
            super.write(b)
//            --ignoringRecursiveCall
//            runBlocking {
//                channel.send(b)
//            }
//        }
    }
//
    override fun write(b: ByteArray, off: Int, len: Int) {
//        synchronized(this) {
//            if (ignoringRecursiveCall > 0) return
            println("InspectOutputStream write 3")
//            ++ignoringRecursiveCall
            super.write(b, off, len)
//            --ignoringRecursiveCall
//            runBlocking {
//                channel.send(b.copyOfRange(off, off + len))
//            }
//        }
    }
}

class InspectInputStream(val stream: InputStream, val channel: Channel<ByteArray>) : FilterInputStream(stream) {
    override fun read(): Int {
        synchronized(this) {
            println("InspectInputStream read 1")
            val b = super.read()
            runBlocking {
                channel.send(byteArrayOf(b.toByte()))
            }
            return b
        }
    }

    override fun read(b: ByteArray): Int {
        println("InspectInputStream read 2")
        return super.read(b)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        synchronized(this) {
            println("InspectInputStream read 3")
            val a = super.read(b, off, len)
            runBlocking {
                channel.send(b.copyOfRange(off, off + len))
            }
            return a
        }
    }
}
