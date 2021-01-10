package env

import hg2g.Settings.AWS_CREDENTIALS
import hg2g.Settings.AWS_REGION
import hg2g.Settings.SUBMISSION_QUEUE_ARN
import hg2g.api.Language
import org.http4k.cloudnative.env.Environment
import org.http4k.connect.amazon.sqs.Http
import org.http4k.connect.amazon.sqs.SQS
import org.http4k.connect.amazon.sqs.receiveMessage
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import java.time.Clock

/**
 * The Guide Editor receives submitted articles into their inbox.
 */
class GuideEditor(
    http: HttpHandler,
    private val env: Environment,
    clock: Clock = Clock.systemDefaultZone()
) {
    private val sqs = SQS.Http(
        AWS_REGION(env),
        { AWS_CREDENTIALS(env) }, http, clock
    )

    fun lookAtInbox() = sqs.receiveMessage(SUBMISSION_QUEUE_ARN(env))
}

/**
 * The Field Researcher submits articles to the Guide.
 */
class FieldResearcher(
    private val http: HttpHandler,
    private val apiKey: String,
    private val researcherName: String,
    private val spokenLanguage: Language,
) {
    fun submitArticle(title: String, text: String) = http(
        Request(POST, "/submit/$researcherName")
            .header("api-key", apiKey)
            .body(
                """
                {
                    "language": "$spokenLanguage",
                    "text": "$title: $text"
                }
            """.trimIndent()
            )
    )
}