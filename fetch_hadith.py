"""Well-known hadith from Sahih al-Bukhari and Sahih Muslim only, located by a key phrase and copied exactly
from the source text (fawazahmed0/hadith-api). Output hadith.json: [[matn, "bukhari"|"muslim", number], ...]
Run: python -I fetch_hadith.py <assets-dir> <bukhari.json> <muslim.json> <report.txt>
"""
import json, re, sys

out, bfile, mfile, report = sys.argv[1:5]
PHRASES = """إنما الأعمال بالنيات
لا يؤمن أحدكم حتى يحب لأخيه
المسلم من سلم المسلمون من لسانه ويده
من كان يؤمن بالله واليوم الآخر فليقل خيرا
الدين النصيحة
الطهور شطر الإيمان
خيركم من تعلم القرآن وعلمه
كلمتان خفيفتان على اللسان
لا تغضب
يسروا ولا تعسروا
الكلمة الطيبة صدقة
من لا يرحم لا يرحم
إن الله لا ينظر إلى صوركم
ليس الشديد بالصرعة
أحب الأعمال إلى الله أدومها
من سلك طريقا يلتمس فيه علما
من صام رمضان إيمانا واحتسابا
المؤمن للمؤمن كالبنيان
مثل المؤمنين في توادهم
من نفس عن مؤمن كربة
إن الله رفيق يحب الرفق
عجبا لأمر المؤمن
اليد العليا خير من اليد السفلى
اتقوا النار ولو بشق تمرة
إذا مات الإنسان انقطع عنه عمله
ما نقصت صدقة من مال
انظروا إلى من أسفل منكم
البر حسن الخلق
حق المسلم على المسلم خمس
من يرد الله به خيرا يفقهه في الدين
بلغوا عني ولو آية
إن الله كتب الإحسان على كل شيء
كل معروف صدقة
لا يدخل الجنة من كان في قلبه مثقال ذرة من كبر
الحياء لا يأتي إلا بخير
أفشوا السلام بينكم
صلاة الجماعة تفضل صلاة الفذ
أرأيتم لو أن نهرا بباب أحدكم
من قام رمضان إيمانا واحتسابا
الدعاء هو العبادة
من دل على خير فله مثل أجر فاعله
لا تحقرن من المعروف شيئا
إن الله طيب لا يقبل إلا طيبا
من حسن إسلام المرء
ارحموا من في الأرض
لا ضرر ولا ضرار
خير الناس أنفعهم
إن الله يحب إذا عمل أحدكم عملا أن يتقنه
الصلوات الخمس والجمعة إلى الجمعة
من صلى البردين دخل الجنة
لا تدخلون الجنة حتى تؤمنوا
تبسمك في وجه أخيك
إن الصدق يهدي إلى البر
مثل الذي يذكر ربه والذي لا يذكر ربه
من قال سبحان الله وبحمده في يوم مائة مرة
إن في الجسد مضغة
""".strip().splitlines()

TASHKEEL = re.compile(r"[ً-ْٰـۡ‏‎]")


def plain(s):
    s = TASHKEEL.sub("", s)
    return s.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ى", "ي").replace("ة", "ه")


def books():
    for path, book in ((bfile, "bukhari"), (mfile, "muslim")):
        for h in json.load(open(path, encoding="utf-8"))["hadiths"]:
            yield book, h["hadithnumber"], h["text"].replace("‏", "").replace("‎", "")


ALL = list(books())
# wrong match (different narration) / answer cut off
EXCLUDE = [["bukhari", 3729], ["bukhari", 528]]
res, rep = [], open(report, "w", encoding="utf-8")
for ph in PHRASES:
    key = plain(ph)
    found = None
    for book, num, t in ALL:
        p = plain(t)
        k = p.find(key)
        if k < 0:
            continue
        # map the plain index back to the original text
        idx, n = 0, 0
        while n < k and idx < len(t):
            if not TASHKEEL.match(t[idx]):
                n += 1
            idx += 1
        q1 = t.rfind('"', 0, idx)
        q2 = t.find('"', idx)
        if q1 >= 0 and q2 > idx and t.count('"', 0, idx) % 2 == 1:
            matn = t[q1 + 1:q2]
        else:
            end = len(t)
            for stop in ('"', "»"):
                e = t.find(stop, idx)
                if e > 0: end = min(end, e)
            matn = t[idx:end]
        matn = re.sub(r"\s+", " ", matn).strip(" ،,.:")
        if [book, num] in EXCLUDE:
            continue
        if 20 <= len(matn) <= 300:
            found = [matn, book, num]
            break
    rep.write(f"{ph}\n   -> {found}\n")
    if found and found[0] not in [r[0] for r in res]:
        res.append(found)
json.dump(res, open(f"{out}/hadith.json", "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
print("hadith", len(res), "of", len(PHRASES))
