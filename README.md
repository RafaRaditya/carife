# Carife — Android MVP

Aplikasi Android untuk mencari kafe tempat nugas. Seluruh layar inti mengikuti prototipe di folder induk.

Ada **dua varian build** dari satu proyek:

| Varian | Paket | Data kafe | Butuh internet? |
| --- | --- | --- | --- |
| `online` | `com.carife.app` | Overpass API langsung + cache | Ya, untuk memuat kota baru |
| `offline` | `com.carife.app.offline` | **Snapshot OpenStreetMap di dalam APK** | **Tidak sama sekali** |

Keduanya memakai basis kode yang sama; yang berbeda hanya flag `BuildConfig.OFFLINE_ONLY` dan sumber data awal.

## Status

Build debug untuk kedua varian berjalan, **58 unit test lolos**, dan keduanya sudah diuji di emulator (Pixel 6a, API 37).

Varian offline diuji dengan **mode pesawat aktif**: Home memuat 95 kafe Bekasi (92 dari OSM + 3 hero demo), ganti kota ke Jakarta menampilkan 604 kafe, detail menampilkan jam `Mo-Su 07:00–21:00` dan website asli, pencarian "kopi" menyaring 604 → 103 hasil. Tidak ada satu pun request jaringan.

Peta penuh dan section **Peta** memakai tile **OpenStreetMap** lewat osmdroid, jadi gambarnya butuh internet (tile yang sudah pernah dibuka tersimpan di cache). Tanpa koneksi, pin dan daftar kafe di layar peta tetap berfungsi. Preview kecil di Home tetap ilustrasi lokal.

| Layar | Status | Catatan |
| --- | --- | --- |
| Home Discovery | Berfungsi | Kafe nyata per kota + data contoh lengkap, jumlah buka dihitung dari data, preview peta pin ilustratif, peta penuh OSM yang bisa digeser/zoom, pemilih kota |
| Explore & Filters | Berfungsi | Filter AND, sort, panel bottom sheet, peta OSM interaktif, empty state yang menjelaskan |
| Cafe Detail | Berfungsi | Semua field OSM + simulasi kepadatan, menu, dan ulasan contoh |
| Bandingkan | Berfungsi | Matriks 2–3 kafe digeser horizontal, sorotan Cocok Nugas & Paling Hemat |
| Teman | Berfungsi | Konten demo berlabel, tautan ke kafe OSM nyata, form preferensi tersimpan |

## Cakupan kota & data offline

13 kota: Bekasi, Jakarta, Depok, Tangerang, Tangerang Selatan, Bogor, Bandung, Semarang, Yogyakarta, Surabaya, Malang, Medan, Makassar.

Snapshot offline ada di `app/src/main/assets/offline/<cityId>.json` (~3.100 kafe, 835 KB), diambil sekali dari Overpass saat menyiapkan proyek. Isinya kafe **nyata**, bukan data karangan. Karena itu `BundledCatalogAssetsTest` ikut memvalidasi berkas ini setiap kali test dijalankan.

Untuk memperbarui snapshot (misal setahun kemudian):

```powershell
$d = "app\src\main\assets\offline"
$q = '[out:json][timeout:90];nwr["amenity"="cafe"](around:8000,-6.238300,106.975600);out center tags;'
Set-Content -Path "$env:TEMP\q.txt" -Value $q -Encoding ascii -NoNewline
curl.exe -s --max-time 150 -A "Carife/1.0" --data-binary "@$env:TEMP\q.txt" -o "$d\bekasi.json" https://overpass-api.de/api/interpreter
```

Koordinat dan radius tiap kota ada di `app/src/main/assets/cities.json`.

## Catatan penting soal data

**Identitas kafe dari OpenStreetMap; angka kelengkapannya data contoh.**

Dari OSM diambil apa adanya: nama, koordinat, alamat, jam operasional, website, dan fasilitas yang memang tercatat. OSM **tidak menyediakan** harga, kecepatan Wi-Fi (Mbps), jumlah colokan, rating, kepadatan, menu, atau ulasan — jadi field itu diisi lapisan demo (`data/demo/`) dan **selalu diberi badge "Contoh"** di kartu, detail, dan tabel banding.

Dua sumber data contoh:

| Sumber | Isi |
| --- | --- |
| `DemoProfiles` | Kurasi manual untuk kafe yang tampil di prototipe. Angkanya mengikuti video: Kopitagram 50 Mbps / Rp22–45rb / 4.8 (330 ulasan) / kepadatan SEDANG 45%. Simetri 75 Mbps sesuai spesifikasi. |
| `DemoDataGenerator` | Angka deterministik dari `id` kafe untuk sisanya, supaya semua kartu terlihat lengkap dan nilainya tidak berubah saat aplikasi dibuka ulang. |

**Tiga kafe hero tidak ada di OSM**, jadi ditambahkan sebagai entri demo di koordinat asli area Bekasi: `Kopitagram Galaxy Bekasi`, `Koma Junkyard Bekasi Timur`, `Titik Temu Galaxy Bekasi`. Semuanya `isDemoData = true` dan hanya muncul untuk kota `bekasi`.

Konsekuensinya:

- Filter harga, Wi-Fi, colokan, kepadatan, dan suasana **berfungsi penuh** (sebelumnya chip ini disabled karena field OSM kosong).
- Matriks perbandingan terisi, bukan "Belum ada data" di semua baris.
- Jarak tetap dihitung haversine dari koordinat nyata, bukan angka tabel spesifikasi.
- Alamat yang tidak ada di OSM tetap tampil "Belum ada data" — overlay tidak mengarang alamat.

Kalau nanti ingin kembali ke data murni OSM, cukup lewati `DemoDataOverlay.apply()` di `CafeRepository.decorate()`; badge "Contoh" otomatis hilang karena bergantung pada `cafe.isDemoData`.

## Build & jalankan

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"

# varian online
.\gradlew.bat assembleOnlineDebug
# varian offline
.\gradlew.bat assembleOfflineDebug

# test kedua varian
.\gradlew.bat testOnlineDebugUnitTest testOfflineDebugUnitTest
```

APK:

- `app/build/outputs/apk/online/debug/app-online-debug.apk` → `com.carife.app`
- `app/build/outputs/apk/offline/debug/app-offline-debug.apk` → `com.carife.app.offline`

```powershell
adb install -r .\app\build\outputs\apk\offline\debug\app-offline-debug.apk
adb shell am start -n com.carife.app.offline/com.carife.app.MainActivity
```

Kedua varian bisa terpasang bersamaan karena package ID-nya berbeda.

`local.properties` berisi `sdk.dir` dan tidak di-commit.

## Versi tooling

Gradle 8.13 · AGP 8.13.0 · Kotlin 2.1.20 · Compose BOM 2025.06.01 · `compileSdk 36` · `minSdk 26` · JDK 17 target (jalan di JDK 21).

## Struktur

- `core/model` — `Cafe`, `City`, `UserPreferences`, `ExploreFilters`, `DemoTypes`
- `data/remote` — `OverpassClient`, `OverpassQuery`, `OsmCafeMapper`
- `data/local` — `BundledCatalog` (snapshot offline di assets), `CafeCache` (TTL 30 menit), `PreferencesStore` (DataStore)
- `data/demo` — `DemoProfiles` (kurasi + kafe hero), `DemoDataGenerator` (angka deterministik), `DemoDataOverlay` (titik suntik)
- `data` — `CafeRepository` (offline-first: bundel → cache → jaringan opsional), `CityRepository`
- `domain` — `FilterEngine`, `MatchScorer`, `OpeningHours`, `DistanceUtil`, `CompareAdvisor`, `ShareTextBuilder`
- `ui` — tema, komponen (`IllustratedMap` untuk preview, `OsmMapView` untuk peta penuh), dan lima layar + `CarifeRoot` (navigasi)

## Test

58 unit test JVM di `app/src/test/`:

```powershell
.\gradlew.bat test
```

Mencakup aturan filter (termasuk Wi-Fi ketat, suasana, colokan, dan batas harga), skor match deterministik, parser jam operasional (`24/7`, rentang lewat tengah malam, format tak dikenal), pemilihan "Paling Hemat", pemetaan tag Overpass termasuk dedupe, validasi 13 berkas snapshot offline, dan checklist penerimaan §7 pada katalog Bekasi berlapis data demo.
