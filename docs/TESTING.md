# Uji SAVR v0.2 di perangkat (TECNO Camon 50 Ultra 5G)

## A. Instalasi & migrasi
- [ ] Pasang di atas v0.1 (kunci sama): semua transaksi lama masih ada, tertaut ke goal "Target Utama", saldo sama.
- [ ] Instal bersih: muncul satu goal "Target Utama"; splash tampil lalu Home.
- [ ] Tutup paksa & restart perangkat: data tetap ada.

## B. Fungsi
- [ ] Tambah pemasukan & pengeluaran; edit nominal/kategori/goal/tanggal; hapus (dengan konfirmasi).
- [ ] Tolak: nominal 0, penarikan melebihi saldo goal, penarikan bertanggal sebelum setoran, hapus setoran yang sudah "dipakai", tanggal masa depan.
- [ ] Goal: tambah, edit nama/target, jadikan utama, setor ke goal, hapus (saldo pindah ke "Tanpa goal").
- [ ] History: kombinasi filter jenis + kategori + tanggal (7 hari, 30 hari, bulan ini, rentang); reset filter.
- [ ] Search: nama catatan, kategori, nominal ("50000"), nama goal.
- [ ] Analytics: grafik berubah sesuai rentang; ketuk titik/batang menampilkan nilai; angka cocok dengan History.
- [ ] Backup → hapus data (uninstall/reinstall) → Restore: semua kembali identik. Coba restore file rusak: ditolak, data lama aman.
- [ ] Profile: ganti nama, tema Gelap/Terang/Sistem, matikan animasi & getaran, tersimpan setelah restart.

## C. Adaptif
- [ ] Font sistem terbesar: tidak ada teks terpotong parah / tombol tak terjangkau.
- [ ] Putar layar: state & overlay tetap; mode terang: semua teks terbaca di atas kaca.
- [ ] Mode hemat daya: background berhenti bergerak.

## D. Performa (jalankan via adb)
```
adb shell am start -W -n com.savr.app/.MainActivity                 # waktu start (TotalTime)
adb shell dumpsys gfxinfo com.savr.app reset
# geser antarhalaman & scroll History ~30 detik
adb shell dumpsys gfxinfo com.savr.app | grep -E "Janky|percentile"   # target janky < 5%
adb shell dumpsys meminfo com.savr.app | grep -E "TOTAL PSS|Graphics"  # catat PSS Home vs Analytics
```
Uji dengan data besar (mis. 3.000 transaksi hasil Restore) dan catat jank saat membuka History/Analytics.
Jika jank > 5%: matikan "Animasi liquid" di Profile dan bandingkan.

## E. Rilis
- [ ] `apksigner verify` lulus; ukuran APK rilis dicatat.
- [ ] Terpasang tanpa peringatan merah; Play Protect tidak memblokir.
