package me.nanova.summaryexpressive

import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        return try {
            ProtoBuf.decodeFromByteArray(UserPreferences.serializer(), input.readBytes())
        } catch (exception: SerializationException) {
            exception.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        output.write(ProtoBuf.encodeToByteArray(UserPreferences.serializer(), t))
    }
}
