package hg2g

import hg2g.Settings.API_KEY_SECRET_ID
import hg2g.Settings.AWS_CREDENTIALS
import hg2g.Settings.AWS_REGION
import hg2g.Settings.SIGNING_KEY_ID_PARAMETER
import hg2g.Settings.SUBMISSION_QUEUE_ARN
import hg2g.Settings.TRANSLATOR_LAMBDA
import hg2g.api.ApiKeySecurity
import hg2g.api.SubmitArticle
import hg2g.external.KmsSigner
import hg2g.external.LambdaBabelFish
import hg2g.external.SigningSQSEditor
import org.http4k.aws.AwsCredentials
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.connect.amazon.kms.Http
import org.http4k.connect.amazon.kms.KMS
import org.http4k.connect.amazon.lambda.Http
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.model.ARN
import org.http4k.connect.amazon.model.FunctionName
import org.http4k.connect.amazon.model.Region
import org.http4k.connect.amazon.model.SSMParameterName
import org.http4k.connect.amazon.model.SecretId
import org.http4k.connect.amazon.secretsmanager.Http
import org.http4k.connect.amazon.secretsmanager.SecretsManager
import org.http4k.connect.amazon.sqs.Http
import org.http4k.connect.amazon.sqs.SQS
import org.http4k.connect.amazon.systemsmanager.Http
import org.http4k.connect.amazon.systemsmanager.SystemsManager
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.composite
import org.http4k.routing.routes
import java.time.Clock

object Settings {
    val AWS_REGION = EnvironmentKey.map(Region::of, Region::value).required("AWS_REGION")
    val AWS_ACCESS_KEY_ID = EnvironmentKey.required("AWS_ACCESS_KEY_ID")
    val AWS_SECRET_ACCESS_KEY = EnvironmentKey.required("AWS_SECRET_ACCESS_KEY")
    val AWS_CREDENTIALS = EnvironmentKey.composite {
        AwsCredentials(AWS_ACCESS_KEY_ID(it), AWS_SECRET_ACCESS_KEY(it))
    }
    val SIGNING_KEY_ID_PARAMETER = EnvironmentKey.map(SSMParameterName::of, SSMParameterName::value).required("SIGNING_KEY_ID_PARAMETER")
    val API_KEY_SECRET_ID = EnvironmentKey.map(SecretId::of, SecretId::value).required("API_KEY_SECRET_ID")
    val TRANSLATOR_LAMBDA = EnvironmentKey.map(FunctionName::of, FunctionName::value).required("TRANSLATOR_LAMBDA")
    val SUBMISSION_QUEUE_ARN = EnvironmentKey.map(ARN::of, ARN::value).required("SUBMISSION_QUEUE_ARN")
}

/**
 * Our main HTTP API.
 */
fun HitchhikersGuideApp(env: Environment, http: HttpHandler, clock: Clock = Clock.systemDefaultZone()): HttpHandler {
    val awsCredentials = { AWS_CREDENTIALS(env) }
    val region = AWS_REGION(env)

    val kms = KMS.Http(region, awsCredentials, http, clock)
    val ssm = SystemsManager.Http(region, awsCredentials, http, clock)
    val sm = SecretsManager.Http(region, awsCredentials, http, clock)
    val sqs = SQS.Http(region, awsCredentials, http, clock)
    val lambda = Lambda.Http(region, awsCredentials, http, clock)

    return CatchAll()
        .then(CatchLensFailure())
        .then(ApiKeySecurity(API_KEY_SECRET_ID(env), sm))
        .then(
            routes(
                SubmitArticle(
                    LambdaBabelFish(TRANSLATOR_LAMBDA(env), lambda),
                    SigningSQSEditor(SUBMISSION_QUEUE_ARN(env), sqs, KmsSigner(kms, ssm, SIGNING_KEY_ID_PARAMETER(env))))
            )
        )
}
