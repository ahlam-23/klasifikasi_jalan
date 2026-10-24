package com.example.klasifikasijalan

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min

class RoadClassifier(context: Context) {
    private var interpreter: Interpreter
    private val IMAGE_SIZE = 224

    init {
        // Menggunakan 4 thread untuk performa real-time
        val options = Interpreter.Options().apply { setNumThreads(4) }

        val fileDescriptor = context.assets.openFd("model_jalan_final.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)

        interpreter = Interpreter(mappedBuffer, options)
    }

    fun classify(bitmap: Bitmap): String {
        // --- 1. PERBAIKAN AKURASI: CENTER CROP ---
        // Mencari sisi terpendek agar gambar menjadi bujur sangkar sempurna tanpa distorsi
        val dimension = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - dimension) / 2
        val y = (bitmap.height - dimension) / 2
        val squaredBitmap = Bitmap.createBitmap(bitmap, x, y, dimension, dimension)

        // Setelah dipotong kotak, baru di-resize ke ukuran input model (224x224)
        val resizedBitmap = Bitmap.createScaledBitmap(squaredBitmap, IMAGE_SIZE, IMAGE_SIZE, true)

        val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        var pixel = 0
        for (i in 0 until IMAGE_SIZE) {
            for (j in 0 until IMAGE_SIZE) {
                val valPixel = intValues[pixel++]

                // --- 2. PERBAIKAN NORMALISASI: MENGGUNAKAN SKALA 0-1 (Opsi B) ---
                // Berdasarkan hasil tes, kemungkinan besar model Anda dilatih dengan rescale=1./255
                // sehingga butuh input skala 0.0 sampai 1.0 agar tidak salah baca.
                byteBuffer.putFloat(((valPixel shr 16 and 0xFF) / 255.0f)) // R
                byteBuffer.putFloat(((valPixel shr 8 and 0xFF) / 255.0f))  // G
                byteBuffer.putFloat(((valPixel and 0xFF) / 255.0f))        // B
            }
        }

        val result = Array(1) { FloatArray(4) }
        interpreter.run(byteBuffer, result)

        // --- CARA PALING AMPUH: DEBUG SELURUH INDEX ---
        // Kita simpan semua probabilitas agar bisa ditampilkan di UI untuk dicek manual
        val prob0 = String.format("%.1f", result[0][0] * 100)
        val prob1 = String.format("%.1f", result[0][1] * 100)
        val prob2 = String.format("%.1f", result[0][2] * 100)
        val prob3 = String.format("%.1f", result[0][3] * 100)

        // --- PEMETAAN FINAL 100% BERDASARKAN HASIL TEST NYATA ANDA ---
        // Foto 1: Aspal Mulus -> Idx 1 (98%) -> Index 1 = Jalan Baik
        // Foto 2: Tanah/Batu  -> Idx 0 (100%) -> Index 0 = Belum Diaspal
        // Foto 3: Lubang Dalam -> Idx 2 (100%) -> Index 2 = Rusak Berat
        // Maka otomatis sisanya: Index 3 = Rusak Ringan
        
        val labels = listOf(
            "Belum Diaspal", // Index 0
            "Jalan Baik",    // Index 1
            "Rusak Berat",   // Index 2
            "Rusak Ringan"   // Index 3
        )

        var maxIdx = 0
        var maxProb = result[0][0]
        for (i in result[0].indices) {
            if (result[0][i] > maxProb) {
                maxProb = result[0][i]
                maxIdx = i
            }
        }

        val debugInfo = "Idx:$maxIdx | 0:$prob0%, 1:$prob1%, 2:$prob2%, 3:$prob3%"
        
        val persentase = maxProb * 100
        
        // --- LOGIKA FILTER OBJEK BUKAN JALAN (THRESHOLD TINGGI) ---
        // Karena model Anda hanya punya 4 kelas jalan, dia akan "memaksa" objek lain
        // (pohon, orang, dll) masuk ke salah satu kelas tersebut.
        // Kita naikkan batas keyakinan menjadi 92% untuk benar-benar memastikan itu jalan.
        if (persentase < 92.0f) {
            return "Bukan Objek Jalan / Tidak Terdeteksi\n$debugInfo"
        }

        return "${labels[maxIdx]} (${String.format("%.1f", persentase)}%)\n$debugInfo"
    }
}