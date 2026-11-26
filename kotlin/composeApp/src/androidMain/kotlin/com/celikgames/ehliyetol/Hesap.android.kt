package com.celikgames.ehliyetol

import android.os.CancellationSignal
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.Executors

/**
 * Hesap (Firebase Authentication): misafir (anonim), e-posta/şifre, Google (Credential Manager).
 * Misafir hesabı sonradan e-posta ya da Google'a bağlanırsa aynı kullanıcı kimliği korunur.
 * Bulut yedeği: Firestore yedek/{uid} = {veri: JSON metni, zaman, surum} — eski sürümle aynı belge, kurallar: yalnız sahibi.
 */
actual object Hesap {
    actual val destekli: Boolean = true
    private val ARKA = Executors.newSingleThreadExecutor()
    private fun fa() = FirebaseAuth.getInstance()

    actual fun durum(): HesapBilgi = runCatching {
        val u = fa().currentUser ?: return@runCatching HesapBilgi()
        var yontem = "misafir"
        for (i in u.providerData) {
            if (i.providerId == "google.com") yontem = "google"
            else if (i.providerId == "password" && yontem != "google") yontem = "eposta"
        }
        HesapBilgi(true, u.isAnonymous, u.email.orEmpty(), u.displayName.orEmpty(), yontem)
    }.getOrDefault(HesapBilgi())

    private fun hataMetni(e: Exception?): String = when (e) {
        null -> "İşlem yapılamadı. Biraz sonra tekrar dene."
        is GetCredentialCancellationException -> ""   // kullanıcı vazgeçti
        is NoCredentialException -> "Telefonda kullanılabilir bir Google hesabı bulunamadı."
        is FirebaseAuthUserCollisionException -> "Bu hesap zaten kayıtlı. Giriş yapmayı dene."
        is FirebaseAuthWeakPasswordException -> "Şifre en az 6 karakter olmalı."
        is FirebaseAuthInvalidUserException -> "Bu e-postayla kayıtlı hesap bulunamadı."
        is FirebaseAuthInvalidCredentialsException -> "E-posta ya da şifre hatalı."
        is FirebaseAuthRecentLoginRequiredException -> "Güvenlik için önce çıkış yapıp yeniden giriş yapman gerekiyor."
        is FirebaseNetworkException -> "İnternet bağlantısı yok."
        else -> "İşlem yapılamadı. Biraz sonra tekrar dene."
    }

    private fun <T> Task<T>.sonuc(islem: String, s: (HesapSonuc) -> Unit) =
        addOnCompleteListener { t -> s(HesapSonuc(islem, t.isSuccessful, if (t.isSuccessful) "" else hataMetni(t.exception))) }

    actual fun islem(islem: String, eposta: String, sifre: String, sonuc: (HesapSonuc) -> Unit) {
        try {
            when (islem) {
                "misafir" -> if (fa().currentUser != null) sonuc(HesapSonuc("misafir", true)) else fa().signInAnonymously().sonuc("misafir", sonuc)
                "giris" -> fa().signInWithEmailAndPassword(eposta, sifre).sonuc("eposta", sonuc)
                "kayit" -> {
                    val u = fa().currentUser
                    if (u != null && u.isAnonymous) u.linkWithCredential(EmailAuthProvider.getCredential(eposta, sifre)).sonuc("eposta", sonuc)
                    else fa().createUserWithEmailAndPassword(eposta, sifre).sonuc("eposta", sonuc)
                }
                "sifirla" -> { fa().setLanguageCode("tr"); fa().sendPasswordResetEmail(eposta).sonuc("sifirla", sonuc) }
                "google" -> google(sonuc)
                "cikis" -> cikis(sonuc)
                "sil" -> sil(sonuc)
                "yedekOku" -> yedekOku(sonuc)
                else -> sonuc(HesapSonuc(islem, false))
            }
        } catch (e: Exception) { sonuc(HesapSonuc(islem, false, hataMetni(e))) }
    }

    private fun google(s: (HesapSonuc) -> Unit) {
        val a = AndroidBaglam.etkinlik.get() ?: return s(HesapSonuc("google", false, hataMetni(null)))
        val kimlik = a.resources.getIdentifier("default_web_client_id", "string", a.packageName)
        if (kimlik == 0) return s(HesapSonuc("google", false, "Google ile giriş bu sürümde yok. E-posta ile giriş yapabilirsin."))
        val secenek = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(a.getString(kimlik)).build()
        val istek = GetCredentialRequest.Builder().addCredentialOption(secenek).build()
        CredentialManager.create(a).getCredentialAsync(a, istek, CancellationSignal(), ARKA,
            object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(r: GetCredentialResponse) {
                    try {
                        val g = GoogleIdTokenCredential.createFrom(r.credential.data)
                        val ac: AuthCredential = GoogleAuthProvider.getCredential(g.idToken, null)
                        val u = fa().currentUser
                        if (u != null && u.isAnonymous) {
                            // misafir hesabını Google'a bağla; bu Google hesabı zaten kayıtlıysa doğrudan onunla gir
                            u.linkWithCredential(ac).addOnCompleteListener { t ->
                                if (t.isSuccessful) s(HesapSonuc("google", true)) else fa().signInWithCredential(ac).sonuc("google", s)
                            }
                        } else fa().signInWithCredential(ac).sonuc("google", s)
                    } catch (e: Exception) { s(HesapSonuc("google", false, hataMetni(e))) }
                }
                override fun onError(e: GetCredentialException) { s(HesapSonuc("google", false, hataMetni(e))) }
            })
    }

    private fun cikis(s: (HesapSonuc) -> Unit) {
        fa().signOut()
        AndroidBaglam.etkinlik.get()?.let { a ->
            runCatching {
                CredentialManager.create(a).clearCredentialStateAsync(ClearCredentialStateRequest(), CancellationSignal(), ARKA,
                    object : CredentialManagerCallback<Void?, ClearCredentialException> {
                        override fun onResult(result: Void?) {}
                        override fun onError(e: ClearCredentialException) {}
                    })
            }
        }
        s(HesapSonuc("cikis", true))
    }

    private fun belge(u: FirebaseUser) = FirebaseFirestore.getInstance().collection("yedek").document(u.uid)
    private fun kalici(u: FirebaseUser?) = u != null && !u.isAnonymous

    actual fun yedekYaz(veri: String, sonuc: (HesapSonuc) -> Unit) {
        val u = fa().currentUser
        if (!kalici(u)) return sonuc(HesapSonuc("yedekYaz", false))
        belge(u!!).set(mapOf("veri" to veri, "zaman" to FieldValue.serverTimestamp(), "surum" to 1)).sonuc("yedekYaz", sonuc)
    }

    private fun yedekOku(s: (HesapSonuc) -> Unit) {
        val u = fa().currentUser
        if (!kalici(u)) return s(HesapSonuc("yedekOku", false))
        belge(u!!).get().addOnCompleteListener { t ->
            val veri = if (t.isSuccessful && t.result?.exists() == true) t.result?.getString("veri").orEmpty() else ""
            s(HesapSonuc("yedekOku", t.isSuccessful, if (t.isSuccessful) "" else hataMetni(t.exception), veri))
        }
    }

    /** Hesabı kalıcı olarak siler (Google Play şartı: uygulama içinden hesap silme). Önce buluttaki yedek silinir. */
    private fun sil(s: (HesapSonuc) -> Unit) {
        val u = fa().currentUser ?: return s(HesapSonuc("sil", true))
        val hesabiSil = { u.delete().sonuc("sil", s); Unit }
        if (kalici(u)) belge(u).delete().addOnCompleteListener { hesabiSil() } else hesabiSil()
    }
}
