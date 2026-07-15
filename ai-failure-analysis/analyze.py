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

import html
import json
import os
import re
import sys
import urllib.request
from datetime import datetime

# Bazi Windows terminalleri (cp1252 gibi eski kod sayfalari) Turkce karakterleri
# (i, s, g, u...) yazdirirken hata veriyor. Ciktiyi UTF-8'e sabitleyerek bu
# sorunu hangi terminalde calisirsa calissin onceden onluyoruz.
sys.stdout.reconfigure(encoding="utf-8")

# ---------------------------------------------------------------------------
# AYARLAR
# ---------------------------------------------------------------------------
# Bu ayarlari KOD DEGISTIRMEDEN, ortam degiskeni (environment variable) ile
# degistirebilirsin. Ollama'dan baska bir sunucuya (ornegin kurum-ici bir
# model sunucusuna) gecmek icin kod duzenlemene gerek yok, sadece calistirmadan
# once asagidaki degiskenleri ayarlaman yeterli.
#
# Windows PowerShell'de ornek kullanim:
#   $env:AI_BACKEND = "chat"
#   $env:AI_URL = "http://ic-sunucu-adresi:port/v1/chat/completions"
#   $env:AI_MODEL = "sunucuda-secilen-model-adi"
#   $env:AI_API_KEY = "gerekiyorsa-token"
#   py analyze.py
#
# Hicbir ayar yapmazsan, varsayilan olarak yerel Ollama'ya baglanir (bugunku hali).
# ---------------------------------------------------------------------------

# "ollama" = Ollama'nin /api/generate formati (bugun kullandigimiz).
# "chat"   = OpenAI-uyumlu /v1/chat/completions formati (kurum-ici model sunucusu
#            veya OpenCode'un baglandigi cogu ic sunucu muhtemelen bunu kullanir).
AI_BACKEND = os.environ.get("AI_BACKEND", "ollama")

# Sunucu adresi.
AI_URL = os.environ.get(
    "AI_URL",
    "http://localhost:11434/api/generate" if AI_BACKEND == "ollama"
    else "http://localhost:11434/v1/chat/completions",
)

# Kullanilacak model adi. Bilgisayarin zayifsa "qwen2.5-coder:3b" birak,
# guclu ise "qwen2.5-coder:7b" yapabilirsin. Kurum-ici sunucuda listeden
# secilen modelin adini AI_MODEL ortam degiskeniyle ver.
MODEL = os.environ.get("AI_MODEL", "qwen2.5-coder:3b")

# Bazi ic sunucular bir API anahtari/token ister. Gerekmiyorsa bos birak.
AI_API_KEY = os.environ.get("AI_API_KEY", "")

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

def collect_report_paths(path):
    """Verilen yol bir KLASOR ise icindeki tum *.json dosyalarini dondurur;
    tek bir DOSYA ise sadece onu dondurur. Boylece yarin cok test yapip her
    raporu bir klasore biriktirebilir, hepsini tek seferde analiz edebilirsin."""
    if os.path.isdir(path):
        files = sorted(
            os.path.join(path, f) for f in os.listdir(path) if f.endswith(".json")
        )
        if not files:
            print(f"[HATA] Klasorde .json rapor yok: {path}")
            sys.exit(1)
        return files
    if not os.path.exists(path):
        print(f"[HATA] Rapor bulunamadi: {path}")
        print("       Once testi calistir (fail uretecek sekilde), sonra bu scripti calistir.")
        sys.exit(1)
    return [path]


def read_failures(report_path):
    """Tek bir JSON raporu okur, fail olan (senaryo, adim, hata) uclulerini dondurur."""
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
# 3) MODELE SOR (yerel Ollama VEYA kurum-ici bir sunucu, cevrimdisi)
# ---------------------------------------------------------------------------

def ask_model(prompt):
    """Prompt'u ayarlanan backend'e (Ollama veya OpenAI-uyumlu bir sunucu)
    gonderir, cevabi metin olarak dondurur. Hangi backend kullanilacagi
    AI_BACKEND ortam degiskeniyle secilir, kod degistirmeye gerek yoktur."""
    if AI_BACKEND == "chat":
        return _ask_chat_completions(prompt)
    return _ask_ollama_generate(prompt)


def _ask_ollama_generate(prompt):
    """Ollama'nin /api/generate formati."""
    payload = json.dumps({
        "model": MODEL,
        "prompt": prompt,
        "stream": False,        # cevabin tamamini tek seferde al
        "options": {
            "temperature": 0,   # rastgeleligi kapat -> kurallara sadik, tutarli cevap
        },
    }).encode("utf-8")

    request = urllib.request.Request(
        AI_URL,
        data=payload,
        headers={"Content-Type": "application/json"},
    )
    data = _send(request)
    return data.get("response", "(bos cevap)").strip()


def _ask_chat_completions(prompt):
    """OpenAI-uyumlu /v1/chat/completions formati. Cogu kurum-ici model
    sunucusu (ve OpenCode'un baglandigi sunucular) bu formati kullanir."""
    payload = json.dumps({
        "model": MODEL,
        "messages": [{"role": "user", "content": prompt}],
        "temperature": 0,
    }).encode("utf-8")

    headers = {"Content-Type": "application/json"}
    if AI_API_KEY:
        headers["Authorization"] = f"Bearer {AI_API_KEY}"

    request = urllib.request.Request(AI_URL, data=payload, headers=headers)
    data = _send(request)
    return data["choices"][0]["message"]["content"].strip()


def _send(request):
    """HTTP istegini gonderir, JSON cevabi dondurur. Baglanti hatasinda
    acik bir mesaj basip cikar (sessizce cokmesin)."""
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.URLError as e:
        print(f"[HATA] Modele baglanilamadi ({AI_URL}): {e}")
        print("       AI_URL / AI_BACKEND / AI_MODEL ayarlarini kontrol et.")
        sys.exit(1)


# ---------------------------------------------------------------------------
# 4) AI CEVABINI AYRISTIR + GORSEL HTML RAPOR URET
# ---------------------------------------------------------------------------

# Her sinifin rengi ve tam adi (gorsel raporda rozet olarak gosterilir).
CLASS_INFO = {
    "A": {"ad": "URUN HATASI", "renk": "#d92d20", "arka": "#fef3f2"},   # kirmizi
    "B": {"ad": "TEST KODU HATASI", "renk": "#b54708", "arka": "#fffaeb"},  # sari/turuncu
    "C": {"ad": "ORTAM HATASI", "renk": "#067647", "arka": "#ecfdf3"},   # yesil
    "?": {"ad": "BELIRSIZ", "renk": "#475467", "arka": "#f9fafb"},       # gri
}


def failure_signature(failure):
    """Ayni kok sebepli fail'leri gruplamak icin bir imza uretir.
    Buyuk projede yuzlerce fail'in cogu ayni koktendir (ornegin ayni ortam
    sorunu). Hata mesajinin ilk anlamli satirini (exception tipi + mesaj)
    imza olarak kullaniyoruz; ayni imzali fail'ler tek grup sayilir."""
    text = (failure.get("error") or "").strip()
    if not text:
        return "(bos hata)"
    first_line = text.splitlines()[0].strip()
    # Cok uzun mesajlarda sondaki degisken kisimlar (koordinat, id vb.) grubu
    # bolmesin diye makul bir uzunlukta kesiyoruz.
    return first_line[:200]


def parse_answer(answer):
    """AI'nin metin cevabindan SINIF / GEREKCE / ONERI alanlarini cikarir.
    Model formati biraz kaydirsa bile calissin diye toleransli arar."""
    def find(label):
        m = re.search(rf"{label}\s*:\s*(.+)", answer, re.IGNORECASE)
        return m.group(1).strip() if m else ""

    sinif_raw = find("SINIF").upper()
    # Ilk gecen A/B/C harfini sinif olarak al; bulunmazsa "?".
    sinif = next((c for c in sinif_raw if c in "ABC"), "?")
    return {
        "sinif": sinif,
        "gerekce": find("GEREKCE") or find("GEREKÇE"),
        "oneri": find("ONERI") or find("ÖNERİ"),
        "raw": answer,
    }


def write_html_report(results, out_path):
    """Sonuclari renkli, kartli, yazdirilabilir (PDF'e cevrilebilir) bir HTML
    raporuna dokup diske yazar. Onların mevcut PDF raporuna da gomulebilir."""
    grup_sayisi = len(results)
    toplam_test = sum(r.get("count", 1) for r in results)
    # Ozet: her sinif KAC TESTI etkiliyor (grup sayisi degil, etkilenen test sayisi).
    sayim = {"A": 0, "B": 0, "C": 0, "?": 0}
    for r in results:
        sayim[r["parsed"]["sinif"]] += r.get("count", 1)

    # Ozet rozetleri (kac test A, kac test B, kac test C).
    ozet_kutulari = ""
    for c in ["A", "B", "C"]:
        info = CLASS_INFO[c]
        ozet_kutulari += f"""
        <div class="ozet-kutu" style="background:{info['arka']};border-color:{info['renk']}">
            <div class="ozet-sayi" style="color:{info['renk']}">{sayim[c]}</div>
            <div class="ozet-ad">{c} · {info['ad']}</div>
        </div>"""

    # Her HATA GRUBU icin bir kart.
    kartlar = ""
    for i, r in enumerate(results, start=1):
        p = r["parsed"]
        info = CLASS_INFO[p["sinif"]]
        count = r.get("count", 1)
        # Bu hata birden cok testi etkiliyorsa, etkilenen senaryolari listele.
        etkilenen = ""
        if count > 1:
            adlar = "".join(f"<li>{html.escape(s)}</li>" for s in r.get("scenarios", []))
            etkilenen = f'<details class="etki"><summary>{count} testi etkiliyor</summary><ul>{adlar}</ul></details>'
        else:
            etkilenen = '<div class="adim">1 testi etkiliyor</div>'
        kartlar += f"""
        <div class="kart">
            <div class="kart-bas">
                <span class="numara">HATA GRUBU #{i}</span>
                <span class="rozet" style="background:{info['renk']}">{p['sinif']} · {info['ad']}</span>
            </div>
            <div class="senaryo">{html.escape(r['failure']['scenario'])}</div>
            <div class="adim">Çöken adım: {html.escape(r['failure']['step'])}</div>
            {etkilenen}
            <div class="satir"><b>Gerekçe:</b> {html.escape(p['gerekce'] or '-')}</div>
            <div class="satir"><b>Öneri:</b> {html.escape(p['oneri'] or '-')}</div>
        </div>"""

    tarih = datetime.now().strftime("%Y-%m-%d %H:%M")
    page = f"""<!doctype html>
<html lang="tr"><head><meta charset="utf-8">
<title>AI Fail Analiz Raporu</title>
<style>
  body {{ font-family: Segoe UI, Arial, sans-serif; color:#1d2939; margin:32px; background:#fff; }}
  h1 {{ font-size:22px; margin:0 0 4px; }}
  .alt {{ color:#667085; font-size:13px; margin-bottom:20px; }}
  .ozet {{ display:flex; gap:12px; margin-bottom:24px; flex-wrap:wrap; }}
  .ozet-kutu {{ border:2px solid; border-radius:10px; padding:12px 20px; min-width:120px; }}
  .ozet-sayi {{ font-size:28px; font-weight:700; }}
  .ozet-ad {{ font-size:12px; color:#475467; }}
  .kart {{ border:1px solid #e4e7ec; border-radius:10px; padding:16px; margin-bottom:14px; }}
  .kart-bas {{ display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }}
  .numara {{ font-weight:700; color:#475467; }}
  .rozet {{ color:#fff; font-size:12px; font-weight:600; padding:4px 10px; border-radius:20px; }}
  .senaryo {{ font-size:16px; font-weight:600; margin-bottom:2px; }}
  .adim {{ color:#667085; font-size:13px; margin-bottom:10px; }}
  .satir {{ font-size:14px; margin:4px 0; }}
  .etki {{ font-size:13px; margin:6px 0; color:#475467; }}
  .etki summary {{ cursor:pointer; font-weight:600; }}
  .etki ul {{ margin:6px 0 0 18px; padding:0; }}
  .etki li {{ margin:2px 0; }}
</style></head>
<body>
  <h1>Yapay Zeka Destekli Test Fail Analizi</h1>
  <div class="alt">Oluşturulma: {tarih} · {toplam_test} fail, {grup_sayisi} benzersiz hata tipi · Model: {html.escape(MODEL)}</div>
  <div class="ozet">{ozet_kutulari}</div>
  {kartlar}
</body></html>"""

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(page)


# ---------------------------------------------------------------------------
# ANA AKIS
# ---------------------------------------------------------------------------

def main():
    # Varsayilan rapor yolu target/cucumber-report.json; ama istenirse
    # komut satirindan baska bir dosya verilebilir (ornek fail ile test icin):
    #   python analyze.py sample-failures/broken-locator.json
    report_path = sys.argv[1] if len(sys.argv) > 1 else REPORT_PATH

    # Tek dosya da olabilir, bir klasor dolusu rapor da. Hepsindeki fail'leri topla.
    failures = []
    for path in collect_report_paths(report_path):
        failures.extend(read_failures(path))

    if not failures:
        print("[BILGI] Raporda fail olan adim yok. Tum testler gecmis gorunuyor.")
        return

    # Ayni sebepten coken fail'leri grupla. Boylece 50 fail'in 30'u ayni
    # koktense, AI'ya 50 kez degil, benzersiz hata sayisi kadar soruyoruz.
    groups = {}
    for failure in failures:
        groups.setdefault(failure_signature(failure), []).append(failure)

    print(f"Toplam {len(failures)} fail, {len(groups)} benzersiz hata tipine gruplandi.")
    print(f"Backend: {AI_BACKEND} | Model: {MODEL} | URL: {AI_URL}\n")

    # Hem duz metin rapor, hem de her grup icin (fail + AI cevabi) sonuc listesi.
    report_lines = [f"AI Fail Analizi Raporu - {datetime.now():%Y-%m-%d %H:%M}\n"]
    results = []

    for i, (sig, group) in enumerate(groups.items(), start=1):
        representative = group[0]        # gruptan bir temsilci fail
        count = len(group)               # bu hata kac testi etkiliyor
        print("=" * 70)
        print(f"HATA GRUBU #{i} ({count} testi etkiliyor)")
        print(f"Temsilci senaryo: {representative['scenario']}")
        print(f"Coken adim: {representative['step']}")
        print("-" * 70)
        print("Yapay zeka dusunuyor...\n")

        answer = ask_model(build_prompt(representative))
        print(answer)
        print()

        results.append({
            "failure": representative,
            "parsed": parse_answer(answer),
            "count": count,
            "scenarios": [g["scenario"] for g in group],
        })

        report_lines.append("=" * 60)
        report_lines.append(f"HATA GRUBU #{i} ({count} testi etkiliyor)")
        report_lines.append(f"Temsilci: {representative['scenario']} | Adim: {representative['step']}")
        report_lines.append(answer)
        report_lines.append("")

    # Onemli olanlar ustte gorunsun: once sinif (A>B>C), sonra etki sayisi.
    oncelik = {"A": 0, "B": 1, "C": 2, "?": 3}
    results.sort(key=lambda r: (oncelik.get(r["parsed"]["sinif"], 9), -r["count"]))

    # 1) Duz metin rapor.
    txt_path = os.path.join(SCRIPT_DIR, "analiz-raporu.txt")
    with open(txt_path, "w", encoding="utf-8") as f:
        f.write("\n".join(report_lines))

    # 2) Gorsel HTML rapor (tarayicida acilir, Ctrl+P ile PDF'e cevrilir).
    html_path = os.path.join(SCRIPT_DIR, "analiz-raporu.html")
    write_html_report(results, html_path)

    print("=" * 70)
    print(f"Metin rapor : {txt_path}")
    print(f"Gorsel rapor: {html_path}  (tarayicida ac, Ctrl+P -> PDF)")


if __name__ == "__main__":
    main()
