package hg2g

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import hg2g.api.H2G2Moshi.auto
import hg2g.external.BabelFish
import hg2g.external.EditorialOffice
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.routing.bind
import org.http4k.routing.path
import se.ansman.kotshi.JsonSerializable

enum class Language {
    Universal, EarthSpeak
}

@JsonSerializable
data class Article(val language: Language, val text: String)

private val articleBody = Body.auto<Article>().toLens()

/**
 * Endpoint to accept an article submission which will be translated into the universal
 */
fun SubmitArticle(babelFish: BabelFish, editorialOffice: EditorialOffice) =
    "/submit/{fieldResearcher}" bind POST to { req: Request ->
        val article = articleBody(req)
        babelFish.translate(article.text)
            .map { editorialOffice.submitArticle(req.path("fieldResearcher")!!, article.language, it) }
            .map { Response(ACCEPTED) }
            .mapFailure { Response(BAD_GATEWAY) }
            .get()
    }