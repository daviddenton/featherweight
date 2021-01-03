package env

import dev.forkhandles.result4k.get
import hg2g.HitchhikersGuideApp
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.connect.amazon.kms.FakeKMS
import org.http4k.connect.amazon.kms.createKey
import org.http4k.connect.amazon.lambda.FakeLambda
import org.http4k.connect.amazon.model.BucketName
import org.http4k.connect.amazon.model.CustomerMasterKeySpec.SYMMETRIC_DEFAULT
import org.http4k.connect.amazon.model.ParameterType
import org.http4k.connect.amazon.model.Region
import org.http4k.connect.amazon.s3.FakeS3
import org.http4k.connect.amazon.s3.createBucket
import org.http4k.connect.amazon.secretsmanager.FakeSecretsManager
import org.http4k.connect.amazon.secretsmanager.createSecret
import org.http4k.connect.amazon.systemsmanager.FakeSystemsManager
import org.http4k.connect.amazon.systemsmanager.putParameter
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Clock
import java.util.UUID

fun main() {
    val clock = Clock.systemDefaultZone()

    val region = Region.of("ldn-north-1")

    FakeS3(clock = clock).apply {
        s3Client().createBucket(BucketName.of("mybucket"), region)
    }.start()

    FakeKMS(clock = clock).apply {
        println(client().createKey(SYMMETRIC_DEFAULT).get())
    }.start()
    FakeLambda(clock = clock).start()
    FakeSecretsManager(clock = clock).apply {
        println(client().createSecret("mysecret", UUID.randomUUID(), "secretValue").get())
    }.start()
    FakeSystemsManager(clock = clock).apply {
        println(client().putParameter("myParameter", "myParamValue", ParameterType.String).get())
    }.start()

    val env = Environment.from(
        "AWS_REGION" to region.value,
        "AWS_ACCESS_KEY_ID" to "accessKeyId",
        "AWS_SECRET_ACCESS_KEY" to "secretAccessKey"
    )
    HitchhikersGuideApp(env, JavaHttpClient(), clock).asServer(SunHttp(8080)).start()
}