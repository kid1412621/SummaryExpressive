<div align="center">

<img width="" src=".github/logo.webp"  width=160 height=160  align="center">

# Summary Expressive

![Discord](https://img.shields.io/discord/1406171833119801394?style=flat&logo=discord&link=https%3A%2F%2Fdiscord.gg%2FWjN73wKTqd)

Summarize YouTube-Videos, Articles, Images and Documents with AI

[MAD](https://developer.android.com/courses/pathways/android-architecture): Kotlin + Jetpack
Compose + M3 Expressive

</div>

## 📱 Screenshots

<div align="center">
<div>
<img src="app/src/main/res/drawable/screen1.webp" width="30%" />
<img src="app/src/main/res/drawable/screen2.webp" width="30%" />
<img src="app/src/main/res/drawable/screen3.webp" width="30%" />

</div>
</div>

## 🔗 Download

[<img src="https://s1.ax1x.com/2023/01/12/pSu1a36.png" alt="Get it on GitHub" height="80">](https://github.com/kid1412621/SummaryExpressive/releases)
[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
alt="Get it on Google Play"
height="80">](https://play.google.com/store/apps/details?id=me.nanova.SummaryExpressive)

### Github Release

There are 2 releases available:

- **standalone** which
  includes [ML model](https://developers.google.com/ml-kit/vision/text-recognition/v2) to recognize
  text from image, it got larger package size.

- **gms** which allows user to download the model from Google Play Services, but GMS required.

### Play Store (coming soon)

The Play Store release bundled with gms version. This package is signed by Google managed key for
simplicity, which means it's not compatible to update in Google Play Store if you installed a github
package previously.

## 📖 Features

- Summarize multiple media types

| media       | supported types                        |
|-------------|----------------------------------------|
| Video(link) | Youtube, BiliBili(Planed)              |
| Document    | MS Word, PDF(very long content planed) |
| Image       | Jpg, Png, Webp (Latin only for now)    |
| Text        | Article link, Plain text               |

- Multiple LLM models supported

| provider    | models |
|-------------|--------|
| OpenAI      | TBD    |
| Gemini      | TBD    |
| Gemini nano | Planed |
| Claude      | Planed |

- [Material 3 Expressive](https://m3.material.io/blog/building-with-m3-expressive) style UI, with
  dynamic color theme.

- Instant summarize via share sheet or text selection toolbar

- Configurable summary length

- Read-Out the summaries

- History search

## 🌟 Credits

- The [original idea](https://github.com/talosross/SummaryYou)
  from [talosross](https://github.com/talosross)

- [Koog](https://koog.ai) for kotlin-based LLM interactions