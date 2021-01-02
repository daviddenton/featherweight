package featherweight

import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment.Companion.ENV
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Clock

fun main() {
    FeatherweightApp(ENV, JavaHttpClient(), Clock.systemDefaultZone()).asServer(SunHttp(8080)).start()
}
