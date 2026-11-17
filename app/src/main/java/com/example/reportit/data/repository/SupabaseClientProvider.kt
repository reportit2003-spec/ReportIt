package com.example.reportit.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClientProvider {

    private const val SUPABASE_URL = "https://ozxicyntdcbqntqdsqln.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im96eGljeW50ZGNicW50cWRzcWxuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA4OTY3NDMsImV4cCI6MjA5NjQ3Mjc0M30.Rfdke4wO0pkdMUsqEgTfn7XFPiFhtET5riEChJJ-aKM"

    private var client: SupabaseClient? = null

    val supabase: SupabaseClient
        get() {
            if (client == null) {
                client = createSupabaseClient(
                    supabaseUrl = SUPABASE_URL,
                    supabaseKey = SUPABASE_ANON_KEY
                ) {
                    install(Auth)
                    install(Postgrest)
                    install(Storage)
                    install(Realtime)
                    defaultSerializer = KotlinXSerializer(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }
            return client!!
        }

    val auth get() = supabase.auth
    val postgrest get() = supabase.postgrest
    val storage get() = supabase.storage
    val realtime get() = supabase.realtime
}