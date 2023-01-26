package xyz.haff.oauth

import com.auth0.jwt.JWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.Clock
import java.time.Instant

data class CachedToken(
    val token: String,
    val expiresAt: Instant,
) {
    fun isNotExpired(clock: Clock) = this.expiresAt.isAfter(Instant.now(clock))
}

class OauthClient(
    private val tokenEndpoint: String,
    private val clientId: String,
    private val clientSecret: String,
    private val scopes: List<String>,
    private val httpClient: HttpClient,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val mutex = Mutex()

    private var cachedToken: CachedToken? = null

    // TODO: Appropriately handle errors
    suspend fun getToken(): String {
        mutex.withLock {
            if (cachedToken?.isNotExpired(clock) == true) {
                return cachedToken!!.token
            } else {
                val response = httpClient.submitForm(
                    url = tokenEndpoint,
                    formParameters = Parameters.build {
                        append("client_id", clientId)
                        append("client_secret", clientSecret)
                        append("grant_type", "client_credentials")
                        append("scopes", scopes.joinToString(separator = " "))
                    }
                )
                val json = JSONObject(response.body<String>())
                val accessToken = json["access_token"] as String
                this.cachedToken = CachedToken(
                    token = accessToken,
                    expiresAt = JWT.decode(accessToken).expiresAtAsInstant,
                )
                return accessToken
            }
        }
    }

}