<div style="text-align: center;">
<img src=".github/logo.webp"  width=160 height=160 alt="logo">

# Summary Expressive

[![Discord](https://img.shields.io/discord/1406171833119801394?style=flat&logo=discord&link=https%3A%2F%2Fdiscord.gg%2FWjN73wKTqd)](https://discord.gg/WjN73wKTqd)
![GitHub License](https://img.shields.io/github/license/kid1412621/SummaryExpressive)
[![GitHub Release](https://img.shields.io/github/v/release/kid1412621/SummaryExpressive)](https://github.com/kid1412621/SummaryExpressive/releases)
[![Google Play Store](https://img.shields.io/badge/Google_Play-414141?logo=google-play)](https://play.google.com/store/apps/details?id=me.nanova.SummaryExpressive)

[![ChatGPT](https://img.shields.io/badge/ChatGPT-74aa9c?logo=openai&logoColor=white)](#)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-886FBF?logo=googlegemini&logoColor=fff)](#)
[![Claude](https://img.shields.io/badge/Claude-D97757?logo=claude&logoColor=fff)](#)

</div>

Summarize YouTube-Videos, Articles, Images and Documents with AI

[MAD](https://developer.android.com/courses/pathways/android-architecture): Kotlin + Jetpack
Compose + M3 Expressive

## 📱 Screenshots

<div style="text-align: center;">
  <img src=".github/screenshots/screen1.webp" alt="Screenshot 1" width="32%">
  <img src=".github/screenshots/screen2.webp" alt="Screenshot 2" width="32%">
  <img src=".github/screenshots/screen3.webp" alt="Screenshot 3" width="32%">
</div>

## 🔗 Downloads

[![Get it on Github](https://images.weserv.nl/?url=https://s1.ax1x.com/2023/01/12/pSu1a36.png&h=80)](https://github.com/kid1412621/SummaryExpressive/releases)
[![Google Play badge](https://images.weserv.nl/?url=https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png&h=80)](https://play.google.com/store/apps/details?id=me.nanova.SummaryExpressive)

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

| media        | supported types                          |
|--------------|------------------------------------------|
| Video (Link) | Youtube, BiliBili (Planed)               |
| Document     | MS Word, PDF (very long contents planed) |
| Image        | Jpg, Png, Webp (Latin only for now)      |
| Text         | Article link, Plain text                 |

- Multiple LLM models supported

| provider    | models |
|-------------|--------|
| OpenAI      | TBD    |
| Gemini      | TBD    |
| Gemini nano | Planed |
| Claude      | TBD    |

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