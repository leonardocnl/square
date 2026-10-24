package br.com.square

import com.jme3.util.BufferUtils
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FrameSinkTest {
    private fun frame(w: Int, h: Int, seed: Int) = BufferUtils.createByteBuffer(w * h * 4).also { b ->
        for (i in 0 until w * h) {
            b.put((seed + i).toByte()).put((seed * 2).toByte()).put((i % 251).toByte()).put(0xFF.toByte())
        }
        b.flip()
    }

    @Test
    fun publishesFrameContentAndReusesSlots() {
        val sink = FrameSink()
        val w = 64
        val h = 48
        repeat(50) { n -> // muito mais frames que blocos: o rodízio reaproveita a memória
            sink.publish(frame(w, h, n), w, h)
        }
        val last = 49
        val image = assertNotNull(sink.current)
        // lê de volta pelo Compose: o primeiro pixel tem o padrão de `frame`
        val pixels = image.toPixelMap()
        val first = pixels[0, 0]
        assertEquals(((last and 0xFF) / 255f), first.red, 0.01f)
        assertEquals((((last * 2) and 0xFF) / 255f), first.green, 0.01f)
        sink.close()
        assertEquals(null, sink.current)
    }

    @Test
    fun resizeReplacesSlots() {
        val sink = FrameSink()
        sink.publish(frame(32, 32, 1), 32, 32)
        sink.publish(frame(48, 16, 2), 48, 16)
        assertEquals(48, sink.current!!.width)
        sink.close()
    }
}
