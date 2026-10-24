# Klasifikasi Kondisi Jalan (Road Condition Classification)

Aplikasi Android berbasis *machine learning* yang dirancang untuk mendeteksi dan mengklasifikasikan kondisi permukaan jalan secara *real-time*. Proyek ini mengimplementasikan model *Deep Learning* langsung pada perangkat seluler dan dilengkapi dengan sistem pelaporan otomatis.

## 🚀 Fitur Utama

* **Deteksi Real-Time:** Menggunakan **CameraX** untuk menangkap dan menganalisis kondisi jalan secara langsung dari kamera perangkat.
* **Klasifikasi Machine Learning:** Diperkuat oleh arsitektur **CNN MobileNetV2** yang telah dioptimalkan untuk perangkat *mobile* (TensorFlow Lite), memastikan deteksi yang ringan namun akurat.
* **Pelacakan Lokasi (GPS):** Terintegrasi dengan sistem GPS untuk mencatat titik koordinat lokasi kerusakan atau kondisi jalan yang terdeteksi.
* **Sistem Pelaporan Otomatis:** Menggunakan **Telegram Bot API** untuk mengirimkan hasil klasifikasi beserta data lokasi secara otomatis ke grup atau instansi terkait.

## 🛠️ Teknologi yang Digunakan

* **Platform:** Android
* **Kamera:** CameraX API
* **Machine Learning:** TensorFlow Lite (TFLite)
* **Model Arsitektur:** Convolutional Neural Network (CNN) - MobileNetV2
* **Integrasi Pihak Ketiga:** Telegram Bot API, Google Maps/Location Services

## 📁 Struktur Proyek Utama

Karena repositori ini menyimpan file `.git` dan struktur aset (seperti gambar `.webp` untuk *interface* atau *dataset*), pastikan struktur kode utama Anda diatur sebagai berikut:
* `app/src/main/` - Berisi *source code* aplikasi Android (UI, integrasi API, CameraX).
* `app/src/main/assets/` - Lokasi penyimpanan file model `.tflite` dan label data.
* `dataset/` - (Opsional) Kumpulan data citra jalan yang digunakan untuk melatih model MobileNetV2.

## ⚙️ Prasyarat & Instalasi

1. Pastikan Anda telah menginstal **Android Studio**.
2. Clone repositori ini:
   ```bash
   git clone [https://github.com/username-anda/klasifikasijalan.git](https://github.com/username-anda/klasifikasijalan.git)
