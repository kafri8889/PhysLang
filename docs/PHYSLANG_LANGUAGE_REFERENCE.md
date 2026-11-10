# PhysLang Language Reference

Dokumentasi resmi bahasa **PhysLang**: DSL (*domain-specific language*) untuk perhitungan fisika klasik dengan **analisis dimensi yang ketat**. Dokumen ini menjelaskan fitur bahasa sesuai implementasi saat ini (bukan rencana), lengkap dengan grammar EBNF.

> Semua contoh di dokumen ini sudah sesuai perilaku interpreter. Contoh yang bisa langsung dijalankan tersedia di folder `assets/` dan `assets/fitur/`.

---

## Daftar Isi

1. [Gambaran Umum](#1-gambaran-umum)
2. [Menjalankan Program](#2-menjalankan-program)
3. [Struktur Leksikal](#3-struktur-leksikal)
4. [Besaran Fisika dan Model Dimensi](#4-besaran-fisika-dan-model-dimensi)
5. [Satuan Bawaan](#5-satuan-bawaan)
6. [Ekspresi dan Operator](#6-ekspresi-dan-operator)
7. [Variabel](#7-variabel)
8. [Satuan Kustom (`unit`)](#8-satuan-kustom-unit)
9. [Konversi Satuan (`in`)](#9-konversi-satuan-in)
10. [Rumus (`form`)](#10-rumus-form)
11. [Fungsi Bawaan](#11-fungsi-bawaan)
12. [Percabangan (`if` / `else if` / `else`)](#12-percabangan-if--else-if--else)
13. [Blok dan Lingkup](#13-blok-dan-lingkup)
14. [Output (`print`)](#14-output-print)
15. [Null](#15-null)
16. [Diagnostik dan Error](#16-diagnostik-dan-error)
17. [Grammar Lengkap (EBNF)](#17-grammar-lengkap-ebnf)
18. [Arsitektur Interpreter](#18-arsitektur-interpreter)
19. [Batasan yang Diketahui](#19-batasan-yang-diketahui)
20. [Contoh Program Lengkap](#20-contoh-program-lengkap)

---

## 1. Gambaran Umum

Di PhysLang, angka bukan sekadar `Double`. Setiap nilai numerik adalah **besaran fisika** yang membawa magnitudo, vektor dimensi SI, skala satuan, dan label satuan. Akibatnya, interpreter dapat menolak ekspresi yang tidak bermakna secara fisika **sebelum program dieksekusi**:

```phys
mass = 5 kg
length = 10 m
print mass + length      // ERROR: Dimension mismatch
```

Ciri utama bahasa:

| Fitur | Contoh |
| --- | --- |
| Literal besaran | `10 kg`, `9.8 m/s^2`, `4 m^2` |
| Aritmetika berdimensi | `force = mass * acceleration` |
| Satuan kustom | `unit kph = 1000 m / 3600 s` |
| Konversi satuan | `distance in km` |
| Rumus fisika | `form Force(m, a) = m * a` |
| Percabangan berbasis relasi fisis | `if (T between 273 K and 373 K) ...` |
| Uji dimensi | `if (q is N) "force" else "other"` |
| Toleransi pengukuran | `if (g == 9.81 m/s^2 within 0.05 m/s^2) ...` |

PhysLang sengaja **bukan** bahasa serba guna: tidak ada kelas, array, loop umum, atau tipe boolean sebagai nilai. Fokusnya adalah ekspresi fisika yang benar secara dimensi.

---

## 2. Menjalankan Program

File program berekstensi `.phys`.

```bash
# Kompilasi
./gradlew classes            # Windows: .\gradlew.bat classes

# Jalankan satu atau beberapa file
./run.sh assets/fitur/15_if_else.phys

# Tanpa argumen: menu interaktif
#   1. CLI interaktif (REPL, ketik `exit` untuk keluar)
#   2. Jalankan file dari path
#   3. Daftar file contoh fitur (assets/fitur)
#   4. Daftar file contoh program (assets)
./run.sh

# Tes otomatis
./gradlew test               # Windows: .\gradlew.bat test
```

Setiap program melewati tahap: **lexer → parser → semantic analyzer → evaluator**. Jika ada error sintaks atau semantik, program **tidak dieksekusi sama sekali** dan semua diagnostik dicetak.

---

## 3. Struktur Leksikal

### 3.1 Baris dan pemisah statement

- Statement dipisahkan oleh **baris baru** (`\n`). Tidak ada titik koma.
- Karakter `\r` diabaikan, sehingga file berformat CRLF juga didukung.
- Spasi dan tab diabaikan.

### 3.2 Komentar

Komentar satu baris diawali `//` atau `#`, dan boleh diletakkan di akhir baris:

```phys
// Ini komentar
# Ini juga komentar
speed = 20 m/s   // komentar di akhir baris
mass = 2 kg      # komentar di akhir baris
```

Tanda `#` di dalam string bukan komentar: `print "Sample #1"`.

### 3.3 Identifier

- Diawali **huruf** atau **underscore**, diikuti huruf, digit, atau underscore: `mass`, `v0`, `kinetic_energy`, `_temp`.
- Huruf Unicode diizinkan, sehingga `µm` dan `Ω` adalah identifier yang valid.
- Peka huruf besar/kecil: `m` (meter) berbeda dengan `M`.

### 3.4 Kata kunci (keyword)

```text
form   unit   print   in     if      else
null   is     within  between and    true   false
```

Keyword tidak dapat dipakai sebagai nama variabel. `true` dan `false` sudah dicadangkan tetapi belum dapat dipakai sebagai nilai (lihat [Batasan](#19-batasan-yang-diketahui)).

### 3.5 Literal angka

- Bilangan bulat: `42`
- Bilangan desimal: `3.14` (harus ada digit setelah titik)
- Notasi ilmiah: `1e3`, `6.674e-11`, `2E+5`. Huruf `e`/`E` hanya dianggap eksponen bila langsung diikuti digit (boleh didahului `+`/`-`), sehingga `3eV` tetap dibaca sebagai `3` dengan satuan `eV`.
- Angka tanpa satuan adalah besaran **tak berdimensi** (*dimensionless*).

### 3.6 Literal string

```phys
print "Hello, physics!"
```

String diapit tanda kutip ganda dan tidak mendukung escape sequence. String hanya dipakai untuk output dan label.

### 3.7 Operator dan tanda baca

| Kategori | Token |
| --- | --- |
| Aritmetika | `+` `-` `*` `/` `^` |
| Pembanding (hanya di kondisi `if`) | `<` `<=` `>` `>=` `==` `!=` |
| Assignment | `=` `+=` `-=` `*=` `/=` |
| Pengelompokan | `(` `)` `{` `}` |
| Pemisah argumen | `,` |

---

## 4. Besaran Fisika dan Model Dimensi

### 4.1 `PhysicsValue`

Setiap besaran di runtime direpresentasikan oleh `PhysicsValue`:

| Field | Arti |
| --- | --- |
| `value` | Magnitudo mentah sesuai satuan yang ditulis |
| `dimensions` | Vektor 7 bilangan bulat: pangkat tiap dimensi dasar SI |
| `scale` | Faktor pengali terhadap satuan dasar SI |
| `unitName` | Label satuan untuk tampilan (opsional) |

Nilai dalam SI = `value × scale`. Contoh: `2 km` disimpan sebagai `value = 2`, `scale = 1000`, sehingga bernilai `2000 m` dalam SI.

### 4.2 Tujuh dimensi dasar SI

| Indeks | Besaran dasar | Satuan dasar |
| --- | --- | --- |
| 0 | Massa | `kg` |
| 1 | Panjang | `m` |
| 2 | Waktu | `s` |
| 3 | Arus listrik | `A` |
| 4 | Suhu | `K` |
| 5 | Jumlah zat | `mol` |
| 6 | Intensitas cahaya | `cd` |

Contoh vektor dimensi:

| Besaran | Satuan | Vektor `[M, L, T, I, Θ, N, J]` |
| --- | --- | --- |
| Kecepatan | `m/s` | `[0, 1, -1, 0, 0, 0, 0]` |
| Gaya | `N` | `[1, 1, -2, 0, 0, 0, 0]` |
| Energi | `J` | `[1, 2, -2, 0, 0, 0, 0]` |
| Tak berdimensi | angka biasa | `[0, 0, 0, 0, 0, 0, 0]` |

### 4.3 Literal besaran

Angka yang **langsung diikuti** nama satuan membentuk literal besaran:

```phys
mass = 10 kg
area = 4 m^2        // pangkat menempel pada satuan: 4 (m^2), bukan (4 m)^2
wavenumber = 3 m^-1
speed = 20 m/s      // = (20 m) / s
```

Pangkat bilangan bulat setelah satuan (`m^2`, `s^-1`) terikat langsung ke satuannya.

---

## 5. Satuan Bawaan

| Kelompok | Satuan |
| --- | --- |
| Dasar SI | `kg` `m` `s` `A` `K` `mol` `cd` |
| Massa | `g` `mg` `t` |
| Panjang | `km` `cm` `mm` `µm` `nm` |
| Waktu | `ms` `min` `h` |
| Frekuensi | `Hz` |
| Gaya | `N` `kN` |
| Tekanan | `Pa` `kPa` `MPa` `bar` `atm` |
| Energi | `J` `kJ` `MJ` `cal` `kcal` `eV` `kWh` |
| Daya | `W` `kW` `MW` `mW` |
| Kelistrikan | `C` `V` `mV` `kV` `Ω` `ohm` `kΩ` `MΩ` `A` `mA` `µA` |
| Kapasitansi | `F` `µF` `nF` `pF` |
| Induktansi | `H` `mH` `µH` |
| Magnet | `T` `Wb` |
| Konduktansi | `S` |
| Sudut (tak berdimensi) | `rad` `deg` |

Nama satuan juga dapat dipakai sebagai nilai `1 <satuan>` di dalam ekspresi, misalnya `100 kg * m / s^2`.

---

## 6. Ekspresi dan Operator

### 6.1 Aturan dimensi aritmetika

| Operasi | Syarat | Dimensi hasil |
| --- | --- | --- |
| `a + b`, `a - b` | Dimensi `a` dan `b` sama (atau `b` tak berdimensi) | Dimensi `a` |
| `a * b` | Selalu valid | Pangkat dijumlahkan |
| `a / b` | Selalu valid | Pangkat dikurangkan |
| `a ^ n` | `n` **tak berdimensi** dan **bilangan bulat** | Pangkat dikali `n` |
| `-a` | `a` besaran | Sama dengan `a` |

```phys
print 2 km + 300 m      // 2.3 km   (hasil memakai satuan operand kiri)
print 300 m + 2 km      // 2300.0 m
print 2 m * 3 m         // 6.0 m^2
print (2 m)^0.5         // ERROR: '^' hanya untuk bilangan bulat
print 5 kg + 10 m       // ERROR: Dimension mismatch
```

Hasil penjumlahan dan pengurangan mempertahankan satuan operand kiri. Hasil perkalian dan pembagian ditampilkan dengan satuan bawaan yang dimensinya cocok, atau dalam bentuk dimensi dasar seperti `m*s^-1`. Angka tak berdimensi ditampilkan tanpa satuan (`print 5 m / 2 m` → `2.5`).

### 6.2 Penggabungan string

`+` dengan salah satu operand string menghasilkan string:

```phys
print "Speed: " + 20 m/s     // Speed: 20.0 m*s^-1
```

Operator `-`, `*`, `/`, dan `^` tidak berlaku untuk string.

### 6.3 Presedensi dan asosiativitas

Dari ikatan terlemah ke terkuat:

| Tingkat | Operator | Asosiativitas | Keterangan |
| --- | --- | --- | --- |
| 1 | `=` `+=` `-=` `*=` `/=` `in` | `=` kanan, `in` kiri | Assignment dan konversi |
| 2 | `+` `-` | kiri | |
| 3 | `*` `/` | kiri | |
| 4 | `^` | **kiri** | `2 ^ 3 ^ 2` = `(2^3)^2` = 64 |
| 5 | `-` (unary) | prefiks | `-2 ^ 2` = `(-2)^2` = 4 |
| 6 | `f(...)` | postfiks | Pemanggilan fungsi/rumus |

Gunakan tanda kurung bila ragu: `-(2 ^ 2)`, `2 ^ (3 ^ 2)`.

Operator pembanding, `within`, `between`, dan `is` **tidak** termasuk operator ekspresi umum: mereka hanya sah di dalam kondisi `if` (lihat [§12](#12-percabangan-if--else-if--else)).

---

## 7. Variabel

Variabel dideklarasikan sekaligus diberi nilai dengan `=`:

```phys
mass = 2 kg
mass = 3 kg        // assignment ulang
print mass         // 3.0 kg
```

- Assignment pertama mendeklarasikan variabel; assignment berikutnya memperbarui nilainya.
- Assignment majemuk `x += y`, `x -= y`, `x *= y`, `x /= y` berarti `x = x + y`, dan seterusnya, dengan aturan dimensi yang sama. Variabelnya harus sudah ada.

  ```phys
  distance = 2 km
  distance += 500 m      // 2.5 km
  speed = 10 m/s
  speed *= 3             // 30 m/s
  distance += 1 kg       // ERROR: Dimension mismatch
  label = "Status: "
  label += "ready"       // penggabungan string
  ```

- Membaca variabel yang belum dideklarasikan adalah error semantik: `Variable or unit 'x' is not defined!`.
- Ekspresi berupa nama variabel saja di satu baris akan mencetak nilainya (gaya REPL):

  ```phys
  x = 4 m
  x                  // mencetak: 4.0 m
  ```

- Variabel dapat **membayangi** satuan dengan nama sama. Variabel bernama `s` membuat `10 m/s` tidak lagi berarti meter per detik, jadi hindari nama variabel yang sama dengan satuan.

> Kata kunci `var`/`val` dari versi lama sudah tidak dipakai.

---

## 8. Satuan Kustom (`unit`)

```phys
unit kph = 1000 m / 3600 s
unit gravity = 9.8 m / s^2
unit Newton = 1 kg * m / s^2

speed = 90 kph
print speed            // 90.0 kph
print SI(speed)        // 25.0 m*s^-1
```

- Ekspresi di kanan `=` dievaluasi menjadi besaran; dimensi dan skalanya menjadi definisi satuan baru.
- Satuan kustom dapat dipakai seperti satuan bawaan: dalam literal (`90 kph`), dalam ekspresi, dalam `in`, dan dalam `is`.
- Satuan tidak boleh dibuat dari string: `unit x = "abc"` ditolak.

---

## 9. Konversi Satuan (`in`)

`ekspresi in satuan` menampilkan besaran dalam satuan lain dengan dimensi yang sama:

```phys
print 1500 m in km        // 1.5 km
print 2 kWh in MJ         // 7.2 MJ
print 5 m in kg           // ERROR: Cannot convert 'm' to 'kg' (dimensional mismatch)!
```

- Target konversi harus **satu nama satuan** (bawaan atau kustom), bukan ekspresi seperti `m/s`. Buat satuan kustom untuk satuan turunan, misalnya `unit kph = ...`.
- Konversi hanya sah jika dimensi sumber dan target identik.

---

## 10. Rumus (`form`)

`form` mendeklarasikan rumus fisika satu ekspresi:

```phys
form Force(m, a) = m * a
form KineticEnergy(m, v) = 0.5 * m * v^2

print Force(2 kg, 3 m/s^2)          // 6.0 N
print KineticEnergy(2 kg, 10 m/s)   // 100.0 J
```

- Parameter tidak bertipe; **dimensi diperiksa per pemanggilan** berdasarkan argumen yang diberikan.
- Badan rumus adalah satu ekspresi; gunakan if-ekspresi untuk rumus *piecewise* (lihat §12.5).
- Rumus menangkap lingkungan tempat ia dideklarasikan (*closure*), sehingga dapat membaca variabel dan satuan di luar.
- Jumlah argumen harus sama dengan jumlah parameter.
- Nama parameter tidak boleh duplikat; nama rumus tidak boleh bentrok dengan deklarasi lain di lingkup yang sama.
- Rumus rekursif ditolak.

---

## 11. Fungsi Bawaan

| Fungsi | Arity | Keterangan |
| --- | --- | --- |
| `SI(x)` | 1 | Menormalisasi `x` ke skala SI dasar (dimensi tetap). `SI(2 km)` → `2000.0 m`. Hanya menerima besaran. |
| `sqrt(x)` | 1 | Akar kuadrat; pangkat dimensi dibagi dua. `sqrt(16 m^2)` → `4.0 m`. Semua pangkat dimensi wajib **genap**: `sqrt(8 m^3)` dan `sqrt(4 N)` ditolak. Hasilnya besaran biasa yang bisa dipakai lagi dalam ekspresi. |

---

## 12. Percabangan (`if` / `else if` / `else`)

Percabangan di PhysLang **didasarkan pada relasi fisis**, bukan nilai boolean umum. `if` dapat dipakai sebagai **statement** maupun sebagai **ekspresi** (seperti di Kotlin).

### 12.1 Bentuk statement

`if` yang berada di awal statement. `else` opsional.

```phys
if (F > maxLoad) {
    print "Over-stress!"
} else if (F == maxLoad within 50 N) {
    print "At the limit"
} else {
    print "Safe"
}
```

- Kondisi wajib diapit `( )`.
- `else` boleh ditulis di baris yang sama dengan `}` atau di baris berikutnya, termasuk dengan komentar di antaranya.
- Cabang dapat berupa blok `{ ... }` atau satu ekspresi, misalnya `if (c) x = 3 m`. Statement seperti `print` di dalam cabang wajib dibungkus `{ }`.

### 12.2 Bentuk ekspresi

`if` di posisi ekspresi: di kanan `=`, setelah `print`, sebagai argumen, di dalam aritmetika, atau sebagai badan `form`.

```phys
weightClass = if (mass > 1 kg) "heavy" else "light"
print if (mass == 2000 g) "exactly 2 kg" else "not 2 kg"
print 2 * if (h >= 5 m) 1 m else 2 m

energy = if (height > 0 m) {
    g = 9.8 m/s^2
    mass * g * height          // nilai blok = ekspresi terakhir
} else {
    0 J
}
```

Aturan bentuk ekspresi:

1. `else` **wajib**.
2. Nilai cabang blok adalah ekspresi terakhir di dalam blok; blok yang diakhiri statement non-ekspresi (misalnya `print`) ditolak.
3. **Konsistensi dimensi antar-cabang**: semua cabang harus bertipe sama, yaitu besaran dengan dimensi identik (skala boleh berbeda, misalnya `m/s` dan `km/h`), atau sama-sama string. `null` boleh dicampur dengan tipe lain.

   ```phys
   v = if (c > 0 m) 10 m/s else 36 km/h   // OK, dimensi sama
   x = if (c > 0 m) 5 m/s else 3 kg       // ERROR: Dimension mismatch between if branches
   y = if (c > 0 m) "text" else 3 kg      // ERROR: Incompatible if branch types
   ```

4. if-ekspresi bersifat *greedy*: cabang `else` mengambil seluruh ekspresi di kanannya. Gunakan kurung bila perlu: `(if (c) 1 m else 2 m) + 3 m`.

### 12.3 Jenis kondisi

PhysLang **tidak mengenal truthiness**: `if (x)` ditolak. Kondisi harus berupa salah satu relasi berikut.

#### a. Perbandingan berdimensi

```phys
if (distance > 500 m) { ... }
```

- Operator: `<`, `<=`, `>`, `>=`, `==`, `!=`.
- Kedua sisi **wajib berdimensi identik**. Besaran berdimensi juga tidak boleh dibandingkan dengan angka tanpa satuan:

  ```phys
  if (5 m > 2 kg) { }    // ERROR: cannot compare 'm' with 'kg'
  if (v > 0) { }         // ERROR: cannot compare 'm*s^-1' with 'dimensionless'
  if (v > 0 m/s) { }     // OK
  ```

- Perbandingan dilakukan dalam **skala SI**, jadi `1 km > 500 m`, `1000 mm == 1 m`, dan `1 h == 60 min` semuanya benar.
- `==` dan `!=` tanpa `within` memakai toleransi relatif `1e-9` untuk menyerap galat pembulatan *floating point*, sehingga `0.1 m + 0.2 m == 0.3 m` benar.
- Tidak boleh merantai pembanding: `1 m < x < 3 m` ditolak; gunakan `between`.

#### b. Toleransi pengukuran: `within`

```phys
if (measuredG == 9.81 m/s^2 within 0.05 m/s^2) {
    print "Agrees with standard gravity"
}
```

- Benar jika `|a − b| ≤ toleransi`.
- Hanya sah setelah `==` atau `!=`.
- Toleransi wajib berdimensi sama dengan kedua sisi.
- Toleransi negatif menyebabkan runtime error.

#### c. Rentang fisis: `between ... and ...`

```phys
if (T between 273.15 K and 373.15 K) {
    print "Liquid water"
}
```

- Inklusif di kedua batas.
- Ketiga nilai wajib berdimensi sama; skala boleh berbeda (`1 km between 500 m and 2000 m` benar).
- Batas bawah lebih besar dari batas atas menyebabkan runtime error.

#### d. Uji dimensi: `is`

```phys
if (force is N) { print "This is a force" }
if (force is kg*m/s^2) { ... }   // setara dengan N
if (f is 1/s) { ... }            // frekuensi
```

- Benar jika dimensi besaran di kiri sama dengan dimensi **ekspresi satuan** di kanan; nilainya tidak dibandingkan.
- Sisi kanan harus **ekspresi satuan murni**: nama satuan (bawaan atau kustom), `1`, digabung dengan `*`, `/`, dan pangkat bilangan bulat `^`. Variabel, angka selain `1`, dan string ditolak.

### 12.4 Dimensional narrowing

Karena dimensi diketahui sebelum eksekusi, hasil uji `is` sudah dapat ditentukan oleh semantic analyzer. Analyzer **hanya memeriksa cabang yang terpilih**, mirip *smart cast* di Kotlin. Ini memungkinkan satu rumus menangani besaran dengan dimensi berbeda:

```phys
form addOne(q) = if (q is m) q + 1 m else q + 1 kg
print addOne(2 m)      // 3.0 m
print addOne(3 kg)     // 4.0 kg
print addOne(2 s)      // ERROR: cabang terpilih (q + 1 kg) tidak valid untuk detik

form classify(q) = if (q is N) "force" else if (q is J) "energy" else "other"
```

Untuk kondisi selain `is`, **semua cabang** dianalisis, termasuk cabang yang tidak akan dieksekusi. Error dimensi di cabang mana pun tetap dilaporkan sebelum program berjalan.

### 12.5 Rumus piecewise

```phys
form friction(push) = if (push <= maxStaticFriction) push else kineticFriction
form drag(v) = if (v < 1 m/s) linearCoef * v else quadCoef * v^2
```

### 12.6 Lingkup cabang

Setiap cabang blok membuat lingkup baru. Variabel yang dibuat di dalam cabang tidak terlihat dari luar, tetapi assignment ke variabel luar tetap berlaku:

```phys
x = 1 m
if (1 m > 0 m) {
    x = 2 m          // memperbarui x di luar
    local = 5 m      // hanya ada di dalam cabang
}
print x              // 2.0 m
print local          // ERROR: not defined
```

---

## 13. Blok dan Lingkup

```phys
totalDistance = 100 m
{
    offset = 25 m
    print totalDistance + offset    // membaca variabel luar
}
print offset                        // ERROR: offset tidak bocor keluar blok
```

- `{ ... }` membuat lingkup anak (*child scope*) yang dapat membaca variabel dan satuan dari lingkup induk.
- Assignment ke variabel yang sudah ada di lingkup induk memperbarui variabel tersebut.
- Blok boleh bersarang.

---

## 14. Output (`print`)

```phys
print "Range: " + range
print 1500 m in km
print Force(2 kg, 3 m/s^2)
```

Format tampilan besaran:

- Jika besaran memiliki label satuan (dari literal, satuan kustom, atau `in`), satuan itu yang dipakai: `90.0 kph`.
- Jika tidak, interpreter mencari satuan bawaan dengan dimensi yang sama: `6.0 N`.
- Jika tidak ada yang cocok, ditampilkan dalam dimensi dasar: `20.0 m*s^-1`.

---

## 15. Null

```phys
x = null
print x          // null
print x + 1 m    // ERROR: Operator '+' cannot be applied to null operand
```

`null` menandai nilai yang belum ada. Semua operasi aritmetika dan relasi kondisi terhadap `null` ditolak oleh semantic analyzer. Di if-ekspresi, cabang `null` boleh dicampur dengan cabang bertipe lain.

---

## 16. Diagnostik dan Error

### 16.1 Format

```text
ERROR: Dimension mismatch in comparison: cannot compare 'm' with 'kg'.
 --> line 1, column 9
if (5 m > 2 kg) { print "x" }
        ^
```

### 16.2 Tahapan error

| Tahap | Contoh | Akibat |
| --- | --- | --- |
| Sintaks (parser) | `if 5 m > 2 m { }` → `Expected '(' after 'if'` | Program tidak dijalankan |
| Semantik (analyzer) | `5 kg + 10 m` → `Dimension mismatch` | Program tidak dijalankan |
| Runtime (evaluator) | `x == y within -1 m` → `RUNTIME ERROR: Tolerance after 'within' must not be negative` | Eksekusi berhenti |

Semantic analyzer memeriksa seluruh program sebelum eksekusi, termasuk cabang `if` yang tidak akan dijalankan (kecuali cabang yang dieliminasi oleh *dimensional narrowing*).

### 16.3 Ringkasan aturan semantik

| Aturan | Contoh yang ditolak |
| --- | --- |
| `+`/`-` butuh dimensi sama | `5 kg + 10 m` |
| `^` butuh eksponen bulat tak berdimensi | `x ^ 0.5`, `x ^ 2 m` |
| Satuan/variabel harus terdefinisi | `5 parsec`, `print unknownVar` |
| Konversi `in` butuh dimensi sama | `5 m in kg` |
| Satuan tidak dari string | `unit x = "a"` |
| `SI()` dan `sqrt()` hanya untuk besaran | `SI("a")`, `sqrt(null)` |
| `sqrt()` butuh pangkat dimensi genap | `sqrt(8 m^3)`, `sqrt(2 m)` |
| Jumlah argumen sesuai | `Force(2 kg)` untuk `form Force(m, a)` |
| Rumus tidak rekursif | `form f(x) = f(x)` |
| Pembanding butuh dimensi identik | `5 m > 2 kg`, `v > 0` |
| Kondisi harus relasi fisis | `if (x)` |
| `within` dimensi sama, setelah `==`/`!=` | `a < b within 1 m` |
| `between` dimensi sama | `x between 0 m and 2 kg` |
| `is` butuh ekspresi satuan | `q is v`, `q is 5` |
| if-ekspresi wajib `else` | `a = if (c) 1 m` |
| Cabang if-ekspresi bertipe sama | `if (c) 5 m/s else 3 kg` |
| Operand bukan `null` | `null > 1 m` |

---

## 17. Grammar Lengkap (EBNF)

Notasi: `{ x }` = nol atau lebih, `[ x ]` = opsional, `( a | b )` = pilihan, `"..."` = terminal, `(* ... *)` = komentar. Grammar ini mencerminkan **implementasi saat ini**.

### 17.1 Grammar leksikal

```ebnf
(* ---------- Karakter ---------- *)
letter          = ? any Unicode letter, e.g. a-z, A-Z, µ, Ω ? ;
digit           = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;

(* ---------- Token ---------- *)
NUMBER          = digit , { digit } , [ "." , digit , { digit } ] , [ exponent ] ;
exponent        = ( "e" | "E" ) , [ "+" | "-" ] , digit , { digit } ;
INTEGER         = digit , { digit } ;
STRING          = '"' , { ? any character except '"' ? } , '"' ;
IDENTIFIER      = ( letter | "_" ) , { letter | digit | "_" } ;   (* excluding KEYWORD *)
COMMENT         = ( "//" | "#" ) , { ? any character except newline ? } ;
NEWLINE         = "\n" ;                               (* "\r" is ignored *)

KEYWORD         = "form" | "unit" | "print" | "in" | "if" | "else" | "null"
                | "is" | "within" | "between" | "and"
                | "true" | "false" ;                   (* reserved, not yet usable *)

ARITH_OP        = "+" | "-" | "*" | "/" | "^" ;
COMPARE_OP      = "<" | "<=" | ">" | ">=" | "==" | "!=" ;
ASSIGN_OP       = "=" | "+=" | "-=" | "*=" | "/=" ;
DELIMITER       = "(" | ")" | "{" | "}" | "," ;

(* Whitespace other than NEWLINE separates tokens and is otherwise ignored. *)
```

### 17.2 Grammar sintaksis

```ebnf
(* ---------- Program ---------- *)
program         = { separator } , [ statement , { separator , { separator } , statement } ] ,
                  { separator } , EOF ;
separator       = NEWLINE | COMMENT ;
line_break      = NEWLINE | COMMENT ;

(* ---------- Statements ---------- *)
statement       = unit_decl
                | form_decl
                | print_stmt
                | if_stmt
                | block
                | expr_stmt ;

unit_decl       = "unit" , IDENTIFIER , "=" , expression ;

form_decl       = "form" , IDENTIFIER , "(" , [ parameter_list ] , ")" , "=" , expression ;
parameter_list  = IDENTIFIER , { "," , IDENTIFIER } ;

print_stmt      = "print" , expression ;

block           = "{" , { separator } , { statement , { separator } } , "}" ;

expr_stmt       = expression ;

(* if at the start of a statement: else is optional *)
if_stmt         = "if" , "(" , condition , ")" , { line_break } , branch ,
                  [ { line_break } , "else" , { line_break } , ( if_stmt | branch ) ] ;

(* ---------- Expressions (lowest to highest precedence) ---------- *)
expression      = assignment ;

assignment      = IDENTIFIER , ASSIGN_OP , expression             (* right associative *)
                | conversion ;

conversion      = additive , { "in" , IDENTIFIER } ;              (* IDENTIFIER = unit name *)

additive        = multiplicative , { ( "+" | "-" ) , multiplicative } ;

multiplicative  = power , { ( "*" | "/" ) , power } ;

power           = unary , { "^" , unary } ;                        (* left associative *)

unary           = "-" , unary
                | call ;

call            = primary , { "(" , [ argument_list ] , ")" } ;
argument_list   = expression , { "," , expression } ;

primary         = quantity
                | NUMBER
                | STRING
                | "null"
                | IDENTIFIER
                | "(" , expression , ")"
                | if_expr ;

(* A number immediately followed by a unit; an integer power binds to the unit *)
quantity        = NUMBER , IDENTIFIER , [ "^" , [ "-" ] , INTEGER ] ;

(* if in expression position: else is required; the else branch is greedy *)
if_expr         = "if" , "(" , condition , ")" , { line_break } , branch ,
                  { line_break } , "else" , { line_break } , ( if_expr | branch ) ;

branch          = block                     (* value = last expression of the block *)
                | expression ;

(* ---------- Conditions (only inside if) ---------- *)
condition       = comparison
                | range_check
                | dimension_check ;

comparison      = expression , ( "<" | "<=" | ">" | ">=" ) , expression
                | expression , ( "==" | "!=" ) , expression , [ "within" , expression ] ;

range_check     = expression , "between" , expression , "and" , expression ;

dimension_check = expression , "is" , unit_expr ;

(* Syntactically parsed as an expression; restricted by the semantic analyzer *)
unit_expr       = unit_factor , { ( "*" | "/" ) , unit_factor } ;
unit_factor     = unit_atom , [ "^" , [ "-" ] , INTEGER ] ;
unit_atom       = IDENTIFIER                (* a built-in or custom unit, not a variable *)
                | "1" ;
```

### 17.3 Catatan grammar

1. **Pratt parser.** Implementasi memakai Pratt parser (`nud`/`led`) dengan tabel presedensi di `Precedence.kt`. Aturan bertingkat di atas adalah bentuk EBNF yang ekuivalen untuk program valid.
2. **Pembanding bukan operator ekspresi.** `COMPARE_OP`, `within`, `between`, dan `is` memiliki presedensi 0 sehingga parsing ekspresi berhenti di depannya. Mereka hanya dikenali oleh `condition`.
3. **Statement vs ekspresi `if`.** `if_stmt` dipilih bila `if` adalah token pertama statement; `if_expr` dipakai di posisi ekspresi lain. Keduanya menghasilkan node AST yang sama (`IfExpr`) dengan flag `isExpression`.
4. **Satu statement per baris.** Setelah `expr_stmt`, `form_decl`, dan `if_stmt` hanya boleh ada `NEWLINE`, `COMMENT`, akhir file, atau `}` penutup blok.
5. **Batasan semantik di luar grammar.** Aturan dimensi (§6, §12, §16.3), keharusan target `in` berupa satuan, dan `unit_expr` pada `is` diperiksa oleh semantic analyzer, bukan parser.
6. **Assignment majemuk.** `x op= e` di-*desugar* oleh parser menjadi `x = x op e`, sehingga seluruh aturan dimensi operator `op` berlaku. `INTEGER` pada pangkat satuan (`m^2`) tidak boleh berbentuk desimal atau notasi ilmiah.

---

## 18. Arsitektur Interpreter

```text
Source (.phys)
   │
   ▼
Lexer              lang/lexer/Lexer.kt              hand-written scanner → List<Token>
   │
   ▼
Parser             lang/parser/Parser.kt            Pratt parser (nud/led) → List<Stmt>
   │
   ▼
SemanticAnalyzer   lang/semantic/SemanticAnalyzer.kt  static dimension and type checks
   │                                                  (Diagnostic via ErrorReporter)
   ▼
PhysicsEvaluator   lang/ast/visitors/PhysicsEvaluator.kt  tree-walking visitor
   │
   ▼
Output
```

| Komponen | Lokasi | Peran |
| --- | --- | --- |
| Token dan keyword | `lang/lexer/TokenType.kt`, `lang/core/Keywords.kt` | Daftar token dan pemetaan keyword |
| AST ekspresi | `lang/ast/Expr.kt` | `BinaryExpr`, `UnaryExpr`, `QuantityExpr`, `IdentifierExpr`, `AssignExpr`, `CallExpr`, `ConversionExpr`, `IfExpr` |
| AST kondisi | `lang/ast/Condition.kt` | `Comparison`, `Between`, `DimensionCheck` |
| AST statement | `lang/ast/Stmt.kt` | `ExprStmt`, `PrintStmt`, `UnitDeclStmt`, `BlockStmt`, `FormDeclStmt` |
| Nilai runtime | `lang/ast/RuntimeValue.kt` | `PhysicsValue`, `StringValue`, `NullValue`, `NativeFunction`, `Formula` |
| Lingkungan | `lang/core/Environment.kt` | Variabel, satuan kustom, fungsi bawaan, lingkup bersarang |
| Fungsi bawaan | `lang/core/Function.kt` | `SI`, `sqrt` (implementasi `Callable`) |
| Diagnostik | `lang/error` | `Diagnostic`, `ErrorReporter` |

---

## 19. Batasan yang Diketahui

Bagian ini mencatat perilaku implementasi saat ini secara jujur:

| Batasan | Keterangan |
| --- | --- |
| Boolean | `true`/`false` dicadangkan tetapi belum menjadi nilai. Kondisi hanya berupa relasi fisis, tanpa `&&`/`||`/`!`. |
| String | Tanpa escape sequence. |
| Bilangan bulat besar | Literal bulat tanpa titik/eksponen di atas 2.147.483.647 tidak didukung; tulis sebagai desimal atau notasi ilmiah (`1e10`). |
| Target `in` | Hanya satu nama satuan, bukan ekspresi satuan. |
| Bayangan satuan | Variabel dengan nama yang sama dengan satuan menutupi satuan tersebut. |
| Redeklarasi `form` di REPL/runner per-statement | Runner per-statement (`./run.sh file.phys` dan REPL) mengizinkan `form` dengan nama sama dideklarasikan ulang (menimpa), sedangkan `PhysRunner` menolaknya. |
| Loop | Belum ada konstruksi perulangan. |

---

## 20. Contoh Program Lengkap

### 20.1 Gerak proyektil dan gesekan

```phys
// A 2 kg ball is launched at 20 m/s with sin(theta)=0.6 and cos(theta)=0.8.
unit gravity = 10 m / s^2
unit launchSpeed = 20 m / s

mass = 2 kg
frictionCoefficient = 0.2

flightTime = (2 * launchSpeed * 0.6) / gravity
range = launchSpeed * 0.8 * flightTime
kineticEnergy = 0.5 * mass * launchSpeed^2
frictionForce = frictionCoefficient * mass * gravity
slidingDistance = kineticEnergy / frictionForce

print "Range: " + range
print "Total distance: " + (range + slidingDistance)
```

### 20.2 Rezim gesekan statis vs kinetis

```phys
unit gravity = 10 m / s^2

mass = 50 kg
normalForce = mass * gravity
maxStaticFriction = 0.5 * normalForce
kineticFriction = 0.3 * normalForce

form friction(push) = if (push <= maxStaticFriction) push else kineticFriction
form acceleration(push) = (push - friction(push)) / mass

print "Acceleration for 400 N push: " + acceleration(400 N)    // 5.0 m*s^-2

if (acceleration(200 N) == 0 m/s^2) {
    print "The crate stays at rest under a 200 N push"
}
```

### 20.3 Pemeriksaan beban struktur

```phys
unit yieldStrength = 250 MPa
area = 2 cm^2

form stress(load) = load / area

form safetyState(load) = if (stress(load) <= 0.6 * yieldStrength) {
    "SAFE"
} else if (stress(load) <= 0.9 * yieldStrength) {
    "WARNING"
} else if (stress(load) == yieldStrength within 1 MPa) {
    "CRITICAL: at yield point"
} else if (stress(load) < yieldStrength) {
    "CRITICAL"
} else {
    "FAILED: plastic deformation"
}

print "Stress at 20 kN: " + (stress(20 kN) in MPa)    // 100.0 MPa
print "50 kN -> " + safetyState(50 kN)                // CRITICAL: at yield point
```

### 20.4 Klasifikasi besaran berdasarkan dimensi

```phys
unit kph = 1000 m / 3600 s

form classify(q) = if (q is N) "force"
    else if (q is J) "energy"
    else if (q is m/s) "velocity"
    else "unclassified"

print classify(3 kN)                // force
print classify(2 kWh)               // energy
print classify(90 kph)              // velocity
print classify(12 kg * m / s^2)     // force

form display(q) = if (q is m/s) q in kph else q in km
print display(25 m/s)               // 90.0 kph
print display(1500 m)               // 1.5 km
```

### 20.5 Indeks file contoh

| File | Topik |
| --- | --- |
| `assets/fitur/01`–`14` | Literal, aritmetika, cek dimensi, variabel, satuan kustom, SI, pangkat, string, fungsi, komentar, blok, null |
| `assets/fitur/15_if_else.phys` | if statement, else-if, enam operator pembanding |
| `assets/fitur/16_if_expression.phys` | if sebagai ekspresi, cabang blok, rumus piecewise |
| `assets/fitur/17_physics_conditions.phys` | `within`, `between`, `is`, dimensional narrowing |
| `assets/fitur/18_compound_and_notation.phys` | Assignment majemuk, notasi ilmiah, `sqrt`, underscore, komentar `#` |
| `assets/01`–`12` | Skenario fisika: kecepatan, kendaraan, hidrolik, orbit, jembatan, pipa, dll. |
| `assets/13_friction_regime.phys` | Gesekan statis/kinetis |
| `assets/14_structural_load_check.phys` | Keamanan struktur |
| `assets/15_water_phase_state.phys` | Fase air berdasarkan suhu |
| `assets/16_quantity_classifier.phys` | Klasifikasi besaran dengan `is` |
| `assets/*error*.phys` | Contoh error yang disengaja |
