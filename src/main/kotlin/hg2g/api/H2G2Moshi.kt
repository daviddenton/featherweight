package hg2g.api

import com.squareup.moshi.Moshi.Builder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.SimpleMoshiAdapterFactory
import org.http4k.format.adapter
import org.http4k.format.asConfigurable

object H2G2Moshi : ConfigurableMoshi(
    Builder()
        .add(SimpleMoshiAdapterFactory(adapter(::KotshiArticleJsonAdapter)))
        .asConfigurable()
        .done()
)