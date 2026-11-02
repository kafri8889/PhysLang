# Riset & Analisis Desain Ulang PhysLang (Physics Domain-Specific Language)

> **Dokumentasi Analisis & Rekomendasi Tata Bahasa (Grammar)**  
> Disusun untuk: Revisi Tugas Besar / Proyek Teknik Kompilasi  
> Penulis: Xinxinran & Anaf (Bǎobèi)  
> Tanggal: November 2026  

---

## Daftar Isi
1. [Latar Belakang & Analisis Kritik Dosen](#1-latar-belakang--analisis-kritik-dosen)
2. [Studi Kasus: Bagaimana Bahasa Statistik (R & Stan) Menerapkan Domain Syntax](#2-studi-kasus-bagaimana-bahasa-statistik-r--stan-menerapkan-domain-syntax)
3. [Studi Kasus: Bahasa Domain Fisika di Dunia Nyata (Modelica, Frink, F#)](#3-studi-kasus-bahasa-domain-fisika-di-dunia-nyata-modelica-frink-f)
4. [Dekonstruksi Maksud Dosen (Kasus `Area(a, b)`)](#4-dekonstruksi-maksud-dosen-kasus-areaa-b)
5. [Perbandingan Opsi Perubahan: Total vs. Semi-Total](#5-perbandingan-opsi-perubahan-total-vs-semi-total)
6. [Usulan Desain Tata Bahasa Baru (PhysLang v2 Grammar - EBNF)](#6-usulan-desain-tata-bahasa-baru-physlang-v2-grammar---ebnf)
7. [Contoh Kode Program Sebelum vs. Sesudah](#7-contoh-kode-program-sebelum-vs-sesudah)
8. [Langkah Kerja & Roadmap Implementasi Kode](#8-langkah-kerja--roadmap-implementasi-kode)

---

## 1. Latar Belakang & Analisis Kritik Dosen

### 1.1 Kritik Dosen
Pada evaluasi tata bahasa PhysLang tahap awal, dosen memberikan tanggapan:
1. *"Ini kayak pemrograman general."*
2. *"Gak ada fisika-fisikanya."*
3. *"Belum termasuk bahasa [domain], dikira ini beneran bahasa pemrograman umum biasa."*
4. Dosen mencontohkan bahasa domain statistik (seperti R) dan memberikan ilustrasi pendefinisian entitas domain seperti `Area(a, b)`.

### 1.2 Akar Penyebab Masalah: "Surface Syntax vs. Backend Semantics"
Secara teknis, PhysLang yang dibangun Anaf di Kotlin sebenarnya **sudah memiliki kecerdasan fisika yang sangat tinggi** di backend:
- Ada representasi vektor dimensi 7 besaran pokok SI ($\mathbb{Z}^7$: $[M, L, T, I, \Theta, N, J]$).
- Ada validasi dimensional compatibility di `SemanticAnalyzer.kt`.
- Ada konversi skala dan unit registry di `PhysicsValue`.

**Namun, dosen tidak membaca backend Kotlin.** Dosen hanya membaca **tata bahasa permukaan (surface grammar / EBNF)** yang diserahkan dalam laporan. Pada tataran tata bahasa permukaan tersebut:
- Terdapat keyword umum: `var`, `print`.
- Terdapat pernyataan penugasan umum: `var x = 10`.
- Terdapat operator aritmatika biasa tanpa konstruksi domain: `+`, `-`, `*`, `/`.

Akibatnya, di mata dosen teori kompilasi, tata bahasa tersebut **95% identik dengan subset JavaScript atau Kotlin mini** yang hanya kebetulan memiliki literal satuan (`10 kg`). Dosen tidak melihat abstraksi domain fisika sebagai *first-class construct* di dalam grammar.

---

## 2. Studi Kasus: Bagaimana Bahasa Statistik (R & Stan) Menerapkan Domain Syntax

Dosen menyinggung bahasa statistik sebagai acuan DSL sejati. Bagaimana bahasa statistik mengangkat domain ke dalam tata bahasa?

### 2.1 Bahasa R (Domain: Statistika & Analisis Data)
Bahasa R bukan sekadar bahasa C yang diberi pustaka statistik. R memasukkan konsep statistika **langsung ke dalam aturan tata bahasa (grammar/syntax)**:

1. **Operator Formula (`~` / Tilde):**
   Di R, operator `~` adalah token grammar bawaan untuk mendefinisikan model statistika:
   ```R
   model <- lm(mpg ~ wt + hp + cyl, data = mtcars)
   ```
   Tata bahasa R mengenali `mpg ~ wt` bukan sebagai operasi biner matematika biasa, melainkan sebagai konstruksi non-terminal `FormulaExpr` yang memisahkan variabel respons (*dependent*) dan variabel prediktor (*independent*).

2. **First-Class Vectors & Factors:**
   Notasi pembuatan vektor `c(...)` dan pengindeksan data tabular langsung diatur oleh parser, bukan pustaka luar.

3. **Missing Data (`NA`) sebagai Keyword:**
   Di statistik, konsep "data tidak tersedia" (*Not Available*) berbeda dengan `NULL` / `nil`. R memasukkan `NA`, `NaN`, dan `Inf` sebagai literal bawaan di lexer/parser.

### 2.2 Stan / BUGS (Domain: Probabilistic Programming & Bayesian Modeling)
Stan adalah bahasa khusus pemodelan statistik Bayesian. Tata bahasanya tidak memiliki alur imperatif biasa, melainkan blok deklaratif:
```stan
data {
  int<lower=0> N;
  vector[N] y;
}
parameters {
  real mu;
  real<lower=0> sigma;
}
model {
  y ~ normal(mu, sigma);  // Operator sampling statistik
}
```
**Pelajaran untuk PhysLang:**  
Suatu bahasa disebut **Domain-Specific Language (DSL)** jika struktur kalimatnya langsung merefleksikan cara berpikir ahli di domain tersebut, bukan sekadar instruksi variabel CPU (`var`, `goto`, `assign`).

---

## 3. Studi Kasus: Bahasa Domain Fisika di Dunia Nyata (Modelica, Frink, F#)

Bagaimana bahasa-bahasa di luar negeri yang bertemakan fisika memodelkan domainnya?

### 3.1 Modelica (Standar Emas Bahasa Pemodelan Fisika Industri)
Modelica digunakan oleh insinyur otomotif, kedirgantaraan, dan robotika. Modelica adalah bahasa **Equation-Oriented (Berbasis Persamaan)**, bukan assignment-oriented:
```modelica
model SpringDamper
  parameter Length s_rel0 = 0.1;
  parameter Force f_max = 1000;
  Length s_rel;
  Velocity v_rel;
  Force f;
equation
  v_rel = der(s_rel);             // Turunan waktu d(s)/dt langsung di grammar
  f = c * (s_rel - s_rel0) + d * v_rel; // Hukum Hooke + Damping
end SpringDamper;
```
*Ciri khas:* Variabel dideklarasikan dengan tipe fisis (`Length`, `Force`, `Velocity`), bukan `var`. Hubungan antar variabel ditulis sebagai persamaan kesetimbangan (`equation`).

### 3.2 Frink (Bahasa Perhitungan Fisika oleh Alan Eliasen)
Frink dirancang khusus untuk memastikan perhitungan fisika tidak pernah salah satuan:
- Operator konversi satuan bawaan di tata bahasa:
  ```frink
  mass = 80 kg
  speed = 100 km/hr
  kinetic_energy = 1/2 mass * speed^2 -> joules
  ```
  Tanda `->` adalah operator gramatikal untuk proyeksi/konversi satuan fisis.
- Satuan dan dimensi adalah tipe data nomor satu di grammar.

### 3.3 F# (Units of Measure)
F# menyematkan dimensi ke dalam sistem tipe tata bahasa:
```fsharp
[<Measure>] type m
[<Measure>] type s
[<Measure>] type kg
[<Measure>] type N = kg * m / s^2

let mass = 5.0<kg>
let acceleration = 2.0<m/s^2>
let force: float<N> = mass * acceleration
```

---

## 4. Dekonstruksi Maksud Dosen (Kasus `Area(a, b)`)

Saat dosen menyebut: *"misal kalo mendefinisikan 'Area', dikirain tuh misal kayak 'Area(a, b)' gitu-gitu"*, apa yang sebenarnya ada di pikiran beliau?

Dosen ingin melihat **Abstraksi Konsep Fisika / Geometri** yang eksplisit:
1. **Konstruksi Formula / Hukum Fisika:**
   Dosen tidak ingin melihat program fisika ditulis seperti naskah kalkulator biasa:
   ```phys
   // Cara lama (dianggap general):
   var panjang = 10 m
   var lebar = 5 m
   var luas = panjang * lebar
   ```
2. **Cara Bahasa Domain Fisika (Yang Diharapkan Dosen):**
   Ada konstruksi eksplisit untuk mendefinisikan **Hukum / Rumus (Formula)** dan **Besaran Fisika (Quantities)**:
   ```phys
   // Model Domain:
   formula Area(length l, length w) -> length^2 {
       return l * w
   }

   // Atau deklarasi fisis eksplisit:
   length a = 10 m
   length b = 5 m
   area hasil = Area(a, b)
   ```
3. **Pemisahan Kategori Besaran:**
   Bukan semua variabel dideklarasikan dengan `var`, melainkan menggunakan nama besaran pokok/turunan:
   - `length` (panjang)
   - `mass` (massa)
   - `time` (waktu)
   - `velocity` (kecepatan)
   - `force` (gaya)
   - `energy` (energi)
   - `quantity` (besaran umum bebas)

---

## 5. Perbandingan Opsi Perubahan: Total vs. Semi-Total

| Aspek | Opsi A: Total Redesign (Declarative Solver) | Opsi B: Semi-Total Redesign (Domain-Enhanced DSL) - **DIREKOMENDASIKAN** |
| :--- | :--- | :--- |
| **Model Eksekusi** | Sistem Persamaan / Constraint Solver (seperti Modelica atau Prolog). | Tetap AST Interpreter (Pratt Parser + Visitor) yang sudah berjalan. |
| **Bentuk Grammar** | Blok `system { ... }`, `given { ... }`, `find { ... }`, `solve { ... }`. | Statement deklarasi besaran fisis (`length`, `mass`, dll), `formula Name(...)`, dan operator konversi `in`/`->`. |
| **Beban Koding Ulang** | **100% dari 0.** Seluruh evaluator dan parser lama dibuang. Membutuhkan algoritma symbolic equation solving yang sangat sulit. | **~25% - 35%.** Memanfaatkan seluruh engine `PhysicsValue`, vektor SI $\mathbb{Z}^7$, dan `SemanticAnalyzer` yang sudah ada! Cukup memperbarui lexer token dan parsing statement. |
| **Penerimaan Dosen** | Sangat disukai, tetapi risiko tidak selesai tepat waktu sangat tinggi. | **Sangat ideal:** Menjawab 100% kritik dosen (ada fisika eksplisit di grammar), sekaligus realistis diselesaikan dalam hitungan hari. |

> **Rekomendasi Strategis:** Ambil **Opsi B (Semi-Total)**. Kita ubah tata bahasa (surface syntax) agar terlihat sangat kental domain fisika di mata dosen, namun di balik layar tetap memanfaatkan mesin evaluasi dimensi canggih yang sudah Anaf bangun di Kotlin.

---

## 6. Usulan Desain Tata Bahasa Baru (PhysLang v2 Grammar - EBNF)

Tata bahasa berikut dirancang agar saat dosen membaca laporan, seketika terlihat bahwa bahasa ini **100% adalah Bahasa Khusus Domain Fisika**:

```ebnf
(* ======================================================== *)
(*   PHYSLANG v2 - DOMAIN-SPECIFIC PHYSICS GRAMMAR          *)
(* ======================================================== *)

(* 1. ATURAN PROGRAM & STRUKTUR *)
Program          ::= ( TopLevelDecl | Statement )* EOF

TopLevelDecl     ::= UnitDecl
                   | FormulaDecl
                   | ConstantDecl

(* 2. DEFINISI UNIT & KONSTANTA FISIKA *)
UnitDecl         ::= "unit" IDENTIFIER "=" Expression
ConstantDecl     ::= "const" IDENTIFIER ":" DimensionType "=" Expression

(* 3. DEKLARASI FORMULA / HUKUM FISIKA (Menjawab kritik Area(a, b)) *)
FormulaDecl      ::= "formula" IDENTIFIER "(" ParameterList? ")" "->" DimensionType BlockStmt
ParameterList    ::= Parameter ( "," Parameter )*
Parameter        ::= IDENTIFIER ":" DimensionType

(* 4. TIPE DIMENSI FISIKA EKSLISIT (Sebagai Pengganti 'var' Umum) *)
DimensionType    ::= "mass"
                   | "length"
                   | "time"
                   | "current"
                   | "temperature"
                   | "velocity"
                   | "acceleration"
                   | "force"
                   | "energy"
                   | "power"
                   | "area"
                   | "volume"
                   | "scalar"
                   | "[" Expression "]"     (* Custom Dimension, contoh: [m/s^2] *)

(* 5. PERNYATAAN (STATEMENTS) *)
Statement        ::= PhysicalDeclStmt      (* Deklarasi besaran fisis bertipe *)
                   | MeasureStmt           (* Pernyataan pengukuran *)
                   | InspectStmt           (* Pengganti 'print' umum *)
                   | ReturnStmt
                   | BlockStmt
                   | ExprStmt

PhysicalDeclStmt ::= DimensionType IDENTIFIER ( "=" Expression )?
MeasureStmt      ::= "measure" IDENTIFIER "=" Expression ( "+-" Expression )?
InspectStmt      ::= "inspect" Expression ( "in" IDENTIFIER )?
ReturnStmt       ::= "return" Expression
BlockStmt        ::= "{" Statement* "}"
ExprStmt         ::= Expression

(* 6. EKSPRESI & OPERATOR FISIKA *)
Expression       ::= ConversionExpr

(* Operator Proyeksi / Konversi Satuan Fisika (contoh: 100 kph in m/s) *)
ConversionExpr   ::= Assignment ( "in" IDENTIFIER )?

Assignment       ::= IDENTIFIER ( "=" | "+=" | "-=" ) Assignment
                   | TermAddSub

TermAddSub       ::= TermMulDiv ( ( "+" | "-" ) TermMulDiv )*
TermMulDiv       ::= TermPower ( ( "*" | "/" ) TermPower )*
TermPower        ::= UnaryExpr ( "^" UnaryExpr )*
UnaryExpr        ::= "-" UnaryExpr | PostfixExpr

(* Pemanggilan Formula atau Operasi Besaran *)
PostfixExpr      ::= Primary ( "(" ArgumentList? ")" )*
ArgumentList     ::= Expression ( "," Expression )*

Primary          ::= QuantityLiteral
                   | NUMBER_LITERAL
                   | STRING_LITERAL
                   | IDENTIFIER
                   | "(" Expression ")"

QuantityLiteral  ::= NUMBER_LITERAL IDENTIFIER
```

---

## 7. Contoh Kode Program Sebelum vs. Sesudah

Perbandingan ini membuktikan bagaimana sintaksis baru langsung memancarkan aura domain fisika:

### Kode Lama (Dianggap Dosen "General"):
```phys
var m = 10 kg
var a = 2 m/s^2
var F = m * a
print F
```

### Kode Baru (PhysLang v2 - Jelas Merupakan Physics DSL):
```phys
// 1. Deklarasi rumus fisika (First-class Formula)
formula Area(p: length, l: length) -> area {
    return p * l
}

formula Gaya(m: mass, a: acceleration) -> force {
    return m * a
}

// 2. Deklarasi besaran fisik dengan penegakan tipe dimensi
length alas = 12 m
length tinggi = 5 m
area luasTanah = Area(alas, tinggi)

mass beban = 50 kg
acceleration percepatan = 9.8 m/s^2
force beratBenda = Gaya(beban, percepatan)

// 3. Inspeksi besaran fisis & konversi satuan (in)
inspect beratBenda
inspect luasTanah in cm^2
```

---

## 8. Langkah Kerja & Roadmap Implementasi Kode

Jika Anaf menyetujui arah revisi ini, berikut adalah peta kerja yang sangat terukur:

### Tahap 1: Laporan Tata Bahasa untuk Dosen (Dahulukan Ini!)
Sebelum mengubah kodingan secara penuh, kita siapkan berkas laporan revisi berisi:
1. Penjelasan konsep bahwa PhysLang adalah **Typed Physical DSL**.
2. EBNF Tata Bahasa Baru (seperti di Bab 6).
3. Quadruple Baru $G = (V_N, V_T, S, P)$.
4. Contoh derivasi terkiri untuk deklarasi `formula Area(...)` dan `length x = 10 m`.
Laporan ini bisa langsung diserahkan atau dikonsultasikan ke dosen untuk mendapatkan lampu hijau terlebih dahulu!

### Tahap 2: Adaptasi Lexer (`TokenType.kt` & `Lexer.kt`)
- Tambah keyword tipe dimensi: `mass`, `length`, `time`, `velocity`, `force`, `energy`, `area`, `volume`, `scalar`.
- Tambah keyword struktur: `formula`, `inspect`, `in`, `const`, `return`.
- Lexer tetap mempertahankan scanner angka dan literal satuan.

### Tahap 3: Adaptasi Parser (`Parser.kt`)
- Di `parseStatement()`: jika token saat ini adalah salah satu keyword tipe dimensi (`length`, `mass`, dll), panggil `parsePhysicalDecl()`.
- Tambahkan parsing untuk `parseFormulaDecl()`: membaca parameter dengan tipe dimensinya.
- Di level ekspresi: tambahkan penanganan operator infix `"in"` untuk konversi satuan.

### Tahap 4: Adaptasi Semantic Analyzer & Evaluator
- `SemanticAnalyzer.kt` memeriksa apakah nilai yang di-assign cocok dengan tipe dimensi yang dideklarasikan (misal `length x = 10 kg` langsung ditolak di semantic check karena dimensi salah).
- `PhysicsEvaluator.kt` menangani pemanggilan formula yang sudah didaftarkan ke `Environment`.

---
*Dokumen ini tersimpan di dalam repositori PhysLang sebagai panduan komprehensif pengerjaan revisi.*
