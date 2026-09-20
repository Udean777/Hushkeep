package com.ssajudn.hushkeep.core.common

/**
 * Messages safe to show in the UI. Backend exception text must never be used
 * as user-facing copy because it can contain URLs, headers, policy names, or
 * implementation details.
 */
object UserFacingMessages {
    const val SESSION_RESTORE_FAILED = "Sesi belum dapat dipulihkan. Coba lagi."
    const val SIGN_IN_FAILED = "Email atau password salah, atau layanan sedang tidak tersedia. Coba lagi."
    const val SIGN_UP_FAILED = "Akun belum dapat dibuat. Periksa data dan coba lagi."
    const val REMOTE_REQUEST_FAILED = "Permintaan belum dapat diselesaikan. Coba lagi."
    const val STORAGE_REQUEST_FAILED = "Data belum dapat diproses. Coba lagi."
    const val UPLOAD_FAILED = "Foto belum berhasil dicadangkan. Periksa koneksi dan coba lagi."
    const val LOCAL_MEDIA_UNAVAILABLE = "Foto tidak dapat dibaca. Pilih ulang foto tersebut."
    const val OTP_VERIFY_FAILED = "Kode verifikasi belum dapat diproses. Coba lagi."
    const val OTP_RESEND_FAILED = "Kode baru belum dapat dikirim. Tunggu sebentar lalu coba lagi."

    fun forError(error: AppError): String = when (error) {
        AppError.NotFound -> "Data tidak ditemukan."
        AppError.NetworkUnavailable -> "Koneksi belum tersedia. Periksa koneksi dan coba lagi."
        is AppError.Validation -> error.message
        is AppError.Remote -> REMOTE_REQUEST_FAILED
        is AppError.Storage -> STORAGE_REQUEST_FAILED
        AppError.AuthenticationRequired -> "Sesi diperlukan. Masuk kembali untuk melanjutkan."
        AppError.Unknown -> "Terjadi kesalahan. Coba lagi."
    }
}
