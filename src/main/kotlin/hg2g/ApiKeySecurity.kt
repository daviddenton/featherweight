package hg2g

import dev.forkhandles.result4k.valueOrNull
import org.http4k.connect.amazon.model.SecretId
import org.http4k.connect.amazon.secretsmanager.SecretsManager
import org.http4k.connect.amazon.secretsmanager.getSecretValue
import org.http4k.core.Filter
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header

/**
 * Get the API Key from the Secretsmanager and use it to vet the incoming requests
 */
fun ApiKeySecurity(secretId: SecretId, secretsManager: SecretsManager): Filter {
    val apiKey = secretsManager.loadApiKey(secretId)
    return ServerFilters.ApiKeyAuth(Header.required("Api-Key")) { it == apiKey }
}

private fun SecretsManager.loadApiKey(secretId: SecretId) =
    getSecretValue(secretId).valueOrNull()!!.SecretString
