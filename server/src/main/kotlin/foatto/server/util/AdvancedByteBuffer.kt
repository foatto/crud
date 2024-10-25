package foatto.server.util

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream
import kotlin.math.max

class AdvancedByteBuffer {

    var buffer: ByteBuffer
        private set

//--- конструкторы --------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- заменяет ByteBuffer.allocate
    constructor(capacity: Int, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN) {
        buffer = ByteBuffer.allocate(max(capacity, 1))
        buffer.order(byteOrder)
    }

    //--- заменяет ByteBuffer.wrap
    constructor(src: ByteArray, byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN) {
        buffer = ByteBuffer.wrap(src)
        buffer.order(byteOrder)
    }

    //--- частный случай создания из hex-строки
    constructor(hex: CharSequence) : this(capacity = hex.length / 2) {
        putHex(hex)
        buffer.flip()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun order(): ByteOrder = buffer.order()

    fun array(): ByteArray = buffer.array()

    fun arrayOffset(): Int = buffer.arrayOffset()

    fun position(): Int = buffer.position()

    fun capacity(): Int = buffer.capacity()

    fun hasRemaining(): Boolean = buffer.hasRemaining()

    fun remaining(): Int = buffer.remaining()

    fun clear(): AdvancedByteBuffer {
        buffer.clear()
        return this
    }

    fun compact(): AdvancedByteBuffer {
        buffer.compact()
        return this
    }

    fun flip(): AdvancedByteBuffer {
        buffer.flip()
        return this
    }

    fun rewind(): AdvancedByteBuffer {
        buffer.rewind()
        return this
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun skip(count: Int): AdvancedByteBuffer {
        for (i in 0 until count) {
            buffer.get()
        }
        return this
    }

    fun get(size: Int): ByteArray {
        val array = ByteArray(size)
        buffer.get(array)
        return array
    }

    fun get(dst: ByteArray): AdvancedByteBuffer {
        buffer.get(dst)
        return this
    }

    fun get(dst: ByteArray, offset: Int, len: Int): AdvancedByteBuffer {
        buffer.get(dst, offset, len)
        return this
    }

    fun put(src: ByteArray, offset: Int = 0, len: Int = src.size): AdvancedByteBuffer {
        checkSize(len)
        buffer.put(src, offset, len)
        return this
    }

    fun put(bb: ByteBuffer): AdvancedByteBuffer {
        checkSize(bb.remaining())
        buffer.put(bb)
        return this
    }

    fun getBoolean(): Boolean = buffer.get().toInt() != 0

    fun putBoolean(value: Boolean): AdvancedByteBuffer {
        checkSize(1)
        buffer.put((if (value) 1 else 0).toByte())
        return this
    }

    fun getByte(): Byte = buffer.get()

    fun putByte(b: Long): AdvancedByteBuffer = putByte(b.toByte())
    fun putByte(b: Int): AdvancedByteBuffer = putByte(b.toByte())
    fun putByte(b: Short): AdvancedByteBuffer = putByte(b.toByte())
    fun putByte(b: Byte): AdvancedByteBuffer {
        checkSize(1)
        buffer.put(b)
        return this
    }

    fun getShort(): Short = buffer.short

    fun putShort(value: Long): AdvancedByteBuffer = putShort(value.toShort())
    fun putShort(value: Int): AdvancedByteBuffer = putShort(value.toShort())
    fun putShort(value: Short): AdvancedByteBuffer {
        checkSize(2)
        buffer.putShort(value)
        return this
    }

    fun getInt(): Int = buffer.int

    //--- трёхбайтовый int
    fun getInt3(): Int {
        val b0 = buffer.get().toInt()
        val b1 = buffer.get().toInt()
        val b2 = buffer.get().toInt()

        return if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            (b0 and 0xFF shl 16) or (b1 and 0xFF shl 8) or (b2 and 0xFF)
        }
        else {
            (b2 and 0xFF shl 16) or (b1 and 0xFF shl 8) or (b0 and 0xFF)
        }
    }

    //--- трёхбайтовый int
    fun putInt3(value: Int): AdvancedByteBuffer {
        checkSize(3)
        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            buffer.put((value shr 16).toByte())
            buffer.put((value shr 8).toByte())
            buffer.put(value.toByte())
        } else {
            buffer.put(value.toByte())
            buffer.put((value shr 8).toByte())
            buffer.put((value shr 16).toByte())
        }
        return this
    }

    fun putInt(value: Long): AdvancedByteBuffer = putInt(value.toInt())
    fun putInt(value: Int): AdvancedByteBuffer {
        checkSize(4)
        buffer.putInt(value)
        return this
    }

    fun getLong(): Long = buffer.long

    fun putLong(value: Long): AdvancedByteBuffer {
        checkSize(8)
        buffer.putLong(value)
        return this
    }

    fun getFloat(): Float = buffer.float

    fun putFloat(value: Double): AdvancedByteBuffer = putFloat(value.toFloat())
    fun putFloat(value: Float): AdvancedByteBuffer {
        checkSize(4)
        buffer.putFloat(value)
        return this
    }

    fun getDouble(): Double = buffer.double
    fun putDouble(value: Double): AdvancedByteBuffer {
        checkSize(8)
        buffer.putDouble(value)
        return this
    }

    fun getShortString(): String {
        val len = buffer.short.toInt()
        val sb = StringBuilder(len)
        for (i in 0 until len) {
            sb.append(buffer.char)
        }
        return sb.toString()
    }

    fun putShortString(s: CharSequence): AdvancedByteBuffer {
        val len = s.length
        checkSize(2 + len * 2)
        buffer.putShort(len.toShort())
//!!! сделать красиво итератором по строке
        for (i in 0 until len) {
            buffer.putChar(s[i])
        }
        return this
    }

    fun getLongString(): String {
        val len = buffer.int
        val sb = StringBuilder(len)
        for (i in 0 until len) {
            sb.append(buffer.char)
        }
        return sb.toString()
    }

    fun putLongString(s: CharSequence): AdvancedByteBuffer {
        val len = s.length
        checkSize(4 + len * 2)
        buffer.putInt(len)
//!!! сделать красиво итератором по строке
        for (i in 0 until len) {
            buffer.putChar(s[i])
        }
        return this
    }

    fun getHex(aSb: StringBuilder?, withSpace: Boolean): StringBuilder {
        val sb = aSb ?: StringBuilder(buffer.remaining() * 2)
        while (buffer.hasRemaining()) {
            byteToHex(buffer.get(), sb, withSpace)
        }
        return sb
    }

    fun putHex(hex: CharSequence?): AdvancedByteBuffer {
        checkSize(if (hex == null) 0 else hex.length / 2)
        if (hex != null) {
            var i = 0
            while (i < hex.length) {
                buffer.put(hexToByte(hex[i++], hex[i++]))
            }
        }
        return this
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun compress(compressionLevel: Int): AdvancedByteBuffer {
        val baos = ByteArrayOutputStream(remaining())
        val dos = DeflaterOutputStream(baos, Deflater(compressionLevel, true))
        writeToOutputStreamAndClose(dos)
        return AdvancedByteBuffer(baos.toByteArray(), buffer.order())
    }

    fun decompress(): AdvancedByteBuffer {
        val baos = ByteArrayOutputStream(remaining())
        val ios = InflaterOutputStream(baos, Inflater(true))
        writeToOutputStreamAndClose(ios)
        return AdvancedByteBuffer(baos.toByteArray(), buffer.order())
    }

//--- служебные методы ----------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- автоматический расширитель буфера, иногда нужен снаружи
    fun checkSize(needSize: Int) {
        //--- может потребоваться несколько расширений подряд
        while (buffer.remaining() < needSize) {
            //--- ищем или создаем буфер увеличенного размера
            val newBB = ByteBuffer.allocate(buffer.capacity() * 2)
            newBB.order(buffer.order())
            //--- копируем данные из текущего буфера в новый
            buffer.flip()
            newBB.put(buffer)
            //--- текущим становится новый буфер
            buffer = newBB
        }
    }

    //--- запись байтов в поток (пусть будет public - может ещё где пригодится, помимо сжатия/расжатия)
    fun writeToOutputStreamAndClose(os: OutputStream) {
        val input = buffer.array()
        val offset = buffer.arrayOffset() + buffer.position()
        val length = buffer.remaining()
        os.write(input, offset, length)
        os.close()
    }

}
