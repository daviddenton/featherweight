package hg2g

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.format.Moshi.auto
import org.http4k.routing.bind
import org.http4k.routing.path

data class Article(
    val language: String,
    val value: String)

private val body = Body.auto<Article>().toLens()

/**
 * Endpoint to accept an article submission which will be translated into the universal
 */
fun SubmitArticle(babelFish: BabelFish, editorialOffice: EditorialOffice) =
    "/submit/{name}" bind POST to { req: Request ->
        babelFish.translate(body(req).value)
            .map { editorialOffice.submitArticle(req.path("name")!!, it) }
            .map { Response(ACCEPTED) }
            .mapFailure { Response(BAD_GATEWAY) }
            .get()
    }