package env

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import hg2g.HitchhikersGuideApp
import hg2g.Settings.API_KEY_SECRET_ID
import hg2g.Settings.AWS_ACCESS_KEY_ID
import hg2g.Settings.AWS_REGION
import hg2g.Settings.AWS_SECRET_ACCESS_KEY
import hg2g.Settings.SIGNING_KEY_ID_PARAMETER
import hg2g.Settings.SUBMISSION_QUEUE_ARN
import hg2g.Settings.TRANSLATOR_LAMBDA
import org.http4k.cloudnative.env.Environment
import org.http4k.connect.amazon.kms.FakeKMS
import org.http4k.connect.amazon.kms.KMS
import org.http4k.connect.amazon.kms.createKey
import org.http4k.connect.amazon.lambda.FakeLambda
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.model.ARN
import org.http4k.connect.amazon.model.AwsAccount
import org.http4k.connect.amazon.model.CustomerMasterKeySpec.SYMMETRIC_DEFAULT
import org.http4k.connect.amazon.model.FunctionName
import org.http4k.connect.amazon.model.ParameterType
import org.http4k.connect.amazon.model.QueueName
import org.http4k.connect.amazon.model.Region
import org.http4k.connect.amazon.model.SSMParameterName
import org.http4k.connect.amazon.model.SecretId
import org.http4k.connect.amazon.secretsmanager.FakeSecretsManager
import org.http4k.connect.amazon.secretsmanager.SecretsManager
import org.http4k.connect.amazon.secretsmanager.createSecret
import org.http4k.connect.amazon.sqs.FakeSQS
import org.http4k.connect.amazon.sqs.SQS
import org.http4k.connect.amazon.sqs.createQueue
import org.http4k.connect.amazon.sqs.receiveMessage
import org.http4k.connect.amazon.systemsmanager.FakeSystemsManager
import org.http4k.connect.amazon.systemsmanager.SystemsManager
import org.http4k.connect.amazon.systemsmanager.putParameter
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.filter.debug
import java.util.UUID

fun main() {

    val region = Region.of("ldn-north-1")
    val queueName = QueueName.of("editorialQueue")
    val awsAccount = AwsAccount.of("1234567890")
    val secretName = SecretId.of("mysecret")
    val lambdaName = FunctionName("babelfish")
    val apiKey = "secretValue"
    val keyParameter = SSMParameterName.of("myParameter")

    val sqs = FakeSQS(awsAccount = awsAccount)
    val kms = FakeKMS()
    val lambda = FakeLambda(lambdaName to { Response(Status.OK).body("""{"text":"hello"}""") })
    val systemsManager = FakeSystemsManager()
    val secretsManager = FakeSecretsManager()

    // setup environment
    sqs.client().createQueue(queueName, emptyMap(), emptyMap())
    secretsManager.client().createSecret(secretName.value, UUID.randomUUID(), apiKey)
    val keyId = kms.client().createKey(SYMMETRIC_DEFAULT).valueOrNull()!!.KeyMetadata.KeyId
    systemsManager.client().putParameter(keyParameter, keyId.value, ParameterType.String)

    val queueArn = ARN.of(SQS.awsService, region, awsAccount, queueName)

    val env = Environment.defaults(
        AWS_REGION of region,
        AWS_ACCESS_KEY_ID of "accessKeyId",
        AWS_SECRET_ACCESS_KEY of "secretAccessKey",
        API_KEY_SECRET_ID of secretName,
        SIGNING_KEY_ID_PARAMETER of keyParameter,
        TRANSLATOR_LAMBDA of lambdaName,
        SUBMISSION_QUEUE_ARN of queueArn,
    )

    val http = AwsReverseProxy(
        KMS to kms,
        SecretsManager to secretsManager,
        SystemsManager to systemsManager,
        Lambda to lambda,
        SQS to sqs
    ).debug()
    val app = HitchhikersGuideApp(env, http)

    app.debug()(
        Request(POST, "/submit/bob")
            .header("Api-key", apiKey)
            .body("""
                {
                    "language": "Universal",
                    "text": "Earth: Mostly Harmless"
                }
            """.trimIndent())
    )

    // print editorial queue messages
    sqs.client().receiveMessage(queueArn).map { it.forEach { println(it) } }
}
