package xyz.haff.oauth

interface OauthClient {

    suspend fun getToken(): String
}