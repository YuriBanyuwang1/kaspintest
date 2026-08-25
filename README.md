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
Retrofit ApiService ──▶ MockApiServer (OkHttp MockWebServer, in-process, localhost)
```

**Kenapa desainnya begini:**

- **Outbox table terpisah dari Transaction table.** Transaction menyimpan fakta bisnis (produk, qty, total, status); Outbox menyimpan bookkeeping pengiriman (payload JSON, retryCount, lastError). Queue screen baca dari Outbox, bukan Transaction, supaya UI riwayat tetap bersih dari detail retry.
- **Stock decrement, insert Transaction, dan insert Outbox terjadi dalam satu `Room.runInTransaction`.** Kalau proses mati di tengah jalan, tidak ada state setengah-jadi (stok berkurang tapi transaksi tidak tercatat, atau sebaliknya).
- **txId adalah UUID yang di-generate di client**, dipakai sebagai primary key lokal *dan* idempotency key yang dikirim ke API. `MockApiServer` men-dedupe berdasarkan txId ini — request yang sama yang terkirim dua kali (misal karena retry setelah response hilang) tidak diproses dobel di sisi server.
- **MockWebServer jalan in-process di localhost**, bukan server sungguhan. Ini penting untuk demo airplane mode: loopback (127.0.0.1) biasanya tetap jalan meski airplane mode aktif, jadi yang benar-benar memblokir sync bukan MockWebServer-nya, melainkan **WorkManager `NetworkType.CONNECTED` constraint** — itu yang dicek terhadap kondisi jaringan asli perangkat (lihat `SyncScheduler`).
- **SyncWorker menyapu ulang seluruh outbox tiap kali jalan** (bukan cuma item yang memicunya), supaya beberapa transaksi yang dibuat saat offline ikut ter-sync sekaligus begitu online.
- **Retry policy:** gagal → `retryCount++`, status tetap `PENDING` dan Worker return `Result.retry()` (WorkManager yang atur exponential backoff), sampai `retryCount >= 5` baru status jadi `FAILED` (butuh retry manual dari Queue screen).

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

- `TransactionRepositoryTest` — stok berkurang benar, insufficient-stock ditolak tanpa mengubah stok, transaksi baru otomatis membuat 1 baris outbox `PENDING`, retry me-reset outbox tanpa duplikasi baris.
- `MockApiServerTest` — txId yang sama dikirim dua kali ditandai `duplicate=true` pada request kedua (bukti idempotency end-to-end lewat Retrofit → mock server), dan toggle force-fail benar-benar membuat server merespons error.

## Known limitation (scope cut, disengaja)

Login/multi-user, printer, QRIS, multi-cabang, conflict-merge server yang canggih, dan enkripsi at-rest **sengaja tidak diimplementasikan** — di luar scope mini app 5 hari ini. Untuk conflict resolution di skala production, precedent yang relevan dari pengalaman saya: migrasi Firestore SSOT di Amartha memakai **LWW (Last-Write-Wins)** — sederhana dan cukup aman untuk kebanyakan kasus lapangan tanpa perlu conflict-merge kompleks; kalau scope-nya diperluas, itu arah yang akan saya pakai duluan sebelum naik ke merge strategy yang lebih rumit.

## AI usage log

| Bagian | AI dipakai? | Yang direview/diubah manual |
|---|---|---|
| Scaffold Gradle / Room schema | Ya | Nama tabel & kolom, index unique pada `txId`, keputusan pisah Outbox dari Transaction |
| Sync worker (WorkManager) | Ya | Aturan retry (max 5 percobaan → FAILED), constraint jaringan, race `apiService` null saat startup |
| Mock API server | Ya | Logika dedupe idempotency, response code untuk force-fail toggle |
| Unit test | Ya | Skenario yang dipilih (stok, outbox, idempotency, retry) berdasarkan risiko nyata yang pernah saya temui, bukan template generik |
| README / dokumentasi | Ya | Fakta arsitektur & keputusan disesuaikan dengan yang benar-benar diimplementasikan |
