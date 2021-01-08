package hg2g

import hg2g.Settings.API_KEY_SECRET_ID
import hg2g.Settings.AWS_ACCOUNT
import hg2g.Settings.AWS_CREDENTIALS
import hg2g.Settings.AWS_REGION
import hg2g.Settings.KMS_URL
import hg2g.Settings.LAMBDA_URL
import hg2g.Settings.SECRETS_MANAGER_URL
import hg2g.Settings.SIGNING_KEY_ID_PARAMETER
import hg2g.Settings.SQS_URL
import hg2g.Settings.SSM_URL
import hg2g.Settings.SUBMISSION_QUEUE
import hg2g.Settings.TRANSLATOR_LAMBDA
import org.http4k.aws.AwsCredentials
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.connect.amazon.AwsServiceCompanion
import org.http4k.connect.amazon.kms.Http
import org.http4k.connect.amazon.kms.KMS
import org.http4k.connect.amazon.lambda.Http
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.model.AwsAccount
import org.http4k.connect.amazon.model.FunctionName
import org.http4k.connect.amazon.model.QueueName
import org.http4k.connect.amazon.model.Region
import org.http4k.connect.amazon.model.SecretId
import org.http4k.connect.amazon.secretsmanager.Http
import org.http4k.connect.amazon.secretsmanager.SecretsManager
import org.http4k.connect.amazon.sqs.Http
import org.http4k.connect.amazon.sqs.SQS
import org.http4k.connect.amazon.systemsmanager.Http
import org.http4k.connect.amazon.systemsmanager.SystemsManager
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.lens.composite
import org.http4k.lens.uri
import org.http4k.routing.routes
import java.time.Clock

object Settings {
    val AWS_CREDENTIALS = EnvironmentKey.composite {
        AwsCredentials(
            EnvironmentKey.required("AWS_ACCESS_KEY_ID")(it),
            EnvironmentKey.required("AWS_SECRET_ACCESS_KEY")(it)
        )
    }
    val AWS_REGION = EnvironmentKey.map(Region::of).required("AWS_REGION")
    val SIGNING_KEY_ID_PARAMETER = EnvironmentKey.required("SIGNING_KEY_ID_PARAMETER")
    val API_KEY_SECRET_ID = EnvironmentKey.map(SecretId::of).required("API_KEY_SECRET_ID")
    val TRANSLATOR_LAMBDA = EnvironmentKey.map(FunctionName::of).required("TRANSLATOR_LAMBDA")
    val AWS_ACCOUNT = EnvironmentKey.map(AwsAccount::of).required("AWS_ACCOUNT")
    val SUBMISSION_QUEUE = EnvironmentKey.map(QueueName::of).required("SUBMISSION_QUEUE")
    val SQS_URL = EnvironmentKey.uri().defaulted("SQS_URL", SQS.awsUrl())
    val LAMBDA_URL = EnvironmentKey.uri().defaulted("LAMBDA_URL", Lambda.awsUrl())
    val SECRETS_MANAGER_URL = EnvironmentKey.uri().defaulted("SECRETS_MANAGER_URL", SecretsManager.awsUrl())
    val KMS_URL = EnvironmentKey.uri().defaulted("KMS_URL", KMS.awsUrl())
    val SSM_URL = EnvironmentKey.uri().defaulted("SSM_URL", SystemsManager.awsUrl())

    private fun AwsServiceCompanion.awsUrl() = EnvironmentKey.composite { awsService.toUri(AWS_REGION(it)) }
}

/**
 * Our main HTTP API.
 */
fun HitchhikersGuideApp(env: Environment, http: HttpHandler, clock: Clock): HttpHandler {
    val kms = KMS.Http(AWS_REGION(env), { AWS_CREDENTIALS(env) }, SetHostFrom(KMS_URL(env)).then(http), clock)
    val ssm = SystemsManager.Http(AWS_REGION(env), { AWS_CREDENTIALS(env) }, SetHostFrom(SSM_URL(env)).then(http), clock)
    val sm = SecretsManager.Http(AWS_REGION(env), { AWS_CREDENTIALS(env) }, SetHostFrom(SECRETS_MANAGER_URL(env)).then(http), clock )
    val sqs = SQS.Http(AWS_REGION(env), { AWS_CREDENTIALS(env) }, SetHostFrom(SQS_URL(env)).then(http), clock)
    val lambda = Lambda.Http(AWS_REGION(env), { AWS_CREDENTIALS(env) }, SetHostFrom(LAMBDA_URL(env)).then(http), clock)

    return ApiKeySecurity(sm, API_KEY_SECRET_ID(env))
        .then(
            routes(
                SubmitArticle(
                    LambdaBabelFish(lambda, TRANSLATOR_LAMBDA(env)),
                    SigningSQSEditor(
                        KmsSigner(kms, ssm, SIGNING_KEY_ID_PARAMETER(env)),
                        sqs, AWS_ACCOUNT(env), SUBMISSION_QUEUE(env)))
            )
        )
}
