package hg2g.external

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.kms.KMS
import org.http4k.connect.amazon.kms.sign
import org.http4k.connect.amazon.model.Base64Blob
import org.http4k.connect.amazon.model.KMSKeyId
import org.http4k.connect.amazon.model.SSMParameterName
import org.http4k.connect.amazon.model.SigningAlgorithm.ECDSA_SHA_256
import org.http4k.connect.amazon.systemsmanager.SystemsManager
import org.http4k.connect.amazon.systemsmanager.getParameter

/**
 * For signing content using Universe-grade encryption. :)
 */
fun interface Signer {
    fun sign(value: String): Result<String, RemoteFailure>
}

/**
 * AWS KMS implementation of the Signer.
 */
fun KmsSigner(kms: KMS, systemsManager: SystemsManager, keyParameterName: SSMParameterName) = Signer { message ->
    systemsManager.getParameter(keyParameterName)
        .map { KMSKeyId.of(it.Parameter.Value!!) }
        .flatMap { kms.sign(it, Base64Blob.encoded(message), ECDSA_SHA_256) }
        .map { it.Signature.decoded() }
}
