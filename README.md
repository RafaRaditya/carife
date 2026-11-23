# Carife — Android MVP

Aplikasi Android untuk menemukan kafe tempat nugas. Pengguna bisa mencari berdasarkan kebutuhan (Wi-Fi, colokan, suasana, zonasi rokok, harga, jarak), melihat detail, membandingkan kafe, dan mengatur rencana nugas bersama teman.

Sekarang hanya ada **satu APK**:

| Item | Nilai |
| --- | --- |
| Nama aplikasi | Carife |
| Package | `com.carife.app` |
| Versi | 1.2 (versionCode 3) |
| APK debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Internet | Opsional untuk katalog; tile peta OSM butuh internet |

Tidak ada lagi flavor `online`/`offline`. Snapshot katalog OpenStreetMap tetap dibundel di dalam APK yang sama, sehingga layar inti bisa dibuka tanpa koneksi. Jika di perangkat masih terpasang **Carife Offline** lama (`com.carife.app.offline`), uninstall aplikasi itu karena varian terpisah sudah dihapus.

## Status

- Build `assembleDebug` berhasil dan **89 unit test JVM** lulus di 15 kelas test.
- Diuji di emulator Pixel 6a (API 37).
- Peta memakai tile OpenStreetMap lewat osmdroid; tile yang sudah pernah dibuka tersimpan di cache. Tanpa internet, pin dan daftar kafe di layar peta tetap berfungsi.

## Fitur

| Layar | Isi |
| --- | --- |
| Home | Masthead + pencarian, jumlah buka sekarang, peta OSM interaktif, kartu kafe unggulan, daftar kafe dengan foto dan aksi favorit/banding |
| Peta / Jelajah | Pencarian, chip cepat, peta hasil, filter lengkap, sort Kecocokan/Terdekat/Termurah, compare tray |
| Detail Kafe | Foto hero, ringkasan fakta, simulasi kepadatan, fasilitas, menu, ulasan, rute, favorit, banding, dan CTA utama **Buat room di kafe ini** |
| Bandingkan | Matriks 2–3 kafe yang bisa digeser, sorotan Cocok Nugas/Paling Hemat, Saran Carife, rute cepat |
| Teman | Satu feed berisi check-in, room nongkrong, dan aktivitas teman. Bisa gabung/keluar room, check-in dari room, dan membuat room dengan pencarian kafe + pemilih tanggal/jam |
| Profil & Pengaturan | Profil lokal, statistik favorit/match/teman, preferensi nugas, avatar, kota utama |
| Teman Saya | Daftar teman dan saran teman dengan pencarian |

## Data & foto

Identitas kafe berasal dari OpenStreetMap. Field yang OSM tidak menyediakan — harga, kecepatan Wi-Fi, jumlah colokan, rating, kepadatan, menu, dan ulasan — diisi lapisan demo di `data/demo/`.

- `DemoProfiles` berisi kurasi manual untuk kafe yang tampil di prototipe.
- `DemoDataGenerator` mengisi angka deterministik dari `id` kafe untuk kafe lainnya, sehingga nilainya stabil saat aplikasi dibuka ulang.
- Tiga kafe hero yang tidak ada di OSM ditambahkan sebagai entri demo di koordinat asli area Bekasi: Kopitagram Galaxy Bekasi, Koma Junkyard Bekasi Timur, dan Titik Temu Galaxy Bekasi.
- Overlay dipanggil di `CafeRepository.load` dan `refresh` lewat `DemoDataOverlay.apply()`.
- Layar Detail menandai menu/ulasan sebagai contoh dan kepadatan sebagai simulasi. Header Teman juga menyebut konten demo. Kartu kafe tidak memakai badge khusus.

Foto suasana adalah **9 gambar contoh dari prototipe** yang dibundel di `app/src/main/assets/photos/`. `DemoCafePhotos` memetakan foto kurasi ke kafe demo dan memilih foto contoh secara deterministik untuk kafe lain. Foto ini ilustrasi suasana, bukan verifikasi foto resmi tiap kafe.

## Cakupan kota & snapshot offline

13 kota: Bekasi, Jakarta, Depok, Tangerang, Tangerang Selatan, Bogor, Bandung, Semarang, Yogyakarta, Surabaya, Malang, Medan, Makassar.

Snapshot ada di `app/src/main/assets/offline/<cityId>.json` (sekitar 3.100 kafe), diambil sekali dari Overpass saat menyiapkan proyek. Isinya kafe nyata, bukan data karangan. `BundledCatalogAssetsTest` memvalidasi berkas snapshot setiap kali test dijalankan.

Untuk memperbarui snapshot:

```powershell
$d = "app\src\main\assets\offline"
$q = '[out:json][timeout:90];nwr["amenity"="cafe"](around:8000,-6.238300,106.975600);out center tags;'
Set-Content -Path "$env:TEMP\q.txt" -Value $q -Encoding ascii -NoNewline
curl.exe -s --max-time 150 -A "Carife/1.0" --data-binary "@$env:TEMP\q.txt" -o "$d\bekasi.json" https://overpass-api.de/api/interpreter
```

Koordinat dan radius tiap kota ada di `app/src/main/assets/cities.json`.

## Build & jalankan

```powershell
# opsional: arahkan ke JDK yang dipakai Android Studio
# $env:JAVA_HOME="C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"

.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

APK tunggal:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install dan buka:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.carife.app/.MainActivity
```

`local.properties` berisi `sdk.dir` dan tidak di-commit.

## Versi tooling

Gradle 8.13 · AGP 8.13.0 · Kotlin 2.1.20 · Compose BOM 2025.06.01 · `compileSdk 36` · `targetSdk 36` · `minSdk 26` · JDK 17 target (jalan di JDK 21).

## Struktur

- `core/model` — `Cafe`, `City`, `UserPreferences`, `ExploreFilters`, `SocialModels`, `DemoTypes`
- `data/remote` — `OverpassClient`, `OverpassQuery`, `OsmCafeMapper`
- `data/local` — `BundledCatalog` (snapshot di assets), `CafeCache` (TTL 30 menit), `PreferencesStore` (DataStore), `ProfilePhotoStore`
- `data/demo` — `DemoProfiles`, `DemoDataGenerator`, `DemoDataOverlay`, `DemoCafePhotos`
- `data` — `CafeRepository` (bundel → cache → jaringan opsional), `CityRepository`, `DemoFriendsData`
- `domain` — `FilterEngine`, `MatchScorer`, `OpeningHours`, `DistanceUtil`, `CompareAdvisor`, `ShareTextBuilder`, `SocialRules`
- `ui/theme` — warna, tipografi Plus Jakarta Sans, shape
- `ui/components` — `CafeCard`, `CafeMapCard`, `CafePhotoPlaceholder`, `CafePicker`, `CarifeSearchField`, `FilterPanel`, `FullMapDialog`, komponen primitif
- `ui` — `CarifeRoot`, `CarifeViewModel`, layar Home, Explore, Detail, Compare, Teman, Profile; `ui/teman` untuk room, aktivitas, dan dialog room

## Test

89 unit test JVM di `app/src/test/`:

```powershell
.\gradlew.bat testDebugUnitTest
```

Mencakup aturan filter (termasuk Wi-Fi ketat, radius dekat/sedang/jauh, suasana, colokan, dan batas harga), skor match deterministik, parser jam operasional (`24/7`, rentang lewat tengah malam, format tak dikenal), pemilihan “Paling Hemat”, pemetaan tag Overpass termasuk dedupe, validasi snapshot 13 kota, aturan sosial/room, dan checklist penerimaan katalog Bekasi berlapis data demo.
