"""Whole Quran + sahih hadith for Noor (sources only, nothing typed by hand).
   quran.json  : [[surah name, [ayah texts...]], ...]  from api.alquran.cloud quran-uthmani
   hadith.json : [[matn, "bukhari"|"muslim", number], ...]  from fawazahmed0/hadith-api
                 (Sahih al-Bukhari and Sahih Muslim only; only sayings the narration attributes to the
                 Prophet, the quoted words exactly as in the source).
Run: python -I fetch_more.py <assets-dir> <bukhari.json> <muslim.json>
"""
import json, re, sys, urllib.request

out, bfile, mfile = sys.argv[1:4]

q = json.load(urllib.request.urlopen("https://api.alquran.cloud/v1/quran/quran-uthmani", timeout=120))["data"]["surahs"]
quran = []
for s in q:
    ayat = []
    for a in s["ayahs"]:
        t = a["text"].lstrip("﻿")
        w = t.split(" ")
        if a["numberInSurah"] == 1 and s["number"] not in (1, 9) and w[0].endswith("سْمِ") and len(w) > 4:
            t = " ".join(w[4:])
        ayat.append(t)
    quran.append([s["name"], ayat])
print("surahs", len(quran), "ayat", sum(len(x[1]) for x in quran))

SPEAKER = re.compile(r"(رَسُولَ اللَّهِ|رَسُولُ اللَّهِ|رسول الله|النَّبِيَّ|النَّبِيُّ|النبي)")
SAYS = re.compile(r"(قَالَ|يَقُولُ|قال|يقول)")
hadith, seen = [], set()
for path, book in ((bfile, "bukhari"), (mfile, "muslim")):
    for h in json.load(open(path, encoding="utf-8"))["hadiths"]:
        t = h["text"].replace("‏", "").replace("‎", "")
        i, j = t.find('"'), t.rfind('"')
        if i < 0 or j <= i:
            continue
        before, matn = t[:i], re.sub(r"\s+", " ", t[i + 1:j]).strip(" ،,.")
        tail = before[-160:]
        if not (SPEAKER.search(tail) and SAYS.search(tail[-60:])):
            continue
        if '"' in matn or not (25 <= len(matn) <= 230) or matn in seen:
            continue
        seen.add(matn)
        hadith.append([matn, book, h["hadithnumber"]])
print("hadith", len(hadith), "bukhari", sum(1 for x in hadith if x[1] == "bukhari"))

json.dump(quran, open(f"{out}/quran.json", "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
json.dump(hadith, open(f"{out}/hadith.json", "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
