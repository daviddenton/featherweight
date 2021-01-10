package hg2g.external

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import hg2g.api.Language
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.model.ARN
import org.http4k.connect.amazon.sqs.SQS
import org.http4k.connect.amazon.sqs.action.MessageAttribute
import org.http4k.connect.amazon.sqs.sendMessage

/**
 * The EditorialOffice accepts translated article for publishing.
 */
fun interface EditorialOffice {
    fun submitArticle(researcherName: String, language: Language, article: String): Result<Unit, RemoteFailure>
}

/**
 * SQS implementation of the EditorialOffice.
 * We need to cryptographically sign the contents with the researcher's name and language.
 */
fun SigningSQSEditor(
    queueArn: ARN,
    sqs: SQS,
    signer: Signer
) = EditorialOffice { researcherName, language, article ->
    signer.sign(contentToSign(researcherName, language, article))
        .flatMap { signature ->
            sqs.sendMessage(
                queueArn,
                article,
                attributes = listOf(
                    MessageAttribute("signature", signature, "String"),
                    MessageAttribute("researcher", researcherName, "String"),
                    MessageAttribute("language", language.name, "String")
                )
            )
        }
        .map { }
}

private fun contentToSign(researcherName: String, language: Language, article: String) =
    researcherName + ":" + language + ":" + article.length
