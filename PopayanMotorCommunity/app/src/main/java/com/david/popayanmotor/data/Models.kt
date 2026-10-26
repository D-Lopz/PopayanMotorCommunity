package com.david.popayanmotor.data

import com.google.gson.annotations.SerializedName

// Los nombres en @SerializedName son los del JSON del backend (schemas de Pydantic).

// ── Auth ───────────────────────────────────────────────────────
data class LoginRequest(
    val email: String,
    val password: String
)

// Sin campo "role": el backend siempre crea MEMBER, aunque alguien lo envíe.
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String?
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    val member: Member
)

data class Member(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val city: String?,
    @SerializedName("join_date") val joinDate: String,   // "2026-10-26T06:54:05" (hora Colombia)
    val role: String          // "MEMBER" o "ADMINISTRATOR"
)

// Sin email ni role: el correo no se cambia y el rol nunca se toca desde el perfil.
data class MemberUpdateRequest(
    val name: String,
    val phone: String?,
    val city: String?
)

// ── Inscripciones (solo lo que usa el perfil para las estadísticas) ──
data class Registration(
    val id: Int,
    @SerializedName("code_expires_at") val codeExpiresAt: String,  // fin del evento: después de esto ya terminó
    val event: RegistrationEvent
)

data class RegistrationEvent(
    @SerializedName("distance_km") val distanceKm: Double?
)

// ── Eventos ────────────────────────────────────────────────────
data class Event(
    val id: Int,
    val name: String,
    val description: String?,
    val date: String,                         // "2026-11-07"
    @SerializedName("end_date") val endDate: String?,
    val time: String,                         // "10:00:00"
    val location: String,
    @SerializedName("max_capacity") val maxCapacity: Int,
    val category: String,
    val price: Int,                           // COP, solo informativo
    @SerializedName("distance_km") val distanceKm: Double?,
    @SerializedName("registered_count") val registeredCount: Int,
    @SerializedName("attendees_count") val attendeesCount: Int,
    val finished: Boolean
)

// Cuerpo de POST/PUT /api/events (solo admin). Los null no se envían: el backend los toma como vacíos.
data class EventRequest(
    val name: String,
    val description: String?,
    val date: String,                         // "2026-11-07"
    @SerializedName("end_date") val endDate: String?,
    val time: String,                         // "16:00"
    val location: String,
    @SerializedName("max_capacity") val maxCapacity: Int,
    val category: String,                     // uno de R.array.event_categories
    val price: Int,
    @SerializedName("distance_km") val distanceKm: Double?
)

// ── Vehículos ──────────────────────────────────────────────────
data class Vehicle(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val type: String,                         // uno de R.array.vehicle_types
    val plate: String,                        // "ABC123" (el backend la normaliza)
    val description: String?,
    @SerializedName("is_primary") val isPrimary: Boolean
)

// Sin member_id: el dueño lo pone el backend a partir del token.
data class VehicleRequest(
    val brand: String,
    val model: String,
    val year: Int,
    val type: String,
    val plate: String,
    val description: String?,
    @SerializedName("is_primary") val isPrimary: Boolean
)

// ── Error del backend: {"detail": "...", "code": "..."} ────────
data class ApiErrorBody(
    val detail: String?,
    val code: String?
)
