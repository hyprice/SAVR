# SAVR v0.2 — Powerful Beta

Aplikasi tabungan Android offline-first dengan antarmuka **Liquid Glass**
(Cobalt Blue · Deep Navy · Cyan · Violet · putih transparan).

## Fitur
- **Home / Search / Analytics / History / Profile** dengan floating navigation bar cair, swipe antarhalaman, dan tombol "+" cepat.
- **Multi-goals**: target nominal sendiri, progress, sisa, tambah/edit/hapus, tentukan goal utama.
- **Transaksi**: pemasukan, pengeluaran/penarikan, edit, hapus, kategori, detail, tanggal & waktu.
- **Validasi saldo ketat**: saldo tiap goal tidak boleh negatif pada titik waktu mana pun (termasuk saat edit/hapus/backdate).
- **History**: filter jenis, kategori, dan tanggal (7 hari, 30 hari, bulan ini, rentang kustom).
- **Search**: transaksi (catatan, kategori, nominal, goal) dan goal.
- **Analytics**: grafik perkembangan saldo (30 hari / 6 / 12 bulan), pemasukan vs pengeluaran per bulan, statistik per kategori, progress semua goal, ringkasan.
- **Backup & Restore** file JSON lewat Storage Access Framework (tanpa izin), divalidasi sebelum menimpa data.
- **Adaptif**: tema Gelap/Terang/Ikuti sistem dengan kontras otomatis, layout fleksibel untuk font besar & layar lebar, animasi otomatis mati di mode hemat daya.
- Ikon launcher adaptif (+monokrom Android 13), splash screen, halaman About (Hyprice).

## Struktur kode (`app/src/main/java/com/savr/app`)
| File | Isi |
|---|---|
| `Data.kt` | Room v2: `transactions`, `goals`, `settings` + migrasi 1→2 |
| `Logic.kt` | Kategori, `Rules` (validasi), `Fmt` (rupiah/tanggal), `Analytics`, `UiModel` |
| `SavrViewModel.kt` | State, aksi, backup/restore, penanganan error DB |
| `Backup.kt` | Serialisasi & validasi JSON backup |
| `Glass.kt` | Sistem desain: palet, background cair, panel kaca, komponen |
| `Charts.kt` | Grafik Canvas |
| `Screens.kt` | Lima halaman utama |
| `Sheets.kt` | Editor transaksi/goal, detail, konfirmasi, About, `Branding` |
| `MainActivity.kt` | Activity, tema, pager, navigation bar |

## Build & rilis
Lihat [`docs/RELEASE.md`](docs/RELEASE.md) (penandatanganan, ukuran APK, pemeriksaan) dan [`docs/TESTING.md`](docs/TESTING.md) (uji di perangkat).

```
./gradlew testDebugUnitTest          # unit test logika
./gradlew assembleRelease            # APK rilis (R8 + shrink resources)
```

## Penghematan ruang
- `material3` dan `material-icons-extended` dibuang (UI memakai komponen sendiri; ikon digambar dengan Canvas / icons-core).
- Rilis memakai R8 `minify` + `shrinkResources`, hanya bahasa `id` & `en`.
- Tanpa gambar raster, font kustom, maupun library grafik: ikon, background, dan grafik digambar kode/vektor.
