package hg2g.external

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.lambda.action.invokeFunction
import org.http4k.connect.amazon.model.FunctionName
import se.ansman.kotshi.JsonSerializable

/**
 * The BabelFish translates any incoming article into the universal language of the guide.
 */
fun interface BabelFish {
    fun translate(source: String): Result<String, RemoteFailure>
}

/**
 * BabelFish deployed into AWS Lambda.
 */
fun LambdaBabelFish(functionName: FunctionName, lambda: Lambda) = BabelFish { message ->
    lambda.invokeFunction<TranslationText>(functionName, TranslationText(message))
        .map { it.text }
}

@JsonSerializable
data class TranslationText(val text: String)
