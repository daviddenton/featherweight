package env

import featherweight.FeatherweightApp
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.connect.amazon.kms.FakeKMS
import org.http4k.connect.amazon.lambda.FakeLambda
import org.http4k.connect.amazon.s3.FakeS3
import org.http4k.connect.amazon.secretsmanager.FakeSecretsManager
import org.http4k.connect.amazon.systemsmanager.FakeSystemsManager
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Clock

fun main() {
    val clock = Clock.systemDefaultZone()

    FakeS3(clock = clock).start()
    FakeKMS(clock = clock).start()
    FakeLambda(clock = clock).start()
    FakeSecretsManager(clock = clock).start()
    FakeSystemsManager(clock = clock).start()

    val env = Environment.from(
        "AWS_REGION" to "ldn-north-1",
        "AWS_ACCESS_KEY_ID" to "accessKeyId",
        "AWS_SECRET_ACCESS_KEY" to "secretAccessKey"
    )
    FeatherweightApp(env, JavaHttpClient(), clock).asServer(SunHttp(8080)).start()
}