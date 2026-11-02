# Dokumentasi Desain Ulang PhysLang v2 (Physicist-Centric DSL)

> **Spesifikasi Bahasa, Tata Bahasa (Grammar EBNF), dan Rencana Perubahan Arsitektur**  
> Dibuat untuk: Tugas Besar Teknik Kompilasi  
> Penulis: Anaf (Bǎobèi) & Xinxinran  
> Tanggal: November 2026  
> Lokasi Proyek: `/run/media/baobaojingyi/Drive D/DOCUMENTS_V3/Java_Kotlin_Project/PhysLang`  

---

## 1. Visi & Filosofi Desain (POV Fisikawan)

PhysLang v2 bertransformasi dari sekadar bahasa prosedural mirip JavaScript/Kotlin menjadi **Domain-Specific Language (DSL) murni untuk komputasi analitik fisika (Lab Notebook Style)**.

### Prinsip Utama:
1. **Zero-Boilerplate Variables (`id = expr`):**  
   Fisikawan tidak menulis `var x = 10` atau `let x = 10`. Mereka langsung menulis simbol besaran di papan tulis atau lembar riset: `m = 10 kg`, `v = 20 m/s`.
2. **First-Class Physics Formula (`form Name(args) = expr`):**  
   Rumus fisika didefinisikan secara matematis satu baris tanpa fungsi kaku atau kata kunci `return`. Kata kunci `form` (atau `formula`) secara eksplisit membedakan definisi rumus dari pemanggilan fungsi biasa, menghilangkan ambiguitas grammar.
3. **First-Class Unit Manipulation (`unit` & `in`):**  
   - Fitur unggulan `unit` tetap dipertahankan: pengguna bebas membuat satuan baru dari ekspresi fisis (`unit kph = 1000 m / 3600 s`).
   - Operator proyeksi/konversi `in` memungkinkan membaca besaran dalam satuan yang diinginkan (`inspect Ek in kJ`).
4. **Physical Boundary Conditions (`if-else`):**  
   Percabangan digunakan untuk kondisi batas fisika (misal: gaya gesek statis vs kinetis, batas kecepatan relativistik, atau ambang suhu kritis). Operator perbandingan (`<`, `>`, `==`) **wajib divalidasi memiliki dimensi yang sama** oleh compiler.
5. **Observasi Bersih (`inspect`):**  
   Menggantikan `print` umum menjadi `inspect` yang merefleksikan proses observasi eksperimen laboratorium.

---

## 2. Tata Bahasa Formal PhysLang v2 (EBNF Grammar)

### A. Aturan Leksikal (Tokens)
```ebnf
(* Literals & Identifiers *)
DIGIT            ::= [0-9]
INT_LITERAL      ::= DIGIT+
DOUBLE_LITERAL   ::= DIGIT+ "." DIGIT+
NUMBER_LITERAL   ::= INT_LITERAL | DOUBLE_LITERAL
STRING_LITERAL   ::= '"' [^"]* '"'
IDENTIFIER       ::= [a-zA-Z_][a-zA-Z0-9_]*

(* Keywords Khusus Domain *)
KEYWORD          ::= "unit" | "form" | "inspect" | "in" | "if" | "else" | "null"

(* Operators & Delimiters *)
ASSIGN_OP        ::= "=" | "+=" | "-=" | "*=" | "/="
COMPARE_OP       ::= "==" | "!=" | "<" | "<=" | ">" | ">="
ARITH_OP         ::= "+" | "-" | "*" | "/" | "^"
DELIMITER        ::= "(" | ")" | "{" | "}" | "," | "\n"
```

### B. Aturan Sintaksis (Syntactic Rules)
```ebnf
Program          ::= ( Statement | "\n" )* EOF

Statement        ::= UnitDeclStmt
                   | FormulaDeclStmt
                   | IfStmt
                   | InspectStmt
                   | BlockStmt
                   | VarAssignStmt
                   | ExprStmt

(* 1. Deklarasi Satuan Baru *)
UnitDeclStmt     ::= "unit" IDENTIFIER "=" Expression

(* 2. Definisi Rumus Fisika (Bebas Ambiguitas) *)
FormulaDeclStmt  ::= "form" IDENTIFIER "(" ParameterList? ")" "=" Expression
ParameterList    ::= IDENTIFIER ( "," IDENTIFIER )*

(* 3. Kondisi Batas Fisika *)
IfStmt           ::= "if" "(" ConditionExpr ")" BlockStmt ( "else" BlockStmt )?
ConditionExpr    ::= Expression COMPARE_OP Expression

(* 4. Observasi / Cetak Hasil *)
InspectStmt      ::= "inspect" Expression ( "in" IDENTIFIER )?

(* 5. Blok Lingkup *)
BlockStmt        ::= "{" ( Statement | "\n" )* "}"

(* 6. Deklarasi & Penugasan Variabel Lab (id = expr) *)
VarAssignStmt    ::= IDENTIFIER ASSIGN_OP Expression

(* 7. Ekspresi Umum *)
ExprStmt         ::= Expression

(* Hierarki Presedensi Ekspresi *)
Expression       ::= ConversionExpr

ConversionExpr   ::= TermAddSub ( "in" IDENTIFIER )?

TermAddSub       ::= TermMulDiv ( ( "+" | "-" ) TermMulDiv )*

TermMulDiv       ::= TermPower ( ( "*" | "/" ) TermPower )*

TermPower        ::= UnaryExpr ( "^" UnaryExpr )*

UnaryExpr        ::= "-" UnaryExpr
                   | PostfixExpr

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

## 3. Quadruple Formal Tata Bahasa $G = (V_N, V_T, S, P)$

1. **Himpunan Non-Terminal ($V_N$):**
   ```text
   V_N = {
       Program, Statement, UnitDeclStmt, FormulaDeclStmt, ParameterList,
       IfStmt, ConditionExpr, InspectStmt, BlockStmt, VarAssignStmt,
       ExprStmt, Expression, ConversionExpr, TermAddSub, TermMulDiv,
       TermPower, UnaryExpr, PostfixExpr, ArgumentList, Primary, QuantityLiteral
   }
   ```

2. **Himpunan Terminal ($V_T$):**
   ```text
   V_T = {
       NUMBER_LITERAL, STRING_LITERAL, IDENTIFIER,
       "unit", "form", "inspect", "in", "if", "else", "null",
       "=", "+=", "-=", "*=", "/=",
       "==", "!=", "<", "<=", ">", ">=",
       "+", "-", "*", "/", "^",
       "(", ")", "{", "}", ",", "\n"
   }
   ```

3. **Simbol Awal ($S$):**
   ```text
   S = Program
   ```

4. **Himpunan Aturan Produksi ($P$):**
   Mengikuti seluruh aturan produksi pada bagian 2 (Aturan Sintaksis) di atas.

---

## 4. Contoh Program Nyata PhysLang v2

```phys
# 1. Menentukan unit pengukuran eksperimen
unit kph = 1000 m / 3600 s
unit Newton = 1 kg * m / s^2

# 2. Merumuskan hukum fisika dasar
form Area(p, l) = p * l
form Gaya(m, a) = m * a
form EnergiKinetik(m, v) = 0.5 * m * v^2

# 3. Input data laboratorium (simpel: id = expr)
massaBenda = 1500 kg
kecepatan = 108 kph
percepatan = 3.5 m/s^2

# 4. Evaluasi perhitungan
F = Gaya(massaBenda, percepatan)
Ek = EnergiKinetik(massaBenda, kecepatan)

# 5. Menampilkan hasil dan konversi unit
inspect F in Newton
inspect Ek in kJ

# 6. Analisis kondisi batas (boundary check)
batasGaya = 4000 Newton
if (F > batasGaya) {
    inspect "Peringatan: Struktur mengalami over-stress!"
} else {
    inspect "Status beban: Aman dalam toleransi normal."
}
```

---

## 5. Rencana Perubahan Teknis pada Kode Kotlin

| File / Modul | Perubahan yang Dilakukan | Status / Catatan |
| :--- | :--- | :--- |
| **`lang.lexer.TokenType`** | Tambah token: `Form`, `Inspect`, `In`, `If`, `Else`, `Less`, `Greater`, `LessEquals`, `GreaterEquals`, `NotEquals`. Hapus/deprecate `Var`. | Token scanner baru |
| **`lang.lexer.Keywords`** | Pendaftaran keyword baru: `"form"`, `"inspect"`, `"in"`, `"if"`, `"else"`. | Update map |
| **`lang.ast.Stmt`** | Tambah data class: `FormulaDeclStmt(name, params, bodyExpr)`, `IfStmt(condition, thenBranch, elseBranch)`, `InspectStmt(expr, targetUnit)`. `VarDeclStmt` disederhanakan/diintegrasikan dengan penugasan `id = expr`. | AST nodes baru |
| **`lang.parser.Parser`** | 1. Tambah method `parseFormulaDecl()`: membaca `form id(args) = expr`.<br>2. Tambah `parseIfStmt()`: membaca `if (cond) { ... } else { ... }`.<br>3. Di `parseStatement()`: jika token adalah `Identifier` diikuti `=`, jadikan assignment statement.<br>4. Tambah `parseInspect()`: membaca opsi konversi `in <unit>`. | Modifikasi Pratt Parser |
| **`lang.semantic.SemanticAnalyzer`** | 1. Registrasi fungsi formula ke tabel simbol.<br>2. Verifikasi tipe pembanding pada `ConditionExpr` (kiri dan kanan wajib memiliki dimensi yang sama!).<br>3. Verifikasi target konversi `in <unit>` harus berdimensi kompatibel dengan ekspresinya. | Guardian hukum fisika |
| **`lang.ast.visitors.PhysicsEvaluator`** | 1. Evaluasi `FormulaDeclStmt`: simpan deklarasi formula ke `Environment`.<br>2. Evaluasi `CallExpr`: binding argumen aktual ke parameter formula, evaluasi ekspresi rumus, kembalikan `PhysicsValue`.<br>3. Evaluasi `IfStmt`: cek boolean condition, eksekusi cabang yang sesuai.<br>4. Evaluasi konversi `in <unit>`. | Runtime interpreter |

---

## 6. Backlog Fitur Masa Depan: Introspection / Environment Reflection API

Fitur refleksi runtime (terinspirasi dari objek `document` pada Web DOM atau `Information[]` pada Wolfram Language):
Memungkinkan pengguna mengueri informasi sesi dan metadata lingkungan eksekusi secara langsung.

1. **Kueri Satuan Tersedia (`inspect units` / `units()`):**
   - Menampilkan seluruh daftar satuan bawaan (*built-in SI*) beserta satuan kustom yang telah didefinisikan melalui `unit <name> = <expr>`.
2. **Kueri Variabel & Rumus Aktif:**
   - Menampilkan daftar variabel yang sedang hidup dalam sesi beserta nilai dan dimensi fisiknya.
   - Menampilkan daftar rumus (`form`) yang terdaftar.
3. **Filter Berdasarkan Dimensi Fisika:**
   - Kemampuan memeriksa satuan apa saja yang kompatibel dengan suatu dimensi tertentu (misal: mencari seluruh unit berdimensi *Length* atau *Velocity*).

---
*Dokumentasi ini menjadi acuan tunggal pengerjaan revisi tugas besar dan kodingan PhysLang v2.*
