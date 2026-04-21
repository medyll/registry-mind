package com.registry.mind.network

import com.registry.mind.data.RegistryPacket
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ClawApiService {
    
    @POST("/api/v1/capture")
    suspend fun sendPacket(
        @Header("X-Registry-Auth") authToken: String,
        @Body packet: RegistryPacket
    ): Response<Unit>
}
