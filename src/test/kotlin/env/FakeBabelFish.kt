package env

import hg2g.H2G2Moshi.auto
import hg2g.external.TranslationText
import org.http4k.connect.ChaosFake
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with

/**
 * Simple BabelFish which reverses the input text. It extends ChaosFake
 * so it is possible to either start it up locally or enable chaotic behaviour
 * using the code API or HTTP interface.
 */
class FakeBabelFish : ChaosFake() {
    private val body = Body.auto<TranslationText>().toLens()

    override val app: HttpHandler = {
        val input = body(it).text
        Response(OK).with(body of TranslationText(input.reversed()))
    }
}

fun main() {
    FakeBabelFish().start()
}