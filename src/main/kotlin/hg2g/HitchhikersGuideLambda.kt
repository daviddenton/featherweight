package hg2g

import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.env.Environment
import org.http4k.serverless.ApiGatewayV2LambdaFunction
import org.http4k.serverless.AppLoader
import java.time.Clock.systemDefaultZone

@Suppress("unused")
class HitchhikersGuideLambda : ApiGatewayV2LambdaFunction(AppLoader {
    HitchhikersGuideApp(Environment.from(it), JavaHttpClient(), systemDefaultZone())
})
