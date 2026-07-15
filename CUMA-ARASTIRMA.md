# AI Destekli Test Üretme Araçları — Araştırma Raporu ve Cuma Planı

Görev: Test senaryosunu kendisi yazan ve o senaryodan test kodunu üreten
AI araçlarını araştırmak; uygun olanları Trendyol otomasyon projesi
(Java 17 + Selenium + Cucumber + Maven) üzerinde denemek.

---

## 1) Büyük Resim: 4 Farklı Yaklaşım Var

Hepsi "AI test yazıyor" diyor ama 4 farklı şey kastediyorlar:

| Yaklaşım | Nasıl çalışır | Örnek araçlar | Bizim projeye entegrasyon |
|---|---|---|---|
| 1. Otonom crawler | URL ver, kendisi gezer, senaryo üretir, KENDİ bulutunda koşar | TestSprite, Functionize, QA.tech | ❌ Kod vermez, ayrı platform |
| 2. Doğal dil → kod export | Sen doğal dille yaz, o koda çevirir | KaneAI, BlinqIO | ⚠️ Kısmi (Java export sınırlı/ücretli) |
| 3. Gherkin'i direkt koşan ajan | Feature dosyanı verirsin, kod olmadan AI koşar | Hercules (açık kaynak) | ✅ Mevcut .feature dosyamız direkt girdi |
| 4. LLM ile framework içine üretim | AI, bizim page object'leri kullanarak feature + step def yazar | OpenCode / Copilot / herhangi LLM | ✅ Tam entegrasyon |

**Kritik bulgu:** Senaryo üretimi (Gherkin) olgunlaştı ve her araçta var —
Cucumber'ımıza direkt uyar. Ama KOD üretimi hâlâ JavaScript/Playwright
merkezli; Java + Selenium kodu üreten araç azınlıkta. Ana eleme kriterimiz bu.

---

## 2) Araç Araç İnceleme

### ⭐ Hercules (TestZeus) — açık kaynak, en güçlü demo adayı
- Dünyanın ilk açık kaynak test ajanı. GitHub: test-zeus-ai/testzeus-hercules
- Kurulum: `pip install testzeus-hercules`
- Çalışma şekli: **Gherkin dosyası ver → kod yazmadan kendisi koşar →
  JUnit/XUnit formatında sonuç verir.** UI, API, görsel, erişilebilirlik.
- Mimari: AutoGen tabanlı multi-agent (Planner + Browser + API agent),
  tarayıcıyı altta Playwright ile sürüyor.
- Gereksinim: bir LLM API anahtarı (OpenAI/Anthropic/lokal model destekli).
- Demo hikayesi: "Mevcut Trendyol feature dosyamızı, step definition'lara
  dokunmadan Hercules'e verdik, AI kendisi koştu."
- Açık kaynak olduğu için ileride kurum içine taşınabilir (güvenlik uyumu).

### KaneAI (LambdaTest / TestMu) — kod export eden en iyi ticari aday
- Doğal dille (Jira ticket, PRD, ekran görüntüsünden bile) test yazarsın →
  pozitif/negatif/edge case'ler dahil yapılandırılmış test üretir →
  **kodu export eder: Selenium, Playwright, Cypress, Appium.**
- Ücretsiz: 100 otomasyon dakikası + 300 dk HyperExecute.
- DİKKAT: trial planda export Selenium+Python ile sınırlı olabilir;
  Java export muhtemelen ücretli planda. Cuma günü CANLI DOĞRULA → bulgu.

### TestSprite — otonom crawler kategorisinin temsilcisi (mentorun duyduğu)
- URL veya PRD ver → AI uygulamayı gezer, test case üretir, kendi bulut
  sandbox'ında koşar, fail'de kök neden + düzeltme önerisi verir. MCP ile
  Cursor/VSCode entegrasyonu var.
- Ücretsiz: 150 kredi/ay. Kurulumsuz, ~10 dakikada ilk sonuç.
- EKSİ: Bizim repoya Java/Selenium kodu ÜRETMEZ — kendi platformunda koşar.
- Trendyol canlı site (login doğrulama, bot koruması) → crawler login
  arkasına geçemeyebilir. Yedek plan: saucedemo.com'da dene, kısıtı not et.

### Katalon (StudioAssist + TrueTest) — "Selenium ailesinden" ticari aday
- Selenium/Appium motoru üzerine kurulu → bizim dünyaya en yakın platform.
- StudioAssist: düz yazıdan test scripti üretir (GPT tabanlı).
- TrueTest (2025): gerçek kullanıcı davranışından otonom test üretir.
- DİKKAT: ücretsiz sürümde AI özellikleri YOK — trial ile denenebilir.

### testRigor — güçlü ama "codeless" (bize uymaz)
- Testi düz İngilizce yazarsın, kendisi çalıştırır. Selenium kodunu
  tamamen ortadan kaldırma iddiasında.
- ❌ Kod üretmez — Selenium'un YERİNE geçer, framework'ümüze eklenmez.
- Rapor notu: "güçlü ama mimarimize uymaz (codeless, vendor lock-in)".

### BlinqIO ("AI Test Engineer") — yarım uyum
- Düz İngilizce / BDD / kayıt → Gherkin senaryosu + otomasyon kodu üretir.
- ⚠️ Gherkin tarafı Cucumber'a uyar AMA ürettiği kod Playwright +
  Cucumber.js (JavaScript) — Java step definition üretmiyor.

### Gherkinizer / Workik — hızlı, ücretsiz senaryo üreticileri
- User story yapıştır → hazır Gherkin senaryoları. 10 dakikalık demo malzemesi.
- Senaryo üretme tarafının kanıtı olarak ekran görüntüsü al.

### Qodo (eski CodiumAI) — farklı katman: unit test üretimi
- IDE içinde, koda bakarak JUnit testi üretir (Java destekli).
- Bireysel kullanım ücretsiz. E2E değil ama "test üretiminin diğer katmanı"
  olarak rapora eklenebilir (örn. ConfigReader için otomatik JUnit).

### Yeni nesil startuplar (pazar genişliği göstergesi)
- **Momentic** (YC W24, $19M yatırım; Notion, Webflow, Retool müşterisi) —
  doğal dilden E2E test + self-healing. Bulut/JS dünyası.
- **QA.tech** — otonom keşif testi ajanı. **Spur** — e-ticarete özel QA
  ajanı (HelloFresh: regresyon süresinde %95 azalma iddiası).
- Kurumsal devler: Mabl, Virtuoso, Functionize, ACCELQ — hepsi bulut /
  codeless, hiçbiri Java kodu üretmiyor.
- Pazara 1.5 milyar dolar+ yatırım, 40+ startup → "bu alan geçici heves
  değil, sektör bu yöne gidiyor" cümlesinin kanıtı.

---

## 3) Karşılaştırma Tablosu

| Araç | Senaryo üretir | Kod üretir | Java+Selenium uyumu | Ücretsiz |
|---|---|---|---|---|
| Hercules | — (Gherkin'i sen verirsin) | ❌ (koda gerek yok, direkt koşar) | ✅ .feature direkt girdi | ✅ açık kaynak (LLM anahtarı gerek) |
| KaneAI | ✅ | ✅ Selenium export | ⚠️ Java export trial'da belirsiz | ✅ 100 dk |
| TestSprite | ✅ | ❌ kendi bulutu | ⚠️ koşar ama kod vermez | ✅ 150 kredi/ay |
| Katalon | ✅ (StudioAssist) | ✅ (Groovy) | ⚠️ Yakın ekosistem, AI paralı | ⚠️ trial |
| testRigor | ✅ | ❌ codeless | ❌ Selenium'un yerine geçer | ✅ var |
| BlinqIO | ✅ Gherkin | ✅ ama Playwright/JS | ⚠️ yarım | ✅ var |
| Gherkinizer/Workik | ✅ | ❌ | ✅ Gherkin direkt kullanılır | ✅ |
| Qodo | — | ✅ JUnit (unit) | ✅ Java | ✅ bireysel |
| LLM (OpenCode vb.) | ✅ | ✅ | ✅ TAM UYUM | zaten var |

---

## 4) Cuma Günü Deneme Planı

| Sıra | Araç | Süre | Yapılacak | Beklenen çıktı |
|---|---|---|---|---|
| 1 | Hercules | ~1 saat | pip install, LLM anahtarı ayarla, mevcut feature dosyasını ver | "Kendi feature dosyamız, sıfır kod, AI koştu" |
| 2 | KaneAI | ~1 saat | Trial aç, doğal dille login testi yazdır, Selenium export dene | Java çıkıyor mu? (çıkmasa da bulgu) |
| 3 | TestSprite | ~45 dk | Ücretsiz hesap, Trendyol URL ver; takılırsa saucedemo.com | Ürettiği senaryolar + login kısıtı notu |
| 4 | LLM ile üretim | ~1 saat | AI'ya user story ver → Gherkin + Java step def ürettir → mvn test | Yeşil test = tam entegrasyon kanıtı |
| 5 | (kalan vakit) Gherkinizer / Katalon trial | 30 dk | Hızlı ekran görüntüleri | Senaryo üretim örnekleri |
| 6 | Rapor | ~1 saat | Karşılaştırma tablosu + ekran görüntüleri + tez | Sunuma hazır bulgu seti |

### Pratik notlar
- Her araçta EKRAN GÖRÜNTÜSÜ al (üretilen senaryo, üretilen kod, sonuç).
- Trendyol login/bot koruması bulut araçları engelleyebilir → saucedemo.com
  yedek test sitesi; kısıt da rapora "canlı prod sitelerde crawler sınırı" olarak girer.
- KaneAI'da kritik soru: "Export → Selenium → dil seçenekleri neler?"
- Hercules için LLM API anahtarı gerekecek (OpenAI/Anthropic/lokal).
- Şirket verisi HİÇBİR araca yüklenmez; sadece Trendyol projesi kullanılır.

---

## 5) Raporun Ana Tezi (sunumun kapanış cümlesi)

> "Pazar ikiye ayrılıyor: senaryo üretimi olgunlaştı ve stack'ten bağımsız —
> Gherkin üreten her araç Cucumber'ımıza uyar. Ama kod üretimi hâlâ
> JS/Playwright merkezli. Java+Selenium'a gerçek entegrasyon için üç yol var:
> (1) Gherkin'i koda çevirmeden koşan açık kaynak ajanlar (Hercules),
> (2) Selenium export'u olan platformlar (KaneAI),
> (3) LLM'e kendi framework'ümüz içinde ürettirme.
> En pratik ve güvenlik-uyumlu kombinasyon: Hercules + LLM destekli üretim;
> ticari tarafta izlenecek adaylar KaneAI ve Katalon."

---

## 6) Kaynaklar

- Hercules: https://github.com/test-zeus-ai/testzeus-hercules · https://testzeus.com/hercules
- KaneAI: https://www.testmuai.com/kane-ai/
- TestSprite: https://www.testsprite.com/ · https://www.testsprite.com/pricing
- Katalon: https://katalon.com/ · https://qaskills.sh/blog/katalon-ai-testing-guide
- testRigor: https://testrigor.com/
- BlinqIO: https://www.docs.blinq.io/recorder/gherkin-scenarios-explained.html
- Gherkinizer: https://gherkinizer.com/ · Workik: https://workik.com/cucumber-test-case-generator
- Momentic: https://momentic.ai/ · Spur: https://www.spurtest.com/ · QA.tech: https://qa.tech/
- Pazar analizi ($1.5B): https://agentmarketcap.ai/blog/2026/04/08/momentic-autonomous-qa-agent-testing-market-2026
- Araç listeleri: https://github.com/tugkanboz/awesome-ai-testing ·
  https://www.shiplight.ai/blog/best-agentic-qa-tools-2026 ·
  https://testguild.com/7-innovative-ai-test-automation-tools-future-third-wave/
