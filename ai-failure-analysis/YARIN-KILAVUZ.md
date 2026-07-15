# Yarın için kılavuz — Failure Analysis PoC (ŞİRKET PROJESİ)

Hedef: gerçek testten fail alıp, AI ile A/B/C sınıflandırıp, sunulabilir çıktı üretmek.
NOT: Yarın ŞİRKETİN büyük Cucumber projesinde çalışacaksın (Trendyol değil).
Trendyol = kanıtlanmış prototip. Aynı mantığı büyük projeye taşıyorsun.

## ⚠️ ÖNEMLİ: Büyük proje paylaşılan gerçek kod — bozarken dikkat!

Trendyol kendi projendi, rahatça kırardık. Büyük projede bir şeyi bozup
UNUTURSAN başkalarının işini bozarsın. Fail elde etmek için güvenli sıra:

1. **EN GÜVENLİ:** Zaten var olan/eski fail raporlarını kullan (proje zaten
   rapor üretiyor). Hiçbir şey bozmana gerek yok, eski bir stack trace al.
2. Bir testi çalıştır, doğal çökeni yakala.
3. **Dikkatli:** Küçük bir değişiklik yap (locator boz) → stack trace'i al →
   HEMEN geri al, KAYDETME/COMMIT ETME. `git diff` ile temiz olduğunu doğrula.

## 0) Zaman bütçesi (kendine dayat, tek adımda takılma)

- OpenCode / kurum-ici model sunucusu keşfi: **max 30-45 dakika**
- Bulamazsan otomasyona uğraşma, "4B) B PLANI"na geç (garanti çalışır)

## 1) Fail elde et (3 farklı tip iyi olur)

Farklı hata tipleri, AI'nın gerçekten ayırt ettiğini gösterir:

- **NoSuchElementException** (element bulunamadı) -> beklenen sınıf **B**
- **TimeoutException** (süre doldu) -> beklenen sınıf **C**
- **AssertionError** (beklenen != gerçek) -> beklenen sınıf **A**

Bunları ya eski raporlarda bul, ya da (dikkatli, geri alarak) üret:
- Locator'a fazladan karakter ekle -> NoSuchElement
- Wait süresini 1 sn yap -> Timeout
- Bir assertion'ın beklenen değerini değiştir -> AssertionError

## 2) Delillerin oluştuğunu doğrula

Her fail sonrası şuraya bak:
- `target/cucumber-report.json` var mı, içinde `"status": "failed"` var mı
- `target/failure-artifacts/<senaryo-adı>/` klasöründe `screenshot.png` var mı

## 3) OpenCode / kurum-ici model sunucusunu keşfet (max 30-45 dk)

Sırayla dene, ilk işe yarayanda dur:

1. Terminalde `opencode --help` — yardım menüsü var mı
2. Config dosyası ara: `opencode.json`, `.opencoderc`, ya da OpenCode'un
   ayarlar/settings ekranı — içinde bir **URL** ve **model adı** yazıyor olmalı
3. OpenCode arayüzünde model seçim listesine bak, hangi modeller var, adlarını not al
4. Biri varsa, IT/altyapı dokümantasyonunda "model server", "LLM endpoint",
   "AI gateway" gibi bir sayfa ara

**Bulduğun bilgiyi not al:** URL, model adı, API key gerekip gerekmediği,
format (JSON'da `prompt` mi yoksa `messages` mi bekliyor).

## 4A) Script'i bağla (bulabilirsen)

Kod değiştirmene GEREK YOK — sadece PowerShell'de bu değişkenleri ayarla,
sonra scripti normal çalıştır:

```powershell
$env:AI_BACKEND = "chat"
$env:AI_URL = "http://BULDUGUN-ADRES/v1/chat/completions"
$env:AI_MODEL = "BULDUGUN-MODEL-ADI"
$env:AI_API_KEY = "GEREKIYORSA-TOKEN"   # gerekmiyorsa bu satiri atla

cd "ai-failure-analysis"
py analyze.py sample-failures/three-failures.json
```

Önce **örnek dosyayla** dene (`three-failures.json`) — bağlantı çalışıyor mu
diye. Çalışırsa, gerçek fail'i de dene:

```powershell
py analyze.py ..\target\cucumber-report.json
```

Bağlanamazsa hata mesajı net söyleyecek (`[HATA] Modele baglanilamadi ...`).
5 dakika uğraş, olmazsa B PLANI'na geç.

## 4B) B PLANI — Elle demo (GARANTİ çalışır, script'e gerek yok)

Script çalışmasa da PoC geçerli. Stack trace neyse onu AI'ya vermek yeter.

**Stack trace'i nereden alırsın?**
- Konsoldan: test çökünce terminalde çıkan hata metnini fareyle seç, Ctrl+C
- VEYA JSON'dan: cucumber-report.json içinde "error_message" alanını kopyala

**Adımlar:**
1. Bir fail'in stack trace'ini kopyala (yukarıdaki gibi)
2. OpenCode'u aç, kurum-ici modeli seç
3. Aşağıdaki promptu yapıştır, [BURAYA...] yerine stack trace'i koy
4. AI'nın cevabını al (SINIF/GEREKÇE/ÖNERİ)
5. Ekran görüntüsü çek (Win + Shift + S) — stack trace + AI cevabı görünsün
6. Farklı hata tipleri için tekrarla (NoSuchElement / Timeout / AssertionError)

**Yapıştırılacak prompt (kurallı, daha isabetli):**
```
Sen bir test otomasyon uzmanisin. Asagidaki Selenium + Cucumber testinin
FAIL kaydini incele ve 3 siniftan BIRINE koy.

Karar kurallari:
- "AssertionError" / "Expected ... but was ..." (beklenen ile gercek farkli)
  -> SINIF A (URUN HATASI): uygulama yanlis veri uretmis.
- "NoSuchElementException" / "Unable to locate element" (bulunamadi)
  -> SINIF B (TEST KODU HATASI): locator eskimis olabilir.
- "TimeoutException" / "tried for N second(s)" (sure doldu)
  -> SINIF C (ORTAM HATASI): zamanlama/ag sorunu.

Siniflar:
(A) URUN HATASI - uygulamada gercek bug
(B) TEST KODU HATASI - test hatali (locator eskimis)
(C) ORTAM HATASI - zamanlama/timeout/ag sorunu

--- FAIL BILGISI ---
[BURAYA STACK TRACE'I YAPISTIR]
--- BITTI ---

Cevabini AYNEN su formatta, Turkce ver:
SINIF: <A / B / C>
GEREKCE: <tek cumle>
ONERI: <tek cumle>
```

3 farklı fail tipi için 3 kez yap, ekran görüntüsü al. Bu bile güçlü bir demo.

## 5) Çıktıyı topla (ne olursa olsun yap)

Script çalıştıysa 2 rapor üretir (script'in kendi klasöründe):
- `analiz-raporu.txt` — düz metin
- `analiz-raporu.html` — GÖRSEL rapor: renkli rozetler (A=kırmızı, B=sarı,
  C=yeşil), özet sayılar, her hata grubu bir kart. Tarayıcıda aç, **Ctrl+P ->
  PDF olarak kaydet** dersen sunulabilir PDF olur. Bunu mentora göster.

Büyük projede yüzlerce fail olabilir; script aynı sebepten çökenleri GRUPLAR
(50 fail'in 30'u aynı köktense, AI'ya 50 değil ~benzersiz-hata kadar sorar) ve
gerçek bug'ları (A) raporun en üstüne koyar. Yani ölçek sorun değil.

Elle yaptıysan: 3 fail'in ekran görüntüleri + kısa bir özet tablo:

| Fail | Beklenen sınıf | AI'nın cevabı | Doğru mu? |
|---|---|---|---|
| Locator bozuldu | B | ... | ... |
| Timeout | C | ... | ... |
| Assertion | A | ... | ... |

## 6) Unutma

- Bozduğun kodu (locator, timeout, assertion) **geri al** (git varsa `git diff`
  ile kontrol et, yoksa elle düzelt) — test tekrar normal çalışır hale gelsin.
- Ne bulduğunu (URL, model adı, format, OpenCode davranışı) not al, akşam
  paylaş — script'i kalıcı olarak düzgün bağlarız.
