package env


import org.http4k.connect.amazon.AwsServiceCompanion
import org.http4k.core.HttpHandler
import org.http4k.routing.bind
import org.http4k.routing.header
import org.http4k.routing.routes

// TODO use connect version
fun AwsReverseProxy(vararg awsServices: Pair<AwsServiceCompanion, HttpHandler>) =
    routes(
        *awsServices.map { it.first.awsService.value to it.second }.toTypedArray()
            .map { service ->
                header("host") { it.contains(service.first) } bind service.second
            }.toTypedArray()
    )
