"""Builds Noor's offline data from authentic sources (no Quran text is typed by hand):
   - verses.json : curated ayat, Uthmani text from api.alquran.cloud (edition quran-uthmani)
   - names.json  : the 99 Names of Allah from api.aladhan.com
Run: python -I fetch_data.py <assets-dir>
"""
import json, sys, time, urllib.request

REFS = """
1:1-7 2:152 2:153 2:155-157 2:186 2:201 2:216 2:255 2:286 3:8 3:26 3:92 3:134 3:139 3:159 3:173
3:190-191 3:200 4:28 4:110 6:162-163 7:23 7:56 9:51 9:129 10:62 11:6 11:115 12:86 12:87 13:28
14:7 14:34 14:40-41 15:49 16:18 16:97 16:128 17:24 17:80 17:82 18:10 18:46 18:109 20:25-28 20:46
21:83 21:87 23:118 24:35 25:63 25:74 26:80 27:62 28:24 29:45 29:69 30:21 31:17 33:41-42 33:56
35:2 36:58 36:82 39:53 40:60 42:19 47:7 49:10 49:13 50:16 51:56 55:13 55:26-27 55:60 59:22 59:23
59:24 62:9 62:10 64:11 65:2-3 67:2 89:27-30 93:3-5 94:5-6 94:7-8 97:1-5 103:1-3 108:1-3 112:1-4
113:1-5 114:1-6
""".split()

FRIDAY = {"62:9", "62:10", "18:10", "18:46", "18:109"}


def get(url):
    for i in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "noor-build"}), timeout=30) as r:
                return json.load(r)
        except Exception:
            time.sleep(2 + i * 2)
    raise SystemExit("failed: " + url)


def main(out):
    verses = []
    for ref in REFS:
        s, a = ref.split(":")
        a1, a2 = (a.split("-") + [a])[:2]
        texts, surah = [], None
        for n in range(int(a1), int(a2) + 1):
            d = get(f"https://api.alquran.cloud/v1/ayah/{s}:{n}/quran-uthmani")["data"]
            surah = d["surah"]
            t = d["text"].lstrip("﻿")
            # the API prefixes ayah 1 (except Al-Fatiha and At-Tawba) with the basmala; it is shown separately
            words = t.split(" ")
            if n == 1 and int(s) not in (1, 9) and words[0].endswith("سْمِ") and len(words) > 4:
                t = " ".join(words[4:])
            texts.append(f"{t} ﴿{to_ar(n)}﴾")
        verses.append({"ref": ref, "surah": surah["name"], "surahEn": surah["englishName"],
                       "text": " ".join(texts), "friday": ref in FRIDAY})
        print(ref, len(verses[-1]["text"]))
    names = [{"n": x["number"], "ar": x["name"], "tr": x["transliteration"], "en": x["en"]["meaning"]}
             for x in get("https://api.aladhan.com/v1/asmaAlHusna")["data"]]
    json.dump(verses, open(f"{out}/verses.json", "w", encoding="utf-8"), ensure_ascii=False)
    json.dump(names, open(f"{out}/names.json", "w", encoding="utf-8"), ensure_ascii=False)
    print(len(verses), "verses,", len(names), "names")


def to_ar(n):
    return "".join("٠١٢٣٤٥٦٧٨٩"[int(c)] for c in str(n))


if __name__ == "__main__":
    main(sys.argv[1])
