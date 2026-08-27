# kaspintest — Mini POS (Task 4, Kasir Pintar Technical Test)

Mini Android app: **Product → Transaction → Local Database (Room) → Sync Queue (Outbox + WorkManager) → Mock API**. Java, no external backend — the "server" is an in-process mock so the whole offline→sync flow is demonstrable without any real network dependency.

## Arsitektur

```
UI (Activity + Fragment, ViewBinding, ViewModel)
        │
        ▼
TransactionRepository ──▶ Room (products / transactions / outbox)
        │
        ▼
SyncScheduler ──▶ WorkManager (SyncWorker, NetworkType.CONNECTED, exponential backoff)
        │
        ▼
OutboxSyncer ──▶ Retrofit ApiService ──▶ MockApiServer
                 (OkHttp MockWebServer, in-process, localhost)
```

**Kenapa desainnya begini:**

- **Outbox table terpisah dari Transaction table.** Transaction menyimpan fakta bisnis (produk, qty, total, status); Outbox menyimpan bookkeeping pengiriman (payload JSON, retryCount, lastError). Queue screen baca dari Outbox, bukan Transaction, supaya UI riwayat tetap bersih dari detail retry.
- **Stock decrement, insert Transaction, dan insert Outbox terjadi dalam satu `Room.runInTransaction`.** Kalau proses mati di tengah jalan, tidak ada state setengah-jadi (stok berkurang tapi transaksi tidak tercatat, atau sebaliknya).
- **txId adalah UUID yang di-generate di client**, dipakai sebagai primary key lokal *dan* idempotency key yang dikirim ke API. `MockApiServer` men-dedupe berdasarkan txId ini — request yang sama yang terkirim dua kali (misal karena retry setelah response hilang) tidak diproses dobel di sisi server.
- **MockWebServer jalan in-process di localhost**, bukan server sungguhan. Ini penting untuk demo airplane mode: loopback (127.0.0.1) biasanya tetap jalan meski airplane mode aktif, jadi yang benar-benar memblokir sync bukan MockWebServer-nya, melainkan **WorkManager `NetworkType.CONNECTED` constraint** — itu yang dicek terhadap kondisi jaringan asli perangkat (lihat `SyncScheduler`).
- **`network_security_config.xml` mengizinkan cleartext HTTP khusus ke `localhost`/`127.0.0.1`.** Ketauan pas verifikasi live di emulator: Android API 28+ men-default-block cleartext traffic secara total, termasuk ke loopback — bukan cuma ke domain eksternal. Tanpa ini, semua panggilan ke MockApiServer gagal dengan `CLEARTEXT communication to localhost not permitted`, dan karena `SyncWorker` menangkap `IOException` lalu menyimpannya sebagai `lastError` (bukan silent-swallow), errornya langsung kelihatan jelas di layar Queue — persis skenario "ketauan dari monitoring, bukan dari laporan user" yang dibahas di Kasus 1 Task 3.
- **Logika sync dipisah ke `OutboxSyncer`, lepas dari kelas Worker.** Dependency-nya masuk lewat constructor, bukan diambil dari Application, supaya retry state machine bisa diuji tanpa emulator. `SyncWorker` tinggal wrapper tipis yang menyiapkan dependency lalu delegasi.
- **OutboxSyncer menyapu ulang seluruh outbox tiap kali jalan** (bukan cuma item yang memicunya), supaya beberapa transaksi yang dibuat saat offline ikut ter-sync sekaligus begitu online.
- **Retry policy:** gagal → `retryCount++`, status tetap `PENDING` dan Worker return `Result.retry()` (WorkManager yang atur exponential backoff), sampai `retryCount >= 5` baru status jadi `FAILED` (butuh retry manual dari Queue screen).
- **Format tampilan dikunci ke konvensi Indonesia lewat `Formats`, bukan ikut locale device.** `Locale.getDefault()` bikin harga tampil `Rp18,000` di device en-US — koma di posisi yang mestinya titik. Buat app yang dibaca kasir seharian, itu salah baca nominal, bukan sekadar isu kosmetik.

## Cara menjalankan

Prasyarat: Android Studio (atau JDK 17+ dan Android SDK terpasang manual), emulator/device API 24+.

```bash
./gradlew assembleDebug
```

Buka project di Android Studio lalu Run, atau install manual:

```bash
./gradlew installDebug
```

## Dependency utama

| Layer | Library |
|---|---|
| Local DB | `androidx.room:room-runtime` + `room-compiler` |
| Background sync | `androidx.work:work-runtime` |
| Networking | `retrofit2` + `converter-gson` |
| Mock server | `com.squareup.okhttp3:mockwebserver` (dipakai sebagai runtime dependency, bukan cuma testing) |
| UI | ViewBinding + `androidx.lifecycle` ViewModel/LiveData + Material Components |
| Test | JUnit4 + Robolectric (in-memory Room tanpa emulator) |

## Skenario fungsional (manual test)

1. **Daftar produk + stok** — buka tab Produk, 5 produk ter-seed otomatis saat app pertama kali jalan (`KaspinApp#seedProductsIfEmpty`).
2. **Buat transaksi** — tap "Buat Transaksi" pada produk, isi qty, Simpan. Stok lokal langsung berkurang, transaksi baru muncul di tab Riwayat berstatus `PENDING`, sync otomatis dienqueue.
3. **Riwayat + status sync** — tab Riwayat menampilkan semua transaksi dengan badge status (`PENDING` oranye / `SYNCED` hijau / `FAILED` merah), realtime lewat LiveData.
4. **Monitor queue (retry/error)** — tab Queue menampilkan outbox dengan retry count & pesan error terakhir. Toggle "Simulasikan gagal jaringan" di layar yang sama memaksa mock API merespons 503, untuk melihat langsung alur `PENDING → retry (backoff) → FAILED → tombol Retry`.
5. **Airplane mode → transaksi tetap masuk → sync setelah online** — aktifkan Airplane Mode di perangkat, buat transaksi (tetap tersimpan lokal, status `PENDING`, tersimpan di Outbox), lalu matikan Airplane Mode — WorkManager otomatis menjalankan `SyncWorker` begitu constraint `NetworkType.CONNECTED` terpenuhi, status berubah jadi `SYNCED` tanpa aksi tambahan dari user.

*(Screenshot kelima flow ini dilampirkan terpisah di PDF submission, bukan di repo.)*

## Unit test

```bash
./gradlew testDebugUnitTest
```

18 test, semuanya JVM murni (Robolectric + Room in-memory) — tidak butuh emulator.

- `OutboxSyncerTest` (8) — inti jaminan offline-first: kapan entri bertahan `PENDING` vs kapan diparkir `FAILED`, pesan `IOException` dimunculkan bukan ditelan, seluruh antrean terkuras sekali jalan, satu kegagalan tidak menghambat entri lain, dan antrean kosong tidak memicu panggilan jaringan. Dijalankan lewat MockWebServer sungguhan melalui jalur Retrofit asli, bukan fake buatan sendiri.
- `TransactionRepositoryTest` (4) — stok berkurang benar, insufficient-stock ditolak tanpa mengubah stok, transaksi baru otomatis membuat 1 baris outbox `PENDING`, retry me-reset outbox tanpa duplikasi baris.
- `FormatsTest` (4) — rupiah, tanggal, dan qty tetap ikut konvensi Indonesia walau locale default device sengaja diset ke US lebih dulu.
- `MockApiServerTest` (2) — txId yang sama dikirim dua kali ditandai `duplicate=true` pada request kedua (bukti idempotency end-to-end lewat Retrofit → mock server), dan toggle force-fail benar-benar membuat server merespons error.

### Verifikasi build release (minified)

Karena bug yang paling mahal di POS justru yang cuma muncul saat minify aktif, build release tidak berhenti diverifikasi di tahap kompilasi:

```bash
./gradlew assembleRelease
```

APK hasilnya ditandatangani dengan debug keystore, dipasang di emulator, lalu dipakai membuat transaksi sungguhan sampai berstatus `SYNCED` — memastikan keep-rule ProGuard untuk model Gson, interface Retrofit, dan Worker benar-benar cukup. Tidak ada exception maupun `ClassNotFoundException` di logcat.

## Known limitation (scope cut, disengaja)

Login/multi-user, printer, QRIS, multi-cabang, conflict-merge server yang canggih, dan enkripsi at-rest **sengaja tidak diimplementasikan** — di luar scope mini app 5 hari ini. Untuk conflict resolution di skala production, precedent yang relevan dari pengalaman saya: migrasi Firestore SSOT di Amartha memakai **LWW (Last-Write-Wins)** — sederhana dan cukup aman untuk kebanyakan kasus lapangan tanpa perlu conflict-merge kompleks; kalau scope-nya diperluas, itu arah yang akan saya pakai duluan sebelum naik ke merge strategy yang lebih rumit.

## AI usage log

| Bagian | AI dipakai? | Yang direview/diubah manual |
|---|---|---|
| Scaffold Gradle / Room schema | Ya | Nama tabel & kolom, index unique pada `txId`, keputusan pisah Outbox dari Transaction |
| Sync worker (WorkManager) | Ya | Aturan retry (max 5 percobaan → FAILED), constraint jaringan, race `apiService` null saat startup |
| Verifikasi live di emulator | Manual | Jalanin 5 flow di Android emulator sungguhan (bukan cuma unit test) — nemuin bug nyata: cleartext HTTP ke localhost diblokir default oleh Android API 28+, di-fix dengan `network_security_config.xml` |
| Mock API server | Ya | Logika dedupe idempotency, response code untuk force-fail toggle |
| Unit test | Ya | Skenario yang dipilih (stok, outbox, idempotency, retry) berdasarkan risiko nyata yang pernah saya temui, bukan template generik. Cakupan sengaja diperluas ke retry state machine karena di situ inti jaminan offline-first-nya |
| Review kualitas akhir | Manual | Nemu format rupiah yang ikut locale device (`Rp18,000` alih-alih `Rp18.000`) dan waktu transaksi yang disimpan tapi tidak pernah ditampilkan — dua-duanya lolos dari semua test karena secara teknis benar, tapi salah menurut konteks domain |
| Verifikasi build release | Manual | Menjalankan APK minified di emulator sampai transaksi `SYNCED`, bukan cuma memastikan `assembleRelease` sukses |
| README / dokumentasi | Ya | Fakta arsitektur & keputusan disesuaikan dengan yang benar-benar diimplementasikan |
