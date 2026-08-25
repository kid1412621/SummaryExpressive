# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SummaryExpressive is an AI/LLM summarizer FOSS Android app that summarizes YouTube videos, web articles, images, and documents. It follows [MAD](https://developer.android.com/courses/pathways/android-architecture) principles using pure Kotlin + Jetpack Compose + Material 3 Expressive. The app is BYOK (Bring Your Own Key), allowing users to configure their own LLM API keys.

## Common Commands

### Building the App
```bash
# Clean and build debug APK
./gradlew clean assembleDebug

# Build release APK (requires keystore.properties setup)
./gradlew assembleRelease

# Build specific variant
./gradlew assembleGmsRelease
./gradlew assembleStandaloneRelease
```

### Testing
```bash
# Run unit tests
./gradlew clean test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run a specific test class
./gradlew testDebugUnitTest --tests="me.nanova.summaryexpressive.ExampleUnitTest"
```

### Code Quality
```bash
# Run lint analysis
./gradlew lint

# Format code (follow Kotlin style guide)
```

## Code Style Guidelines
- Make the code structure as simple as possible
- Never use full-qualified class name inline, use import statement instead
- Follow [Kotlin style guide](https://developer.android.com/kotlin/style-guide)

## Dev Environment Tips
- Use `./gradlew clean assembleDebug` to verify the build
- Use `./gradlew clean test` to run tests
- Configure SDK paths in `local.properties`
- Set up signing keys in `keystore.properties` for release builds

## Architecture & Code Structure

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3 Expressive (alpha version for expressive features)
- **DI**: Dagger/Hilt
- **Database**: Room (SQLite)
- **Networking**: Ktor 3.x
- **Async**: Kotlin Coroutines + Flow
- **ML Kit**: For text recognition from images (build flavor dependent)
- **LLM Integration**: Koog library

### Build Flavors
The app uses two product flavors:
- **gms**: Uses Google Play Services ML Kit (smaller APK size, requires Google Play Services)
- **standalone**: Bundles ML model in APK (larger package size, works without Google Play Services)

Configure in `app/build.gradle.kts:58-68`.

### Project Structure

#### Core Application (`app/src/main/kotlin/me/nanova/summaryexpressive/`)
- **`App.kt`**: Application class with `@HiltAndroidApp` annotation
- **`MainActivity.kt`**: Main activity handling deep links, share intents, and initialization

#### Dependency Injection (`di/AppModule.kt`)
Hilt module providing:
- Repositories (`UserPreferencesRepository`, `AIProviderConfigRepository`, `HistoryRepository`)
- Room database and DAOs (`HistoryDao`, `AIProviderConfigDao`)
- LLM handler
- Ktor HTTP client with cookies and JSON serialization

#### Data Layer (`data/`)
- **`local/database/`**: Room database, DAOs, entities, and type converters
  - `AppDatabase.kt`, `HistoryDao.kt`, `AIProviderConfigDao.kt`, `AIProviderConfigEntity.kt`, `converters/`
- **`local/datastore/`**: User preferences ProtoBuf DataStore and serializer
  - `UserPreferencesSerializer.kt`
- **`repository/`**: Single sources of truth for data access
  - `AIProviderConfigRepository.kt`: AI provider credentials and configurations
  - `HistoryRepository.kt`: Summarization history repository
  - `UserPreferencesRepository.kt`: User settings and preferences repository

#### Domain & Data Models (`model/`)
- `ExtractedContent.kt`: Content extraction model
- `HistorySummary.kt`: History entry entity/model
- `ProviderConfig.kt`: AI provider configuration model
- `SummaryData.kt`: Core summary data model interface
- `SummaryLength.kt`: Summarization length enum
- `SummaryOutput.kt`: LLM output model
- `SummarySource.kt`: Input source model (Video, Article, Text, Document)
- `SummaryType.kt`: Supported content types (YouTube, BiliBili, article, image, document, text)
- `UserPreferences.kt`: User preferences state and settings model
- `VideoSubtype.kt`: Video platform classifications

#### Custom Exceptions (`exception/`)
- **`SummaryException.kt`**: Custom exception hierarchy with localization support

#### LLM Integration (`llm/`)
- **`LLMHandler.kt`**: Core handler for LLM interactions, supports multiple providers
- **`AIProvider.kt`**: Provider definitions (OpenAI, Gemini, Claude, DeepSeek, etc.)
- **`Prompts.kt`**: Prompt templates for different content types
- **`CustomModel.kt`**: Custom model configuration
- **`GeminiSanitizingHttpClientEngine.kt`**: Engine decorator for Google Gemini compatibility
- **`tools/`**: Extraction tools
  - `YouTubeTranscriptTool.kt`: YouTube transcript extraction
  - `BiliBiliSubtitleTool.kt`: BiliBili subtitle extraction
  - `ArticleExtractorTool.kt`: Web article content extraction
  - `FileExtractorTool.kt`: Document parsing

#### ViewModels (`vm/`)
- **`AppViewModel`**: App-level state, onboarding, settings, deep links
- **`SummaryViewModel`**: Main summarization logic, content processing
- **`HistoryViewModel`**: History browsing, searching, deletion
- **`UiState.kt`**: State classes for UI rendering

#### UI Layer (`ui/`)
- **`AppNavigation.kt`**: Navigation graph setup
- **`Nav.kt`**: Route definitions
- **`page/`**: Screen composables & page-specific subcomponents
  - `HomeScreen.kt`: Main summary screen
  - `HistoryScreen.kt`: History browser with paging
  - `SettingsScreen.kt`: App configuration
  - `AdvancedSetupScreen.kt`: Advanced prompt setup
  - `OnboardingScreen.kt`: First-run setup
  - `BilibiliLoginScreen.kt`: BiliBili authentication sheet
- **`component/`**: Global reusable UI components (`SummaryCard`, `LlmSwitcher`, `LlmIndicator`, `LogoIcon`, `ClickablePasteIcon`)
- **`theme/`**: Material 3 theming (colors, typography, theme)

#### Services
- **`InstantSummaryActivity.kt`**: Overlay activity for instant summaries via share sheet

### Supported Content Types

| Type | Source | Processing Method |
|------|--------|-------------------|
| YouTube videos | Video URL | Transcript extraction via `YouTubeTranscriptTool` |
| BiliBili videos | Video URL | Subtitle extraction via `BiliBiliSubtitleTool` |
| Articles | URL | Content extraction via `ArticleExtractorTool` |
| Images | File/URI | ML Kit text recognition (flavor-dependent) |
| Documents | File/URI | File parsing via `FileExtractorTool` |
| Text | Direct input | Direct LLM processing |

### LLM Providers

The app supports multiple LLM providers configured in settings:
- **OpenAI**: gpt-4.1, gpt-4.1-mini, gpt-4.1-nano, gpt-40, gpt-4o-mini, gpt-5, gpt-5-mini, gpt-5-nano, o1, o3, o3-mini, o4-mini
- **Gemini**: gemini-2.0-flash, gemini-2.0-flash-001, gemini-2.0-flash-lite, gemini-2.0-flash-lite-001, gemini-2.5-flash, gemini-2.5-pro
- **Claude**: claude-3-haiku, claude-3-opus, claude-3-5-haiku, claude-3-5-sonnet, claude-3-7-sonnet, claude-opus-4-0, claude-sonnet-4-0
- **DeepSeek**: deepseek-chat, deepseek-reasoner
- **Custom models**: User-configurable endpoints

## Key Configuration Files

### `build.gradle.kts` (root)
Defines plugin versions:
- Android Gradle Plugin: 9.3.1
- Kotlin: 2.4.0
- KSP: 2.3.11
- Hilt: 2.60

### `app/build.gradle.kts`
Main app configuration:
- Min SDK: 33 (Android 13)
- Target SDK: 37
- Java 25 toolchain
- Compose BOM: 2026.06.00
- Room: 2.8.4
- Key signing configuration in `keystore.properties`

### `local.properties`
Local SDK paths (not tracked in git)

### `keystore.properties`
Release signing keys (not tracked in git)

## Development Guidelines

### Code Style
- Follow [Kotlin Android Style Guide](https://developer.android.com/kotlin/style-guide)
- Keep code structure as simple as possible
- Follow Android best practices

### Architecture Patterns
- **Layered Architecture & UDF**: UI Layer (Compose + ViewModels with StateFlow), Domain/Model Layer (`model/`, `exception/`), and Data Layer (`data/repository/`, `data/local/`).
- **Repository Pattern**: Repositories act as the Single Source of Truth (SSOT). ViewModels never interact directly with DAOs, DataStores, or raw network clients.
- **Dependency Inversion**: Models and domain logic are decoupled from UI and ViewModel layers.
- **Component Placement Conventions**:
  - **Global Reusable Components**: Place in `ui/component/` (e.g. `SummaryCard`, `LlmSwitcher`, `LlmIndicator`, `LogoIcon`, `ClickablePasteIcon`).
  - **Page-Specific Components**: Place alongside the screen in `ui/page/` (or `ui/page/<feature>/`) scoped specifically to that screen/feature (e.g. `BilibiliLoginScreen.kt` sheet).
- **Custom Exceptions**: Centralized in `exception/SummaryException.kt` with string resource localization support.

### Dependency Injection
- All major components are Hilt-injectable
- Use `@Singleton` for app-wide dependencies
- Use `@ActivityScoped` for activity-level dependencies (when needed)

### Room Database
- Database: `summary_expressive_db`
- Main entity: `HistorySummary` with DAO operations
- Type converters in `converters/` package

### Material 3 Expressive
The app uses Material 3 Expressive alpha features for enhanced UI. Material version: `1.5.0-alpha26`

## Testing
- Unit tests in `src/test/kotlin/`
- Instrumented tests in `src/androidTest/kotlin/`
- Use `./gradlew test` for unit tests
- Use `./gradlew connectedAndroidTest` for instrumented tests

## Build & Release

### Development
- Debug builds are debuggable with application ID suffix `.debug`
- Use `assembleDebug` for development builds

### Release
- Release builds are minified and shrunk
- Requires `keystore.properties` for signing
- Two distribution channels:
  1. **GitHub Releases**: Both gms and standalone variants
  2. **Google Play Store**: GMS variant with Google-managed signing

### Version Info
Current version: 1.3.2 (versionCode 49)
- Update version in `app/build.gradle.kts:21-22`

## Permissions & Security
The app likely requires:
- Internet access (for LLM calls and content extraction)
- Storage access (for document/image processing)
- Clipboard access (for instant summary feature)
- Overlay permission (for instant summary overlay)

## Known Dependencies
- `ai.koog:koog-agents:1.1.1` - Kotlin-based LLM interactions
- `io.ktor:ktor-client-android:3.5.2` - HTTP client
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0` - JSON serialization
- `org.jsoup:jsoup:1.23.1` - HTML parsing
- `io.coil-kt:coil-compose:2.7.0` - Image loading

## Build Warnings & Notes
- ML Kit dependency is flavor-specific (see `app/build.gradle.kts:145-150`)
- ProGuard/R8 rules configured in `app/proguard-rules.pro`
- Some resources excluded from APK (see packaging section)
- Lint rule: MissingTranslation disabled for localization flexibility

---

**Note**: Content from AGENTS.md has been merged into this file.
