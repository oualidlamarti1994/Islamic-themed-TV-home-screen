# Islamic-themed TV home screen (Noor · نور)

[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-0070ba?logo=paypal&logoColor=white&style=for-the-badge)](https://www.paypal.com/donate/?business=oualidlamarti94%40gmail.com&currency_code=EUR&item_name=Islamic-themed-TV-home-screen)

An ad-free, Islamic home screen for Android TV / Google TV (Android 8+). Small, plain Java, no libraries, no tracking.

![Verse screen](docs/verse-screen.png)

## Features

- **Home screen (launcher)** with no ads: clock, Gregorian + Hijri date, greeting «السلام عليكم», today's five prayer
  times with the next one highlighted and a countdown, and rows for Favourites, Games, All apps and Quick tiles
  (Settings, Wi-Fi, Bluetooth, Display, Surah Al-Kahf, Verse). Hold OK on an app to add/remove it from Favourites.
- **Animated background with a 3D feel**: eight-pointed star tessellation, 3D-tilting star rosettes, a gold light
  sweep, rising gold particles, a shimmering mihrab arch and a turning crescent (tuned for 2 GB TVs).
- **Rotating Quran and hadith**: every 30 seconds a random ayah from the **whole Quran** (Uthmani text) or a **sahih
  hadith** from **Sahih al-Bukhari / Sahih Muslim only**, with its reference.
- **Verse on wake**: every time the TV screen turns on, a full-screen verse inside a gold mihrab with the Basmala,
  surah reference and a hadith.
- **Friday**: asks whether to play **Surah Al-Kahf**. Nothing plays without the viewer pressing *Play*.
- **Prayer times** from [Aladhan](https://aladhan.com) by latitude/longitude and calculation method, with a small
  card at each prayer time; the adhan plays only if the viewer presses *Play*. Never wakes a TV in standby.
- **Islamic occasions** from the Hijri date: Jumu'ah, Ramadan, the last ten nights, both Eids, Arafah, Ashura,
  Hijri new year, the White Days.

## Build (Windows, no Gradle)

Needs Android Studio's bundled JDK and the Android SDK (platform 33, build-tools 35), plus Python 3.

```powershell
powershell -ExecutionPolicy Bypass -File fetch_assets.ps1   # fonts, Quran, hadith
powershell -ExecutionPolicy Bypass -File make-art.ps1       # icon, banner, wallpaper (optional, already in repo)
powershell -ExecutionPolicy Bypass -File build.ps1          # -> build\noor.apk (signs with a new local key)
```

## Install on the TV (ADB over Wi-Fi)

Turn on *Developer options → USB debugging* on the TV, then:

```sh
adb connect <tv-ip>:5555
adb install -r build/noor.apk
adb shell appops set app.noor.tv SYSTEM_ALERT_WINDOW allow     # verse on wake over other apps
adb shell appops set app.noor.tv APP_AUTO_START allow          # TCL TVs only (TclAppBoot)
adb shell cmd package set-home-activity --user 0 app.noor.tv/.HomeActivity
```

Set your city and method (Aladhan method numbers: 2 ISNA, 3 MWL, 4 Umm al-Qura, 13 Diyanet, 21 Morocco):

```sh
# JSON, base64-encoded: {"lat":50.73,"lon":7.10,"method":21,"city":"Bonn"}
adb shell content call --uri content://app.noor.tv.config --method set64 --arg <base64-json>
```

Other keys: `wake_on`, `wake_seconds`, `friday_on`, `adhan_mode` (`ask` | `silent` | `off`), `kahf_url`, `adhan_url`, `favourites`.

On Google TV the stock launcher takes Home back while it is enabled; `pm disable-user --user 0 com.google.android.apps.tv.launcherx`
makes Noor stick. **Careful:** on some Google TVs the setup wizard (`com.google.android.tungsten.setupwraith`) then
grabs Home, and disabling it can make the TV forget a Wi-Fi network it saved; re-join Wi-Fi afterwards if that happens.
To go back: `pm enable --user 0 com.google.android.apps.tv.launcherx` (and setupwraith).

## Support / Spenden

If Noor is useful for you, you can support the project with a donation (PayPal: oualidlamarti94@gmail.com).
Jazakum Allahu khayran · جزاكم الله خيرا

[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-0070ba?logo=paypal&logoColor=white&style=for-the-badge)](https://www.paypal.com/donate/?business=oualidlamarti94%40gmail.com&currency_code=EUR&item_name=Islamic-themed-TV-home-screen)

## Credits

- Quran text: [Tanzil](https://tanzil.net) Uthmani text via [alquran.cloud](https://alquran.cloud) (verbatim; the
  Basmala is shown once above the verse).
- Hadith: [fawazahmed0/hadith-api](https://github.com/fawazahmed0/hadith-api) — Sahih al-Bukhari and Sahih Muslim only.
- 99 Names and prayer times: [Aladhan API](https://aladhan.com).
- Fonts: [Amiri and Amiri Quran](https://github.com/aliftype/amiri) (SIL Open Font License).
- Default audio streams: Surah Al-Kahf by Maher Al-Muaiqly ([mp3quran.net](https://mp3quran.net)), adhan by Omar
  Al-Kazabri ([assabile.com](https://www.assabile.com)).

Code: MIT © oualidlamarti1994
