package env

import dev.forkhandles.result4k.valueOrNull
import hg2g.HitchhikersGuideApp
import org.http4k.cloudnative.env.Environment
import org.http4k.connect.amazon.AwsServiceCompanion
import org.http4k.connect.amazon.kms.FakeKMS
import org.http4k.connect.amazon.kms.KMS
import org.http4k.connect.amazon.kms.createKey
import org.http4k.connect.amazon.lambda.FakeLambda
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.model.AwsAccount
import org.http4k.connect.amazon.model.CustomerMasterKeySpec.SYMMETRIC_DEFAULT
import org.http4k.connect.amazon.model.FunctionName
import org.http4k.connect.amazon.model.ParameterType
import org.http4k.connect.amazon.model.QueueName
import org.http4k.connect.amazon.model.Region
import org.http4k.connect.amazon.secretsmanager.FakeSecretsManager
import org.http4k.connect.amazon.secretsmanager.SecretsManager
import org.http4k.connect.amazon.secretsmanager.createSecret
import org.http4k.connect.amazon.sqs.FakeSQS
import org.http4k.connect.amazon.sqs.SQS
import org.http4k.connect.amazon.sqs.createQueue
import org.http4k.connect.amazon.systemsmanager.FakeSystemsManager
import org.http4k.connect.amazon.systemsmanager.SystemsManager
import org.http4k.connect.amazon.systemsmanager.putParameter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.filter.debug
import org.http4k.routing.bind
import org.http4k.routing.header
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Clock
import java.util.UUID

fun main() {
    val clock = Clock.systemDefaultZone()

    val region = Region.of("ldn-north-1")

    val queueName = QueueName.of("editorialQueue")
    val awsAccount = AwsAccount.of("1234567890")
    val secretName = "mysecret"
    val lambdaName = FunctionName("babelfish")

    val sqs = FakeSQS(clock = clock, awsAccount = awsAccount)
    val kms = FakeKMS(clock = clock)
    val lambda = FakeLambda(lambdaName to { _: Request -> Response(Status.OK) }, clock = clock)
    val systemsManager = FakeSystemsManager(clock = clock)
    val secretsManager = FakeSecretsManager(clock = clock)

    // setup infra
    sqs.client().createQueue(queueName, emptyMap(), emptyMap())
    val keyId = kms.client().createKey(SYMMETRIC_DEFAULT).valueOrNull()!!.KeyMetadata.KeyId
    secretsManager.client().createSecret(secretName, UUID.randomUUID(), "secretValue")
    systemsManager.client().putParameter("myParameter", "myParamValue", ParameterType.String)

    val env = Environment.from(
        "AWS_REGION" to region.value,
        "AWS_ACCESS_KEY_ID" to "accessKeyId",
        "AWS_SECRET_ACCESS_KEY" to "secretAccessKey",
        "SUBMISSION_QUEUE" to queueName.value,
        "AWS_ACCOUNT" to awsAccount.value,
        "API_KEY_SECRET_ID" to secretName,
        "SIGNING_KEY_ID_PARAMETER" to keyId.value,
        "TRANSLATOR_LAMBDA" to lambdaName.value,
    )

    val http = Hosts(
        KMS to kms,
        SecretsManager to secretsManager,
        SystemsManager to systemsManager,
        Lambda to lambda,
        SQS to sqs
    ).debug()
    HitchhikersGuideApp(env, http, clock).asServer(SunHttp(8080))
}

fun Hosts(vararg services: Pair<AwsServiceCompanion, HttpHandler>) = routes(
    *services.map { service ->
        header("host") { it.contains(service.first.awsService.value) } bind service.second
    }.toTypedArray()
)
