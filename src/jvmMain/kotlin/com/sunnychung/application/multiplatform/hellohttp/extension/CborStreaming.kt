@file:OptIn(ExperimentalSerializationApi::class)

package com.sunnychung.application.multiplatform.hellohttp.extension

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.cbor.ByteString
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.OutputStream
import kotlin.experimental.or

fun <T> Cbor.encodeToStream(serializer: SerializationStrategy<T>, value: T, out: OutputStream) {
    val output = ByteArrayOutputS(out)
    val dumper = CborWriter(this, CborEncoder(output))
    dumper.encodeSerializableValue(serializer, value)
}

private class ByteArrayOutputS(private val out: OutputStream) : ByteArrayOutput() {
    private var position: Int = 0

    override fun ensureCapacity(elementsToAppend: Int) {
        throw UnsupportedOperationException()
    }

    override fun toByteArray(): ByteArray {
        throw UnsupportedOperationException()
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        // avoid int overflow
        if (offset < 0 || offset > buffer.size || count < 0
            || count > buffer.size - offset
        ) {
            throw IndexOutOfBoundsException()
        }
        if (count == 0) {
            return
        }

        out.write(buffer, offset, count)
        this.position += count
    }

    override fun write(byteValue: Int) {
        out.write(byteValue)
        position++
    }
}

/*****************************************************************/
/** Below is an exact copy from kotlinx.serialization.cbor.Cbor **/
/*****************************************************************/

/**
 * Implements [encoding][encodeToByteArray] and [decoding][decodeFromByteArray] classes to/from bytes
 * using [CBOR](https://tools.ietf.org/html/rfc7049) specification.
 * It is typically used by constructing an application-specific instance, with configured behaviour, and,
 * if necessary, registered custom serializers (in [SerializersModule] provided by [serializersModule] constructor parameter).
 *
 * ### Known caveats and limitations:
 * Supports reading collections of both definite and indefinite lengths; however,
 * serialization always writes maps and lists as [indefinite-length](https://tools.ietf.org/html/rfc7049#section-2.2.1) ones.
 * Does not support [optional tags](https://tools.ietf.org/html/rfc7049#section-2.4) representing datetime, bignums, etc.
 * Fully support CBOR maps, which, unlike JSON ones, may contain keys of non-primitive types, and may produce such maps
 * from corresponding Kotlin objects. However, other 3rd-party parsers (e.g. `jackson-dataformat-cbor`) may not accept such maps.
 *
 * @param encodeDefaults specifies whether default values of Kotlin properties are encoded.
 *                       False by default; meaning that properties with values equal to defaults will be elided.
 * @param ignoreUnknownKeys specifies if unknown CBOR elements should be ignored (skipped) when decoding.
 */
@ExperimentalSerializationApi
public sealed class Cbor(
    internal val encodeDefaults: Boolean,
    internal val ignoreUnknownKeys: Boolean,
    override val serializersModule: SerializersModule
) : BinaryFormat {

    /**
     * The default instance of [Cbor]
     */
    public companion object Default : Cbor(false, false, EmptySerializersModule())

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val output = ByteArrayOutput()
        val dumper = CborWriter(
            this,
            CborEncoder(output)
        )
        dumper.encodeSerializableValue(serializer, value)
        return output.toByteArray()
    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        throw UnsupportedOperationException()

//        val stream = ByteArrayInput(bytes)
//        val reader = CborReader(this, CborDecoder(stream))
//        return reader.decodeSerializableValue(deserializer)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class CborImpl(encodeDefaults: Boolean, ignoreUnknownKeys: Boolean, serializersModule: SerializersModule) :
    Cbor(encodeDefaults, ignoreUnknownKeys, serializersModule)

/**
 * Creates an instance of [Cbor] configured from the optionally given [Cbor instance][from]
 * and adjusted with [builderAction].
 */
@ExperimentalSerializationApi
public fun CborStream(from: Cbor = Cbor, builderAction: CborBuilder.() -> Unit): Cbor {
    val builder = CborBuilder(from)
    builder.builderAction()
    return CborImpl(builder.encodeDefaults, builder.ignoreUnknownKeys, builder.serializersModule)
}

/**
 * Builder of the [Cbor] instance provided by `Cbor` factory function.
 */
@ExperimentalSerializationApi
public class CborBuilder internal constructor(cbor: Cbor) {

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    public var encodeDefaults: Boolean = cbor.encodeDefaults

    /**
     * Specifies whether encounters of unknown properties in the input CBOR
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    public var ignoreUnknownKeys: Boolean = cbor.ignoreUnknownKeys

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Cbor] instance.
     */
    public var serializersModule: SerializersModule = cbor.serializersModule
}


private const val FALSE = 0xf4
private const val TRUE = 0xf5
private const val NULL = 0xf6

private const val NEXT_HALF = 0xf9
private const val NEXT_FLOAT = 0xfa
private const val NEXT_DOUBLE = 0xfb

private const val BEGIN_ARRAY = 0x9f
private const val BEGIN_MAP = 0xbf
private const val BREAK = 0xff

private const val ADDITIONAL_INFORMATION_INDEFINITE_LENGTH = 0x1f

private const val HEADER_BYTE_STRING: Byte = 0b010_00000
private const val HEADER_STRING: Byte = 0b011_00000
private const val HEADER_NEGATIVE: Byte = 0b001_00000
private const val HEADER_ARRAY: Int = 0b100_00000
private const val HEADER_MAP: Int = 0b101_00000
private const val HEADER_TAG: Int = 0b110_00000

/** Value to represent an indefinite length CBOR item within a "length stack". */
private const val LENGTH_STACK_INDEFINITE = -1

private const val HALF_PRECISION_EXPONENT_BIAS = 15
private const val HALF_PRECISION_MAX_EXPONENT = 0x1f
private const val HALF_PRECISION_MAX_MANTISSA = 0x3ff

private const val SINGLE_PRECISION_EXPONENT_BIAS = 127
private const val SINGLE_PRECISION_MAX_EXPONENT = 0xFF

private const val SINGLE_PRECISION_NORMALIZE_BASE = 0.5f

// Differs from List only in start byte
private class CborMapWriter(cbor: Cbor, encoder: CborEncoder) : CborListWriter(cbor, encoder) {
    override fun writeBeginToken() = encoder.startMap()
}

// Writes all elements consequently, except size - CBOR supports maps and arrays of indefinite length
private open class CborListWriter(cbor: Cbor, encoder: CborEncoder) : CborWriter(cbor, encoder) {
    override fun writeBeginToken() = encoder.startArray()

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = true
}

// Writes class as map [fieldName, fieldValue]
private open class CborWriter(private val cbor: Cbor, protected val encoder: CborEncoder) : AbstractEncoder() {
    override val serializersModule: SerializersModule
        get() = cbor.serializersModule

    private var encodeByteArrayAsByteString = false

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (encodeByteArrayAsByteString && serializer.descriptor == ByteArraySerializer().descriptor) {
            encoder.encodeByteString(value as ByteArray)
        } else {
            super.encodeSerializableValue(serializer, value)
        }
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = cbor.encodeDefaults

    protected open fun writeBeginToken() = encoder.startMap()

    //todo: Write size of map or array if known
    @OptIn(ExperimentalSerializationApi::class)
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val writer = when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> CborListWriter(cbor, encoder)
            StructureKind.MAP -> CborMapWriter(cbor, encoder)
            else -> CborWriter(cbor, encoder)
        }
        writer.writeBeginToken()
        return writer
    }

    override fun endStructure(descriptor: SerialDescriptor) = encoder.end()

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        encodeByteArrayAsByteString = descriptor.isByteString(index)
        val name = descriptor.getElementName(index)
        encoder.encodeString(name)
        return true
    }

    override fun encodeString(value: String) = encoder.encodeString(value)

    override fun encodeFloat(value: Float) = encoder.encodeFloat(value)
    override fun encodeDouble(value: Double) = encoder.encodeDouble(value)

    override fun encodeChar(value: Char) = encoder.encodeNumber(value.code.toLong())
    override fun encodeByte(value: Byte) = encoder.encodeNumber(value.toLong())
    override fun encodeShort(value: Short) = encoder.encodeNumber(value.toLong())
    override fun encodeInt(value: Int) = encoder.encodeNumber(value.toLong())
    override fun encodeLong(value: Long) = encoder.encodeNumber(value)

    override fun encodeBoolean(value: Boolean) = encoder.encodeBoolean(value)

    override fun encodeNull() = encoder.encodeNull()

    @OptIn(ExperimentalSerializationApi::class) // KT-46731
    override fun encodeEnum(
        enumDescriptor: SerialDescriptor,
        index: Int
    ) =
        encoder.encodeString(enumDescriptor.getElementName(index))
}

// For details of representation, see https://tools.ietf.org/html/rfc7049#section-2.1
private class CborEncoder(private val output: ByteArrayOutput) {

    fun startArray() = output.write(BEGIN_ARRAY)
    fun startMap() = output.write(BEGIN_MAP)
    fun end() = output.write(BREAK)

    fun encodeNull() = output.write(NULL)

    fun encodeBoolean(value: Boolean) = output.write(if (value) TRUE else FALSE)

    fun encodeNumber(value: Long) = output.write(composeNumber(value))

    fun encodeByteString(data: ByteArray) {
        encodeByteArray(data, HEADER_BYTE_STRING)
    }

    fun encodeString(value: String) {
        encodeByteArray(value.encodeToByteArray(), HEADER_STRING)
    }

    private fun encodeByteArray(data: ByteArray, type: Byte) {
        val header = composeNumber(data.size.toLong())
        header[0] = header[0] or type
        output.write(header)
        output.write(data)
    }

    fun encodeFloat(value: Float) {
        output.write(NEXT_FLOAT)
        val bits = value.toRawBits()
        for (i in 0..3) {
            output.write((bits shr (24 - 8 * i)) and 0xFF)
        }
    }

    fun encodeDouble(value: Double) {
        output.write(NEXT_DOUBLE)
        val bits = value.toRawBits()
        for (i in 0..7) {
            output.write(((bits shr (56 - 8 * i)) and 0xFF).toInt())
        }
    }

    private fun composeNumber(value: Long): ByteArray =
        if (value >= 0) composePositive(value.toULong()) else composeNegative(value)

    private fun composePositive(value: ULong): ByteArray = when (value) {
        in 0u..23u -> byteArrayOf(value.toByte())
        in 24u..UByte.MAX_VALUE.toUInt() -> byteArrayOf(24, value.toByte())
        in (UByte.MAX_VALUE.toUInt() + 1u)..UShort.MAX_VALUE.toUInt() -> encodeToByteArray(value, 2, 25)
        in (UShort.MAX_VALUE.toUInt() + 1u)..UInt.MAX_VALUE -> encodeToByteArray(value, 4, 26)
        else -> encodeToByteArray(value, 8, 27)
    }

    private fun encodeToByteArray(value: ULong, bytes: Int, tag: Byte): ByteArray {
        val result = ByteArray(bytes + 1)
        val limit = bytes * 8 - 8
        result[0] = tag
        for (i in 0 until bytes) {
            result[i + 1] = ((value shr (limit - 8 * i)) and 0xFFu).toByte()
        }
        return result
    }

    private fun composeNegative(value: Long): ByteArray {
        val aVal = if (value == Long.MIN_VALUE) Long.MAX_VALUE else -1 - value
        val data = composePositive(aVal.toULong())
        data[0] = data[0] or HEADER_NEGATIVE
        return data
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun SerialDescriptor.isByteString(index: Int): Boolean {
    return getElementAnnotations(index).find { it is ByteString } != null
}

private open class ByteArrayOutput {
    private var array: ByteArray = ByteArray(32)
    private var position: Int = 0

    open fun ensureCapacity(elementsToAppend: Int) {
        if (position + elementsToAppend <= array.size) {
            return
        }
        val newArray = ByteArray((position + elementsToAppend).takeHighestOneBit() shl 1)
        array.copyInto(newArray)
        array = newArray
    }

    public open fun toByteArray(): ByteArray {
        val newArray = ByteArray(position)
        array.copyInto(newArray, startIndex = 0, endIndex = this.position)
        return newArray
    }

    open fun write(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size) {
        // avoid int overflow
        if (offset < 0 || offset > buffer.size || count < 0
            || count > buffer.size - offset
        ) {
            throw IndexOutOfBoundsException()
        }
        if (count == 0) {
            return
        }

        ensureCapacity(count)
        buffer.copyInto(
            destination = array,
            destinationOffset = this.position,
            startIndex = offset,
            endIndex = offset + count
        )
        this.position += count
    }

    open fun write(byteValue: Int) {
        ensureCapacity(1)
        array[position++] = byteValue.toByte()
    }
}
