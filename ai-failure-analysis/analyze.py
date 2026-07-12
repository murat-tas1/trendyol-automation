"""
AI Fail Analizi - PoC
=====================
Amaç: Cucumber testleri fail olduğunda, hatanın sebebini yapay zekaya
(Ollama'da calisan yerel bir LLM'e) acıklatmak.

Akis:
  1) Cucumber'in urettigi JSON raporunu oku (target/cucumber-report.json)
  2) Fail olan senaryolari ve hata detaylarini (stack trace) cikar
  3) Her fail icin yapay zekaya "bu neden coktu?" diye sor
  4) Yapay zeka fail'i 3 sinifa ayirsin + aciklama + oneri versin:
        (A) Urun/uygulama hatasi  -> gercek bug
        (B) Test kodu hatasi       -> locator eskimis, yanlis beklenti
        (C) Ortam/zamanlama hatasi -> timeout, ag, simulator gec acildi
  5) Sonuclari ekrana ve bir rapor dosyasina yaz

Not: Hicbir veri disari cikmaz. Model tamamen yerelde (Ollama) calisir.
"""

import json
import os
import sys
import urllib.request
from datetime import datetime

# ---------------------------------------------------------------------------
# AYARLAR
# ---------------------------------------------------------------------------

# Ollama'nin yerel adresi. Ollama kurulunca bilgisayarinda bu adreste calisir.
OLLAMA_URL = "http://localhost:11434/api/generate"

# Kullanilacak model. Bilgisayarin zayifsa "qwen2.5-coder:3b" birak,
# guclu ise "qwen2.5-coder:7b" yapabilirsin.
MODEL = "qwen2.5-coder:3b"

# Bu scriptin bulundugu klasor (rapor dosyasini buraya yazacagiz, nereden
# calistirilirsa calistirilsin dogru yere yazsin diye).
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

# Cucumber'in yazdigi JSON rapor dosyasi (RunCucumberTest.java icinde ayarlandi).
REPORT_PATH = os.path.join("target", "cucumber-report.json")

# Fail olunca Hooks.java'nin kaydettigi ekran goruntusu / sayfa kaynagi klasoru.
ARTIFACTS_DIR = os.path.join("target", "failure-artifacts")

# Stack trace cok uzun olabilir; modele bogmadan anlamli bir pencere gonderelim.
MAX_STACKTRACE_CHARS = 2000


# ---------------------------------------------------------------------------
# 1) CUCUMBER JSON RAPORUNDAN FAIL'LERI CIKAR
# ---------------------------------------------------------------------------

def read_failures(report_path):
    """JSON raporu okur, fail olan (senaryo, adim, hata) uclulerini dondurur."""
    if not os.path.exists(report_path):
        print(f"[HATA] Rapor bulunamadi: {report_path}")
        print("       Once testi calistir (fail uretecek sekilde), sonra bu scripti calistir.")
        sys.exit(1)

    with open(report_path, encoding="utf-8") as f:
        features = json.load(f)

    failures = []
    # JSON yapisi: features -> elements(senaryolar) -> steps(adimlar) -> result
    for feature in features:
        for scenario in feature.get("elements", []):
            for step in scenario.get("steps", []):
                result = step.get("result", {})
                if result.get("status") == "failed":
                    failures.append({
                        "scenario": scenario.get("name", "bilinmeyen senaryo"),
                        "step": f"{step.get('keyword', '').strip()} {step.get('name', '')}".strip(),
                        "error": result.get("error_message", "(hata mesaji yok)"),
                    })
    return failures


# ---------------------------------------------------------------------------
# 2) YAPAY ZEKAYA SORULACAK METNI (PROMPT) HAZIRLA
# ---------------------------------------------------------------------------

def build_prompt(failure):
    """Fail bilgisinden, modele gonderilecek yapilandirilmis soruyu olusturur."""
    stacktrace = failure["error"][:MAX_STACKTRACE_CHARS]

    return f"""Sen bir test otomasyon uzmanisin. Selenium + Cucumber testinin FAIL kaydini
analiz edip 3 siniftan BIRINE koyacaksin.

Karar icin ONCE hata (exception) tipine bak. Su kurallari uygula:

  - "AssertionError" veya "Expected ... but was ..." (beklenen deger ile gercek
    deger farkli) -> SINIF A (URUN HATASI). Cunku uygulama YANLIS VERI uretmis.
    Ornek: beklenen "Skechers" ama gelen "Nike" -> uygulama yanlis urunu koymus -> A.

  - "NoSuchElementException" / "Unable to locate element" (element hic bulunamadi)
    -> SINIF B (TEST KODU HATASI). Cunku locator muhtemelen eskimis.

  - "TimeoutException" / "tried for N second(s)" (element beklendi ama sure doldu)
    -> SINIF C (ORTAM HATASI). Cunku bu genelde zamanlama/yavaslik sorunudur.

Siniflarin anlami:
  (A) URUN HATASI      -> uygulamada gercek bug, sistem yanlis calisiyor / yanlis veri
  (B) TEST KODU HATASI -> uygulama iyi ama test hatali (locator eskimis)
  (C) ORTAM HATASI     -> uygulama da test de iyi, sadece zamanlama/ag/timeout sorunu

--- FAIL BILGISI ---
Senaryo: {failure['scenario']}
Coken adim: {failure['step']}
Hata / stack trace:
{stacktrace}
--- BITTI ---

Once hata tipini bul, sonra yukaridaki kurala gore sinifla.
Cevabini AYNEN su formatta, Turkce ver:
SINIF: <A / B / C harflerinden biri>
GEREKCE: <tek cumleyle neden bu sinifa koydun>
ONERI: <tek cumleyle nasil duzeltilir>
"""


# ---------------------------------------------------------------------------
# 3) OLLAMA'YA SOR (yerel, cevrimdisi)
# ---------------------------------------------------------------------------

def ask_ollama(prompt):
    """Prompt'u yerel Ollama modeline gonderir, cevabi metin olarak dondurur."""
    payload = json.dumps({
        "model": MODEL,
        "prompt": prompt,
        "stream": False,        # cevabin tamamini tek seferde al
        "options": {
            "temperature": 0,   # rastgeleligi kapat -> kurallara sadik, tutarli cevap
        },
    }).encode("utf-8")

    request = urllib.request.Request(
        OLLAMA_URL,
        data=payload,
        headers={"Content-Type": "application/json"},
    )

    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            data = json.loads(response.read().decode("utf-8"))
            return data.get("response", "(bos cevap)").strip()
    except urllib.error.URLError:
        print("[HATA] Ollama'ya baglanilamadi. Ollama calisiyor mu?")
        print("       Terminalde 'ollama list' ile kontrol et, model indirili mi bak.")
        sys.exit(1)


# ---------------------------------------------------------------------------
# ANA AKIS
# ---------------------------------------------------------------------------

def main():
    # Varsayilan rapor yolu target/cucumber-report.json; ama istenirse
    # komut satirindan baska bir dosya verilebilir (ornek fail ile test icin):
    #   python analyze.py sample-failures/broken-locator.json
    report_path = sys.argv[1] if len(sys.argv) > 1 else REPORT_PATH
    failures = read_failures(report_path)

    if not failures:
        print("[BILGI] Raporda fail olan adim yok. Tum testler gecmis gorunuyor.")
        return

    print(f"Toplam {len(failures)} fail bulundu. Model: {MODEL}\n")

    # Sonuclari ayni zamanda bir rapor dosyasina yazalim (mentora gostermek icin).
    report_lines = [f"AI Fail Analizi Raporu - {datetime.now():%Y-%m-%d %H:%M}\n"]

    for i, failure in enumerate(failures, start=1):
        print("=" * 70)
        print(f"FAIL #{i}: {failure['scenario']}")
        print(f"Coken adim: {failure['step']}")

        # Bu fail'e ait ekran goruntusu var mi, kullaniciya hatirlatalim.
        safe = "".join(c if c.isalnum() or c in "-_" else "_" for c in failure["scenario"])
        shot = os.path.join(ARTIFACTS_DIR, safe, "screenshot.png")
        if os.path.exists(shot):
            print(f"Ekran goruntusu: {shot}")

        print("-" * 70)
        print("Yapay zeka dusunuyor...\n")

        answer = ask_ollama(build_prompt(failure))
        print(answer)
        print()

        report_lines.append("=" * 60)
        report_lines.append(f"FAIL #{i}: {failure['scenario']}")
        report_lines.append(f"Adim: {failure['step']}")
        report_lines.append(answer)
        report_lines.append("")

    # Raporu diske yaz (scriptin kendi klasorune).
    out_path = os.path.join(SCRIPT_DIR, "analiz-raporu.txt")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(report_lines))
    print("=" * 70)
    print(f"Rapor kaydedildi: {out_path}")


if __name__ == "__main__":
    main()
