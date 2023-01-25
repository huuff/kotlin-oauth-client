package xyz.haff.oauth

import com.auth0.jwt.JWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

data class CachedToken(
    val token: String,
    val expiresAt: LocalDateTime,
)

@Serializable
data class TokenResponse(
    val access_token: String,
)

class OauthClient(
    private val tokenEndpoint: String,
    private val clientId: String,
    private val clientSecret: String,
    private val scopes: List<String>,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    private val mutex = Mutex()

    private var cachedToken: CachedToken? = null

    suspend fun getToken(): String {
        mutex.withLock {
            if (cachedToken?.expiresAt?.isBefore(LocalDateTime.now(clock)) == true) {
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
                val json: TokenResponse = response.body()
                this.cachedToken = CachedToken(
                    token = json.access_token,
                    expiresAt = LocalDateTime.ofInstant(JWT.decode(json.access_token).expiresAtAsInstant, ZoneOffset.UTC),
                )
                return json.access_token
            }
        }
    }

}