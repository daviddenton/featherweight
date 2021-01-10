package hg2g

import com.squareup.moshi.Moshi.Builder
import hg2g.api.KotshiArticleJsonAdapter
import hg2g.external.KotshiTranslationTextJsonAdapter
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.SimpleMoshiAdapterFactory
import org.http4k.format.adapter
import org.http4k.format.asConfigurable

object H2G2Moshi : ConfigurableMoshi(
    Builder()
        .add(
            SimpleMoshiAdapterFactory(
                adapter(::KotshiArticleJsonAdapter),
                adapter { KotshiTranslationTextJsonAdapter() }
            )
        )
        .asConfigurable()
        .done()
)