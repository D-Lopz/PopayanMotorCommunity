package com.david.popayanmotor.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // POST /api/auth/login → token JWT + datos del miembro
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    // POST /api/auth/register → crea la cuenta (rol MEMBER); no devuelve token
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Member

    // GET /api/events?category=Meet&search=moto (los parámetros null no se envían)
    @GET("events")
    suspend fun getEvents(
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): List<Event>

    @GET("events/{id}")
    suspend fun getEvent(@Path("id") id: Int): Event

    // Crear, editar y eliminar: el backend responde 403 si quien llama no es ADMINISTRATOR.
    @POST("events")
    suspend fun createEvent(@Body body: EventRequest): Event

    @PUT("events/{id}")
    suspend fun updateEvent(@Path("id") id: Int, @Body body: EventRequest): Event

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: Int): Response<Unit>

    // ── Perfil: el backend solo deja ver/editar el propio (o el admin) ──
    @GET("members/{id}")
    suspend fun getMember(@Path("id") id: Int): Member

    @PUT("members/{id}")
    suspend fun updateMember(@Path("id") id: Int, @Body body: MemberUpdateRequest): Member

    // Con member_id = el propio: un miembro solo puede pedir las suyas, y el admin
    // (que sin filtro recibe las de todos) recibe también solo las suyas.
    @GET("registrations")
    suspend fun getRegistrations(@Query("member_id") memberId: Int): List<Registration>

    // ── Garaje: el backend solo devuelve y deja tocar los vehículos del dueño del token ──
    // member_id por el admin: sin filtro el backend le devuelve los vehículos de TODOS.
    @GET("vehicles")
    suspend fun getVehicles(@Query("member_id") memberId: Int): List<Vehicle>

    @GET("vehicles/{id}")
    suspend fun getVehicle(@Path("id") id: Int): Vehicle

    @POST("vehicles")
    suspend fun createVehicle(@Body body: VehicleRequest): Vehicle

    @PUT("vehicles/{id}")
    suspend fun updateVehicle(@Path("id") id: Int, @Body body: VehicleRequest): Vehicle

    // 204 sin cuerpo → Response<Unit> para no intentar leer un JSON vacío
    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Int): Response<Unit>
}
