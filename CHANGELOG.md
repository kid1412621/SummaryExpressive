# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.0] - 2026-07-29

- Bump dependencies (koog 1.1.1, compose 2.26.01.00)
- Bump java to 25
- Target SDK set to 37

## [1.3.1] - 2026-01-04

### Changed
- Bump dependencies
- Update kotlin to 2.3.0
- Update gradle wrapper to 9.2.1

## [1.3.0] - 2025-12-01

### Added

- Mistral support
- OpenRouter support

## [1.2.0] - 2025-11-13

### Added

- Custom model name support
- Qwen support
- Ollama support

### Fixed

- History type filter buttons overflow

## [1.1.0] - 2025-10-10

### Added

- Text-to-speech speed adjustment

## [1.0.3] - 2025-10-09

### Fixed

- Unable to summarize documents and images

## [1.0.2] - 2025-10-08

### Fixed

- Instant summary overlay remaining in recent apps
- Instant summary sometimes showing old summary 

## [1.0.1] - 2025-10-08

### Fixed

- Bilibili shared link extraction

## [1.0.0] - 2025-10-07

### Added

- Filter history by type
- Support document in share sheet

### Changed

- History saving implementation (BREAKING CHANGE: Previous saves will gone)
- improved LLM interaction

## [0.9.0] - 2025-09-25

### Added

- Support multi lang read out
- Collapse and expand the summary result card

### Changed

- History deletion action using swipe gesture instead of long press
- Improve article extraction accuracy

## [0.8.0] - 2025-09-21

### Added

- Support BiliBili video summary

### Fixed

- Onboarding screen background color

## [0.7.3] - 2025-09-16

### Fixed

- Failed to request deepseek
- Unable to reset AI provider custom base URL

## [0.7.2] - 2025-09-13

### Changed

- UI(icons) improvements

### Fixed

- Model duplication

## [0.7.1] - 2025-09-13

### Changed

- merged API key settings into AI provider

### Fixed

- Auto append https to custom URL if missing

## [0.7.0] - 2025-09-11

### Added

- Support model selection
- Support DeepSeek models

### Fixed

- Google play link in setting page

## [0.6.1] - 2025-09-08

### Fixed

- Unable to share link from other app
- Go back button in settings screen sometimes not working

## [0.5.0] - 2025-09-05

### Added

- Option to auto extract url from shared content

### Changed

- settings screen options order tweak
- preference storage adjustment(will lose current settings)

## [0.4.0] - 2025-09-04

### Added

- Original link in summary card
- Discord link in settings

## [0.3.1] - 2025-09-03

### Fixed

- unable to rerun agent when error happened

## [0.3.0] - 2025-08-28

### Added

- Claude model support
- Gemini custom baseUrl support

### Fixed

- Settings screen back button not working
- Failed to parse YouTube live video link


## [0.2.5] - 2025-08-23

### Changed

- onboarding screen enhancement

## [0.2.2] - 2025-08-10

### Changed

- no markdown output(will update in future)
- remove unused multiline setting
- align to baseline purple color schema

### Fixed

- nav back from settings screen will clear inputs
- history page order by date

## [0.2.1] - 2025-08-09

### Added

- shortcut for instant summarization of clipboard content
- overlay for instant summary via share sheet and text selection toolbar

### Changed

- homescreen overhaul: update document adding button to FAB menu
- hide FABs when scroll down
- regenerate button combined with summarize button
- navigation animation more intuitive

### Fixed

- home screen state not saved during navigation
- share url is not available via chrome quick share menu

## [0.1.0] - 2025-08-03

- First MVP

## [0.0.1] - 2025-07-24

- First POC