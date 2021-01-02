package featherweight

import featherweight.Settings.AWS_CREDEMTIALS
import featherweight.Settings.AWS_REGION
import org.http4k.aws.AwsCredentials
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.connect.amazon.kms.Http
import org.http4k.connect.amazon.kms.KMS
import org.http4k.connect.amazon.lambda.Http
import org.http4k.connect.amazon.lambda.Lambda
import org.http4k.connect.amazon.model.Region
import org.http4k.connect.amazon.s3.Http
import org.http4k.connect.amazon.s3.S3
import org.http4k.connect.amazon.secretsmanager.Http
import org.http4k.connect.amazon.secretsmanager.SecretsManager
import org.http4k.connect.amazon.systemsmanager.Http
import org.http4k.connect.amazon.systemsmanager.SystemsManager
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.composite
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

object Settings {
    val AWS_CREDEMTIALS = EnvironmentKey.composite {
        AwsCredentials(
            EnvironmentKey.required("AWS_ACCESS_KEY_ID")(it),
            EnvironmentKey.required("AWS_SECRET_ACCESS_KEY")(it)
        )
    }
    val AWS_REGION = EnvironmentKey.map(Region::of).required("AWS_REGION")
}

fun FeatherweightApp(env: Environment, http: HttpHandler, clock: Clock): HttpHandler {
    val s3 = S3.Http(AWS_REGION(env), { AWS_CREDEMTIALS(env) }, http, clock)
    val kms = KMS.Http(AWS_REGION(env), { AWS_CREDEMTIALS(env) }, http, clock)
    val lambda = Lambda.Http(AWS_REGION(env), { AWS_CREDEMTIALS(env) }, http, clock)
    val secretsManager = SecretsManager.Http(AWS_REGION(env), { AWS_CREDEMTIALS(env) }, http, clock)
    val systemsManager = SystemsManager.Http(AWS_REGION(env), { AWS_CREDEMTIALS(env) }, http, clock)

    return routes(
        "/app/{name}" bind POST to { req: Request ->
            Response(OK)
        },
        "/" bind GET to { req: Request -> Response(OK).body("hello world") }
    )
}

