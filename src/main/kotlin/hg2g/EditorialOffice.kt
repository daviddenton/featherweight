package hg2g

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.model.AwsAccount
import org.http4k.connect.amazon.model.QueueName
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
 * We need to cryptographically sign the contents with the researcher's name.
 */
fun SigningSQSEditor(
    signer: Signer,
    sqs: SQS,
    awsAccount: AwsAccount,
    submissionQueueName: QueueName) = EditorialOffice { researcherName, language, article ->
    signer.sign(contentToSign(researcherName, language, article))
        .flatMap { signature ->
            sqs.sendMessage(awsAccount, submissionQueueName,
                article,
                attributes = listOf(
                    MessageAttribute("signature", signature, "String"),
                    MessageAttribute("researcher", researcherName, "String")
                )
            )
        }
        .map { Unit }
}

private fun contentToSign(researcherName: String, language: Language, article: String) =
    researcherName + ":" + language + ":" + article.length
