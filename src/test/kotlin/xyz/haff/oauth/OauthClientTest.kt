package xyz.haff.oauth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import java.time.Instant

class OauthClientTest : FunSpec({

    test("renews expired token") {
        // ARRANGE
        val expiredToken = JWT.create()
            .withExpiresAt(Instant.ofEpochMilli(0))
            .sign(Algorithm.HMAC256("test"))

        val mockHttpEngine = MockEngine {
            respond(
                content = ByteReadChannel("""
                    {
                        "access_token": "$expiredToken"
                    }
                """.trimIndent()),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )

        }
        val mockHttpClient = HttpClient(mockHttpEngine)
        val oauthClient = OauthClient(
            tokenEndpoint = "test",
            clientId = "test",
            clientSecret = "test",
            scopes = listOf("test"),
            httpClient = mockHttpClient,
        )

        // ACT
        val firstToken = oauthClient.getToken()
        val secondToken = oauthClient.getToken()

        // ASSERT
        mockHttpEngine.requestHistory.size shouldBe 2
        firstToken shouldBe expiredToken
        secondToken shouldBe expiredToken
    }

})
