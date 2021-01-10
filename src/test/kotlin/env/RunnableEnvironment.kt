package env

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import hg2g.HitchhikersGuideApp
import hg2g.Settings.API_KEY_SECRET_ID
import hg2g.Settings.AWS_ACCESS_KEY_ID
import hg2g.Settings.AWS_REGION
import hg2g.Settings.AWS_SECRET_ACCESS_KEY
import hg2g.Settings.DEBUG
import hg2g.Settings.SIGNING_KEY_ID_PARAMETER
import hg2g.Settings.SUBMISSION_QUEUE_ARN
import hg2g.Settings.TRANSLATOR_LAMBDA
import hg2g.api.Language.EarthSpeak
import org.http4k.cloudnative.env.Environment
import org.http4k.connect.amazon.AwsReverseProxy
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
import org.http4k.connect.amazon.systemsmanager.FakeSystemsManager
import org.http4k.connect.amazon.systemsmanager.SystemsManager
import org.http4k.connect.amazon.systemsmanager.putParameter
import java.util.UUID

fun main() {
    val region = Region.of("ldn-north-1")
    val queueName = QueueName.of("editorialQueue")
    val awsAccount = AwsAccount.of("1234567890")
    val secretName = SecretId.of("mysecret")
    val lambdaName = FunctionName("babelfish")
    val apiKey = "secretValue"
    val keyParameter = SSMParameterName.of("myParameter")

    val babelFish = FakeBabelFish()
    val kms = FakeKMS()
    val lambda = FakeLambda(lambdaName to babelFish)
    val sqs = FakeSQS(awsAccount = awsAccount)
    val secretsManager = FakeSecretsManager()
    val systemsManager = FakeSystemsManager()

    // setup environment
    sqs.client().createQueue(queueName, emptyMap(), emptyMap())
    secretsManager.client().createSecret(secretName.value, UUID.randomUUID(), apiKey)
    val keyId = kms.client().createKey(SYMMETRIC_DEFAULT).valueOrNull()!!.KeyMetadata.KeyId
    systemsManager.client().putParameter(keyParameter, keyId.value, ParameterType.String)

    val env = Environment.defaults(
        DEBUG of true,
        AWS_REGION of region,
        AWS_ACCESS_KEY_ID of "accessKeyId",
        AWS_SECRET_ACCESS_KEY of "secretAccessKey",
        API_KEY_SECRET_ID of secretName,
        SIGNING_KEY_ID_PARAMETER of keyParameter,
        TRANSLATOR_LAMBDA of lambdaName,
        SUBMISSION_QUEUE_ARN of ARN.of(SQS.awsService, region, awsAccount, queueName),
    )

    val app = HitchhikersGuideApp(
        env,
        AwsReverseProxy(
            KMS to kms,
            SecretsManager to secretsManager,
            SystemsManager to systemsManager,
            Lambda to lambda,
            SQS to sqs
        )
    )

    FieldResearcher(app, apiKey, "Ford Prefect", EarthSpeak)
        .submitArticle("Earth", "Mostly Harmless")

    GuideEditor(sqs, env).lookAtInbox().map { it.forEach(::println) }
}
