# Handover: Fitur `form` di PhysLang v2

> **Konteks:** working tree akan di-**revert total**. Dokumen ini adalah satu-satunya acuan.
> Semua perubahan sebelumnya dibuang. Mulai dari nol, tapi dengan peta yang jelas.

---

## 1. Tujuan fitur

PhysLang v2 bergaya *lab notebook*. Fitur `form` = deklarasi formula fisika **satu baris**, tanpa block statement, tanpa `return`.

```phys
form Gaya(m, a) = m * a
form Tekanan(F, A) = F / A

gaya    = Gaya(10 kg, 2 m/s^2)     # → 20.0 N
tekanan = Tekanan(gaya, 4 m^2)     # → 5.0 Pa
```

**Scope minimal.** Jangan melebar ke introspection, block statement, closure canggih, atau refactor arsitektur. Yang diminta hanya: parser → AST → semantic → runtime → test → contoh `assets/`.

---

## 2. Arsitektur proyek (titik sentuh)

| Layer | File | Peran untuk `form` |
|---|---|---|
| Lexer | `lang/lexer/Lexer.kt`, `TokenType.kt` | `form` harus jadi keyword; `parseIdentifier()` sudah ambil `[A-Za-z0-9_]+` |
| Parser | `lang/parser/Parser.kt` | tambah `parseFormStmt()`, dispatch di `parseStatement()`; perbaiki `nud()` untuk unit berpangkat |
| Precedence | `lang/parser/Precedence.kt` | pastikan `TokenType.In` punya precedence (bug lama: `Unexpected token 'in' after expression`) |
| AST | `lang/ast/Stmt.kt`, `StmtVisitor.kt`, `Expr.kt`, `Node.kt`, `Literal.kt` | `FormDeclStmt(name, parameterNames, body)`; `UnitNode.BaseUnit / CompositeUnit / PowerUnit` |
| Runtime value | `lang/ast/RuntimeValue.kt` | `RuntimeValue.Formula` yang implement `Callable` |
| Callable | `lang/core/Callable.kt` | `arity()` + `call(evaluator, args)` |
| Environment | `lang/core/Environment.kt` | `putVar/getVar/putUnit/getUnit/hasUnit/removeVar`; parent-scope untuk pemanggilan formula |
| Evaluator | `lang/ast/visitors/PhysicsEvaluator.kt` | `visitFormDeclStmt`, `visitCallExpr`, `visitQuantityExpr` |
| Semantic | `lang/semantic/SemanticAnalyzer.kt`, `SemanticType.kt` | registrasi formula + validasi dimensi/arity |
| Test | `src/test/kotlin/FormTest.kt` | **spesifikasi perilaku** — ini kontraknya |

---

## 3. Checklist pekerjaan

- [ ] **1. Lexer**: daftarkan `form` sebagai keyword → `TokenType.Form`.
- [ ] **2. AST**: `data class FormDeclStmt(name, parameterNames: List<String>, body: Expr)` + `accept()` + `visitFormDeclStmt` di `StmtVisitor`.
- [ ] **3. Parser**: `parseFormStmt()` — `form IDENT ( IDENT (, IDENT)* ) = expression`, validasi kurung/tutup, error jelas kalau `=` hilang.
- [ ] **4. Runtime**: `RuntimeValue.Formula(name, parameters, body, closure)` implement `Callable`:
  ```kotlin
  override fun call(evaluator: PhysicsEvaluator, arguments: List<RuntimeValue>): RuntimeValue {
      val local = Environment(closure)
      parameters.forEachIndexed { i, p -> local.putVar(p, arguments.elementAtOrNull(i)) }
      return evaluator.withEnvironment(local) { body.accept(evaluator) }
  }
  ```
- [ ] **5. Evaluator**: `visitFormDeclStmt` → `putVar(name, Formula(... currentEnvironment))`; `visitCallExpr` → resolusi callee (`NativeFunction` | `Formula`), cek arity, panggil.
- [ ] **6. SemanticType**: tambahkan
  ```kotlin
  object StringType : SemanticType()
  object Boolean   : SemanticType()
  object Null      : SemanticType()
  data class Formula(
      val name: String,
      val parameters: List<String>,
      val body: Expr,
      val returnType: SemanticType,
      val closure: Environment
  ) : SemanticType()
  ```
  Plus `displayName` untuk pesan diagnostic.
- [ ] **7. Semantic analyzer**: `visitFormDeclStmt` evaluasi body dengan parameter placeholder (`PhysicsValue(1.0, IntArray(7))`), simpan tipe. `visitCallExpr` inferensi return type dari tipe argumen aktual; cek arity; pesan error jelas.
- [ ] **8. `FormTest` hijau** — jangan ubah test kecuali assertion yang jelas salah format (mis. `3.1536E7 s`, bukan `31536000.0 s`).
- [ ] **9. Full suite** — `./gradlew test` tanpa regresi di `VariableTest`, `ConversionExprTest`, `BlockStatementTest`.
- [ ] **10. Assets** — buat contoh study case `.phys` di `assets/` (lihat §6).

---

## 4. Bug yang sudah teridentifikasi (jangan diulang)

### 4.1 Unit berpangkat dihitung dua kali
`visitQuantityExpr` lama mengalikan nilai literal dengan `scale` unit yang **sudah** memuat pangkat → `4 m^2` jadi `16.0`. Akibatnya `Tekanan(Gaya(10 kg, 2 m/s^2), 4 m^2)` = `1.25 Pa` bukan `5.0 Pa`.

**Aturan benar:** nilai literal **hanya** nilai numerik; `scale` unit diterapkan sekali.

```kotlin
override fun visitQuantityExpr(expr: QuantityExpr): RuntimeValue {
    val unit = resolveUnit(expr.unit.asUnitNode()) ?: error("Unknown unit")
    val numeric = (expr.value.accept(this) as PhysicsValue).value
    val dimensions = unit.dimensions.copyOf()
    // Unit berpangkat: scale dipangkatkan, TAPI nilai tidak
    return PhysicsValue(numeric, dimensions, unit.scale)
}
```

### 4.2 Perkalian/pembagian membuang satuan
Hasil `Multiply`/`Divide` harus **mewarisi** `scale` dari operan, dan **biarkan `unitName = null`** supaya `toString()` menebak satuan turunan (`N`, `Pa`, `J`). Jangan hardcode registry satuan turunan.

### 4.3 `10 m / s` dianggap `(10 m) / variabel s`
`Tokenizer` mengeluarkan `NumberLiteral, Identifier(m), Divide, Identifier(s)`. Parser `nud()` hanya menerima satu identifier setelah angka, jadi `s` jatuh ke jalur variabel → `Variable or formula 's' is not defined`.

**Butuh keputusan:** dukung `UnitNode.CompositeUnit(left, operator, right)` di `nud()` untuk unit pembagi/perkalian (`m/s`, `kg m/s^2`), **atau** minta user menulis `10 m/s` dengan notasi lain. Rekomendasi: dukung CompositeUnit + PowerUnit, karena test memakainya.

### 4.4 Closure hilang saat revert
`Formula` dan `SemanticType.Formula` **wajib** menyimpan `closure: Environment`, karena `RuntimeValue.Formula.call()` membuat `Environment(closure)` dan `SemanticType.Formula` dipakai ulang di `visitAssignExpr` / `visitVarDeclStmt`. Kalau closure tidak ada lagi, `SemanticAnalyzer` tidak compile (`Unresolved reference 'Formula'`) dan formula tidak bisa akses satuan luar.

---

## 5. Nilai yang sudah terverifikasi (sebelum revert)

| Ekspresi | Hasil |
|---|---|
| `Gaya(10 kg, 2 m/s^2)` | `20.0 N` |
| Dimensi N | `[1, 1, -2, 0, 0, 0, 0]` |
| Dimensi Pa | `[1, -1, -2, 0, 0, 0, 0]` |
| `4 m^2` (setelah fix) | `4.0 m^2` |
| `2^3` | `8.0` |
| `2 m / s^2` | `2.0` (benar, karena scale `s^2` = 1) |
| `20 m/s in kph` (dengan `unit kph = 1000 m / 3600 s`) | `72.0 kph` |
| `TahunDetik()` | `3.1536E7 s` (format scientific, **bukan** `31536000.0 s`) |

---

## 6. Isi `FormTest.kt` yang harus lulus

| Test | Script | Ekspektasi |
|---|---|---|
| Dua parameter | `form Gaya(m,a) = m*a`; `Gaya(10 kg, 2 m/s^2)` | `20.0 N` |
| Formula nested | `Tekanan(Gaya(...), 4 m^2)` | `5.0 Pa` |
| Pangkat | `form Ek(m,v) = 0.5*m*v^2`; `Ek(2 kg, 10 m/s)` | `100.0 J` |
| Tanpa parameter | `form TahunDetik() = 365*24*3600 s` | `3.1536E7 s` |
| Arity salah | `Gaya(1 kg)` | diagnostic error |
| Parameter duplikat | `form X(a,a) = a` | diagnostic error |
| Reuse | definisi → panggil → panggil lagi | konsisten |
| Konversi dalam formula | body pakai `in` | sesuai |
| Satuan kustom dalam formula | formula pakai `kph` | sesuai |

---

## 7. Contoh study case untuk `assets/` (buat setelah test hijau)

```
assets/
  mekanika_dasar.phys      # Gaya, Usaha, Energi kinetik, Tekanan
  kinematika.phys          # v = s/t, a = Δv/t, konversi kph
  satuan_kustom.phys       # unit kph, kn (knot), konversi
```

Contoh isi `mekanika_dasar.phys`:
```phys
# Mekanika dasar — PhysLang v2
form Gaya(m, a)     = m * a
form Usaha(F, s)    = F * s
form Tekanan(F, A)  = F / A
form Ek(m, v)       = 0.5 * m * v^2

gaya = Gaya(10 kg, 2 m/s^2)
print gaya                       # 20.0 N

print Usaha(gaya, 5 m)           # 100.0 J
print Tekanan(gaya, 4 m^2)       # 5.0 Pa
print Ek(2 kg, 10 m/s)           # 100.0 J
```

---

## 8. Perintah & jebakan build

```bash
./gradlew compileKotlin
python3 -c "import shutil; shutil.rmtree('build/test-results/test', ignore_errors=True)"
./gradlew test --tests FormTest --info 2>&1 | grep "expected:"
./gradlew test
```

- Output Gradle sering **timeout** di mount Drive D → tulis ke log (`> /tmp/x.log`) lalu `grep`.
- `build/test-results/test` harus dihapus kalau hasil terlihat basi.
- `read_file` pernah mengembalikan isi **glitch** (`->` alih-alih `:`) → verifikasi dengan `search_files` bila mencurigakan.
- `Environment.hasUnit()` / `getUnit()` sudah menangani **unit built-in + custom**. Jangan buat registry sendiri.
- `BoolVal` runtime = `PhysicsValue(1.0/0.0, IntArray(7))`, `NullVal` = `RuntimeValue.NullValue`, `StringVal` = `StringValue` — pertahankan konsistensi ini.

---

## 9. Urutan kerja yang efisien

1. Lexer keyword `form` → compile.
2. AST + StmtVisitor → compile.
3. Parser `parseFormStmt()` → compile. Ambil **satu** test sederhana (`form Kuadrat(x) = x*x`) untuk verifikasi parse + eval.
4. Runtime `Formula` + `Callable` + `withEnvironment` → compile.
5. SemanticType lengkap (`Formula`, `StringType`, `Boolean`, `Null`) → compile.
6. SemanticAnalyzer registrasi + call inference → compile.
7. Fix unit berpangkat (`visitQuantityExpr`) + CompositeUnit di `nud()`.
8. `FormTest` hijau satu per satu.
9. `./gradlew test` penuh.
10. Assets.

**Jangan lompat ke assets sebelum test hijau.**
