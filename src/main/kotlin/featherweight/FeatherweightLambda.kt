package featherweight

import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.serverless.ApiGatewayV2LambdaFunction
import org.http4k.serverless.AppLoader
import java.time.Clock

@Suppress("unused")
class FeatherweightLambda : ApiGatewayV2LambdaFunction(AppLoader {
    FeatherweightApp(Environment.from(it), JavaHttpClient(), Clock.systemDefaultZone())
})
