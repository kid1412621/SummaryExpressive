# AGENTS.md

This file provides guidance for AI agents working with code in this repository.

## Project Overview

SummaryExpressive is an AI/LLM summarizer FOSS Android app that summarizes YouTube/BiliBili videos, web articles, images, and documents. It follows [MAD (Modern Android Development)](https://developer.android.com/courses/pathways/android-architecture) principles using pure Kotlin + Jetpack Compose + Material 3 Expressive. The app is BYOK (Bring Your Own Key), allowing users to configure their own LLM API keys.

---

## Common Commands

### Building
```bash
# Clean and build debug APK
./gradlew clean assembleDebug

# Build release APK (requires keystore.properties setup)
./gradlew assembleRelease

# Build specific flavor variant
./gradlew assembleGmsRelease
./gradlew assembleStandaloneRelease
```

### Testing
```bash
# Run unit tests
./gradlew clean test

# Run a specific unit test class
./gradlew testDebugUnitTest --tests="me.nanova.summaryexpressive.ExampleUnitTest"

# Run instrumented tests on connected device/emulator
./gradlew connectedAndroidTest
```

### Code Quality & Lint
```bash
# Run lint analysis
./gradlew lint
```

> [!TIP]
> Coding agents should use the `android-cli` skill for CLI workflows, SDK management, running/debugging apps on emulators/devices, UI inspection, and taking screenshots.

---

## Development Guidelines

### Code Style
- **Simple structure**: Keep the code structure as simple and readable as possible.
- **Imports**: Never use fully-qualified class names inline; always use import statements.
- **Style guide**: Follow the [Kotlin Android Style Guide](https://developer.android.com/kotlin/style-guide).

### Material 3 Expressive UI
- **Expressive Compliance**: Use the `material-3` skill to check and ensure that any newly added or updated UI complies with Material Design 3 Expressive guidelines (expressive shapes, spring motion physics, tonal elevation, dynamic color, and tokens).
- **Material 3 Version**: The app targets Compose Material 3 Expressive features using `1.5.0-alpha26`.

---

## Architecture

### Architectural Patterns
- **Layered Architecture & UDF**:
  - **UI Layer**: Compose + ViewModels exposing reactive `StateFlow`, adhering to Unidirectional Data Flow (UDF).
  - **Domain / Model Layer (`model/`, `exception/`)**: Pure domain models and centralized exceptions decoupled from UI and Data layers.
  - **Data Layer (`data/`)**: Repositories act as the Single Source of Truth (SSOT). ViewModels never interact directly with DAOs, DataStores, or raw network clients.
- **Component Placement Conventions**:
  - **Global Reusable Components**: Place in `ui/component/` (e.g. `SummaryCard`, `LlmSwitcher`, `LlmIndicator`, `LogoIcon`, `ClickablePasteIcon`).
  - **Page-Specific Components**: Place alongside the screen in `ui/page/` (or `ui/page/<feature>/`) scoped to that specific screen/feature (e.g. `BilibiliLoginScreen.kt` sheet).
- **Dependency Injection (Hilt)**:
  - All major dependencies are Hilt-injectable using `@Singleton` for app-wide dependencies or `@ActivityScoped` for activity-level dependencies.
- **Database (Room)**:
  - Database name: `summary_expressive_db`.
  - Main entity: `HistorySummary` with `HistoryDao`.
  - Custom type converters reside in `data/converters/`.
- **Custom Exceptions**:
  - Centralized in `exception/SummaryException.kt` with string resource localization support.

### Technology Stack & Key Dependencies
- **Language**: Kotlin 2.4.x
- **UI Framework**: Jetpack Compose with Material 3 Expressive (`1.5.0-alpha26` for expressive features)
- **Dependency Injection**: Dagger / Hilt
- **Database**: Room (SQLite) 2.8.x with Paging 3
- **Networking**: Ktor Client 3.x
- **LLM Integration**: Koog library (`ai.koog:koog-agents`, client executors for OpenAI, Gemini, Anthropic, DeepSeek, Mistral, Qwen, Ollama, OpenRouter)
- **HTML Parsing**: Jsoup
- **Image Loading**: Coil (`io.coil-kt:coil-compose`)
- **Async**: Kotlin Coroutines + Flow
- **ML Kit**: Text recognition from images (Google Play Services / standalone bundled)

---

## Code Structure

### Build Flavors & Distribution
The app defines two product flavors under the `distribution` dimension (`app/build.gradle.kts`):
- **`gms`**: Uses Google Play Services ML Kit (`com.google.android.gms:play-services-mlkit-text-recognition`). Smaller APK size, requires Google Play Services. Used for Google Play Store releases (signed with Google-managed key).
- **`standalone`**: Bundles ML model in the APK (`com.google.mlkit:text-recognition`). Larger package size, functions offline without Google Play Services.

### Project Structure

#### Core Application (`app/src/main/kotlin/me/nanova/summaryexpressive/`)
- **`App.kt`**: Application entry point with `@HiltAndroidApp`
- **`MainActivity.kt`**: Main activity handling deep links, share intents, and navigation
- **`InstantSummaryActivity.kt`**: Overlay activity for instant summarization via share sheet or text selection

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

---

## Supported Content & LLM Providers

### Supported Content Types

| Type            | Source       | Processing Method                                 |
|-----------------|--------------|---------------------------------------------------|
| YouTube videos  | Video URL    | Transcript extraction via `YouTubeTranscriptTool` |
| BiliBili videos | Video URL    | Subtitle extraction via `BiliBiliSubtitleTool`    |
| Articles        | URL          | Content extraction via `ArticleExtractorTool`     |
| Images          | File / URI   | ML Kit text recognition (flavor-dependent)        |
| Documents       | File / URI   | File parsing via `FileExtractorTool` (PDF, DOCX)  |
| Text            | Direct input | Direct LLM processing                             |

### Supported LLM Providers

- **OpenAI**
- **Gemini**
- **Claude**
- **DeepSeek**
- **Mistral**
- **Ollama**
- **OpenRouter**
- **DashScope (Qwen)**
- **Bedrock**
- **Custom models** (OpenAI-compatible endpoints)

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

### Key Configuration Files
- **`build.gradle.kts`**: Root build file defining build plugins (AGP, Kotlin, KSP, Hilt).
- **`app/build.gradle.kts`**: App build configuration (Min SDK: 33, Target SDK: 37, Java 25 toolchain, flavors, packaging).
- **`gradle/libs.versions.toml`**: Centralized version catalog for dependencies and plugins.
- **`local.properties`**: Local Android SDK paths (not tracked in git).
- **`keystore.properties`**: Release signing credentials (not tracked in git).

### Build & Packaging Notes
- **ProGuard / R8**: Release builds enable minification and resource shrinking with rules in `app/proguard-rules.pro`.
- **Packaging Exclusions**: Certain license and netty property files are excluded from the APK in `packaging.resources`.
- **Lint**: `MissingTranslation` rule is disabled in `app/build.gradle.kts` for localization flexibility.
