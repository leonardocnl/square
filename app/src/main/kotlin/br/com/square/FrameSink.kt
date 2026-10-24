package br.com.square

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Recebe os frames do jME (thread do jME) e os entrega ao Compose (thread da interface) sem alocar
 * por frame: três blocos de memória nativa do Skia são reaproveitados em rodízio, e a `Image` do
 * frame mais antigo é fechada antes do bloco ser reescrito. A memória nativa do Skia não respeita
 * `-Xmx`; por isso nada aqui fica para o coletor de lixo.
 */
class FrameSink : AutoCloseable {
    /** Quem desenha [current] deve segurar este lock: ele impede que a imagem seja fechada durante o desenho. */
    val lock = Any()

    /** Último frame (RGBA, de baixo para cima). Estado do Compose: ler no desenho redesenha ao mudar. */
    var current by mutableStateOf<ImageBitmap?>(null)
        private set

    private class Slot(val width: Int, val height: Int) {
        val data: Data = Data.makeUninitialized(width * height * 4)
        var image: Image? = null
        fun close() {
            image?.close()
            data.close()
        }
    }

    private val slots = arrayOfNulls<Slot>(SLOTS)
    private var next = 0

    fun publish(buffer: ByteBuffer, width: Int, height: Int) = synchronized(lock) {
        var slot = slots[next]
        if (slot == null || slot.width != width || slot.height != height) {
            slot?.close()
            slot = Slot(width, height).also { slots[next] = it }
        } else {
            slot.image?.close()
        }
        MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), slot.data.writableData(), width * height * 4L)
        val image = Image.makeRaster(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE), slot.data, width * 4)
        slot.image = image
        current = image.toComposeImageBitmap()
        next = (next + 1) % SLOTS
    }

    override fun close() = synchronized(lock) {
        current = null
        slots.forEachIndexed { i, slot -> slot?.close(); slots[i] = null }
    }

    private companion object {
        const val SLOTS = 3
    }
}
