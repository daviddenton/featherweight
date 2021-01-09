package hg2g

import com.squareup.moshi.Moshi
import org.http4k.connect.adapter
import org.http4k.format.AwsJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.asConfigurable

object H2G2Moshi : ConfigurableMoshi(Moshi.Builder()
    // todo replace with http4k adapter factory
    .add(object : AwsJsonAdapterFactory(adapter(::KotshiArticleJsonAdapter)) {})
    .asConfigurable()
    .done()
)