package com.sunnychung.application.multiplatform.hellohttp.serializer

import com.sunnychung.application.multiplatform.hellohttp.util.log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SynchronizedListSerializer<E>(elementSerializer: KSerializer<E>) : KSerializer<MutableList<E>> {
    private val listSerializer = ListSerializer(elementSerializer)
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: MutableList<E>) {
        log.d { "SynchronizedListSerializer serialize" }
        synchronized(value) {
            listSerializer.serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): MutableList<E> {
        log.d { "SynchronizedListSerializer deserialize" }
        return listSerializer.deserialize(decoder).toMutableList()
    }
}
