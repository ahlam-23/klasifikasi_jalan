package com.example.klasifikasijalan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.klasifikasijalan.databinding.FragmentDetectionBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DetectionFragment : Fragment() {
    private var _binding: FragmentDetectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var classifier: RoadClassifier
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastBitmap: Bitmap? = null
    private var currentGps: String = "Belum terdeteksi"
    private var currentAddress: String = "Mencari alamat..."

    // Token & ID Telegram (Update berdasarkan input user)
    private val botToken = "8840890721:AAFe5ZG4nZlibeT2kb__u3DKNDy4rwXfVYY"
    private val chatId = "2030461158"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && 
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // Camera will be started manually via button
        } else {
            Toast.makeText(context, "Izin Kamera & Lokasi diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processGalleryImage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        UiAnimations.staggerFadeUp(binding.contentContainer)

        binding.viewFinder.visibility = View.INVISIBLE

        classifier = RoadClassifier(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        checkPermissions()
        getLocation()

        binding.btnCamera.setOnClickListener {
            if (imageCapture == null) {
                // Kamera belum aktif, aktifkan dulu
                startCamera()
                binding.imgPreview.visibility = View.GONE
                binding.viewFinder.visibility = View.VISIBLE
                binding.btnProcess.visibility = View.GONE
                binding.btnProcess.alpha = 0f
                binding.tvResult.text = "Kamera diaktifkan. Klik lagi untuk ambil foto."
            } else if (binding.imgPreview.visibility == View.VISIBLE) {
                // Sedang menampilkan hasil/preview, kembali ke kamera live
                binding.imgPreview.visibility = View.GONE
                binding.viewFinder.visibility = View.VISIBLE
                binding.btnProcess.visibility = View.GONE
                binding.btnReport.isEnabled = false
                binding.tvResult.text = "Arahkan kamera ke permukaan jalan"
            } else {
                // Kamera sudah live, ambil foto
                takePhoto()
            }
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnProcess.setOnClickListener {
            lastBitmap?.let { processDetection(it) }
        }

        binding.btnReport.setOnClickListener {
            sendReport()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }) {
            // Do not start camera automatically
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                activity?.runOnUiThread {
                    showPreview(bitmap)
                    UiAnimations.reveal(binding.btnProcess)
                    binding.tvResult.text = "Gambar siap dianalisis"
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        })
    }

    private fun processGalleryImage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            showPreview(bitmap)
            UiAnimations.reveal(binding.btnProcess)
            binding.tvResult.text = "Gambar siap dianalisis"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPreview(bitmap: Bitmap) {
        lastBitmap = bitmap
        binding.viewFinder.visibility = View.GONE
        binding.imgPreview.visibility = View.VISIBLE
        binding.imgPreview.setImageBitmap(bitmap)
    }

    private fun processDetection(bitmap: Bitmap) {
        val result = classifier.classify(bitmap)
        
        // Membersihkan teks debug untuk pemrosesan logika
        val cleanResult = result.substringBefore("\n")
        binding.tvResult.text = cleanResult
        
        // Cek apakah hasil deteksi valid
        val isValidRoad = cleanResult.contains("Jalan Baik") || 
                         cleanResult.contains("Rusak Ringan") || 
                         cleanResult.contains("Rusak Berat") || 
                         cleanResult.contains("Belum Diaspal")

        binding.btnReport.isEnabled = isValidRoad
        binding.btnProcess.visibility = View.GONE
        
        // 1. Logika Saran Keselamatan & Pesan untuk Masyarakat
        val (actionPlan, urgency) = when {
            cleanResult.contains("Rusak Berat") -> 
                "⚠️ Mohon hati-hati, jalan rusak parah. Laporan telah tercatat untuk ditindaklanjuti." to "Bahaya (Mohon Hindari)"
            cleanResult.contains("Rusak Ringan") -> 
                "🔍 Tetap waspada terhadap lubang kecil agar tidak terjadi kecelakaan." to "Waspada"
            cleanResult.contains("Belum Diaspal") -> 
                "🏗️ Jalan tanah/kerikil. Berkendaralah dengan kecepatan rendah." to "Informasi"
            cleanResult.contains("Jalan Baik") -> 
                "✅ Jalan dalam kondisi baik. Selamat berkendara dengan aman!" to "Aman"
            else -> "-" to "-"
        }

        // 2. Gabungkan hasil yang lebih informatif ke UI
        if (isValidRoad) {
            val formattedResult = StringBuilder()
            formattedResult.append(cleanResult)
            formattedResult.append("\n\n📍 Lokasi: $currentAddress")
            formattedResult.append("\n\n💡 Tips Keselamatan:\n$actionPlan")
            formattedResult.append("\n\n📌 Status: $urgency")
            formattedResult.append("\n⏰ Dilaporkan: ${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}")
            
            binding.tvResult.text = formattedResult.toString()
        }

        // Suggest comment based on result
        val suggestedComment = when {
            cleanResult.contains("Rusak Berat") -> "Kondisi jalan rusak berat, mohon segera ditindaklanjuti."
            cleanResult.contains("Rusak Ringan") -> "Terdapat kerusakan ringan pada permukaan jalan."
            cleanResult.contains("Belum Diaspal") -> "Jalan ini belum diaspal atau masih berupa tanah/kerikil."
            cleanResult.contains("Jalan Baik") -> "Kondisi jalan terpantau baik."
            else -> ""
        }
        binding.etComment.setText(suggestedComment)
        
        if (!isValidRoad) {
            Toast.makeText(context, "Gambar bukan merupakan objek jalan yang valid", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                currentGps = "${it.latitude}, ${it.longitude}"
                binding.tvGps.text = "GPS: $currentGps"
                
                // Get Address from Coordinates
                try {
                    val geocoder = Geocoder(requireContext(), Locale("id", "ID"))
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val street = address.thoroughfare ?: "Jalan tidak dikenal"
                        val district = address.locality ?: ""
                        val city = address.subAdminArea ?: ""
                        currentAddress = "$street, $district, $city\n($currentGps)"
                        binding.tvGps.text = "Lokasi: $currentAddress"
                    }
                } catch (e: Exception) {
                    currentAddress = "Gagal mengambil nama jalan"
                }
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun sendReport() {
        val bitmap = lastBitmap ?: return
        val result = binding.tvResult.text.toString().substringBefore("\n")
        val comment = binding.etComment.text.toString()
        val gps = currentGps
        
        // Buat link Google Maps agar bisa dibuka di Telegram
        val googleMapsUrl = "https://www.google.com/maps/search/?api=1&query=$gps"

        val message = """
            📢 *LAPORAN WARGA - KONDISI JALAN*
            ━━━━━━━━━━━━━━━━━━━━
            🚦 *KONDISI DI LOKASI:*
            `${result.uppercase()}`
            
            📍 *ALAMAT LENGKAP:*
            `${currentAddress.uppercase()}`
            
            🗺️ *BUKA DI PETA:*
            [Lihat Lokasi di Google Maps]($googleMapsUrl)
            
            📝 *PESAN DARI WARGA:*
            "$comment"
            
            📅 *WAKTU PELAPORAN:*
            ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}
            ━━━━━━━━━━━━━━━━━━━━
            _Sent via RoadSense AI_
        """.trimIndent()

        Toast.makeText(context, "Mengirim laporan...", Toast.LENGTH_SHORT).show()

        val client = OkHttpClient()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", message)
            .addFormDataPart("parse_mode", "Markdown")
            .addFormDataPart("photo", "road_damage.jpg", 
                RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray))
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendPhoto")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Gagal kirim: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity?.runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Laporan berhasil terkirim!", Toast.LENGTH_SHORT).show()
                        
                        // Reset form to prevent spam
                        binding.etComment.text?.clear()
                        binding.btnReport.isEnabled = false
                        binding.tvResult.text = "Laporan telah terkirim.\nSilakan ambil foto baru untuk deteksi lainnya."
                        
                        // Reset preview and back to camera
                        binding.imgPreview.visibility = View.GONE
                        binding.viewFinder.visibility = View.VISIBLE
                        lastBitmap = null
                    } else {
                        Toast.makeText(context, "Error server: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}