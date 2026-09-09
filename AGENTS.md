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
- **Clean Layered Architecture & UDF**:
  - **UI Layer (`ui/`, `vm/`)**: Compose + ViewModels exposing reactive `StateFlow`, adhering to Unidirectional Data Flow (UDF). ViewModels encapsulate coroutines within `viewModelScope` and consume domain UseCases exclusively (ViewModels never interact directly with repository interfaces). ViewModels never accept another ViewModel's UI state.
  - **Domain Layer (`domain/`, `model/`, `exception/`)**: Pure Kotlin domain logic completely decoupled from UI, Room, and Android framework classes:
    - **UseCases (`domain/usecase/`)**: Discrete business operations (`SummarizeContentUseCase`, `GetHistorySummariesUseCase`, `DeleteHistorySummaryUseCase`, `RestoreHistorySummaryUseCase`, `UpdateProviderConfigUseCase`, `GetOnboardingStatusUseCase`, `SetOnboardingStatusUseCase`, `GetUserSettingsUseCase`, `UpdateUserPreferencesUseCase`).
    - **Repository Interfaces (`domain/repository/`)**: Contracts for data access (`HistoryRepository`, `UserPreferencesRepository`, `AIProviderConfigRepository`).
    - **Provider Interfaces (`domain/provider/`)**: Abstractions for system capabilities (`DocumentMetadataProvider`, `AppLocaleProvider`).
    - **Domain Models (`model/`)**: Pure Kotlin data classes and enums (`HistorySummary`, `SummaryType`, `SummaryOutput`, `SummarySource`, `ProviderConfig`, `UserPreferences`, `UserSettings`).
    - **Centralized Exceptions (`exception/`)**: Centralized custom exceptions (`SummaryException`) with string resource localization support.
  - **Data Layer (`data/`)**: Implements domain repository interfaces as the Single Source of Truth (SSOT). Coordinates between local Room database, ProtoBuf DataStore, Ktor network client, and LLM engines. Room entities (`HistoryEntity`, `AIProviderConfigEntity`) and mappers live in `data/local/database/`.
- **Component Placement Conventions**:
  - **Global Reusable Components**: Place in `ui/component/` (e.g. `SummaryCard`, `LlmSwitcher`, `LlmIndicator`, `LogoIcon`, `ClickablePasteIcon`).
  - **Page-Specific Components**: Place alongside the screen in `ui/page/` (or `ui/page/<feature>/`) scoped to that specific screen/feature (e.g. `BilibiliLoginScreen.kt` sheet).
- **Dependency Injection (Hilt)**:
  - Repository interfaces are bound to implementations in `di/RepositoryModule.kt` via `@Binds`.
  - App-wide infrastructure (Database, DAOs, HTTP client, LLM handler) is provided in `di/AppModule.kt`.
- **Database (Room)**:
  - Database name: `summary_expressive_db`.
  - Main entity: `HistoryEntity` with `HistoryDao`. Mapped to pure domain `HistorySummary` via extension mappers.
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

#### Dependency Injection (`di/`)
- **`AppModule.kt`**: Provides Room database, DAOs, LLM handler, and Ktor HTTP client
- **`RepositoryModule.kt`**: `@Binds` domain repository interfaces to data layer implementations

#### Domain Layer (`domain/`, `model/`, `exception/`)
- **`domain/usecase/`**: Discrete business operations
  - `SummarizeContentUseCase.kt`: Content resolution, provider selection, LLM execution, history persistence
  - `GetHistorySummariesUseCase.kt`: Filtered and searched history streams
  - `DeleteHistorySummaryUseCase.kt`: History entry deletion
  - `RestoreHistorySummaryUseCase.kt`: History entry restoration
  - `UpdateProviderConfigUseCase.kt`: URL normalization, model management
  - `GetOnboardingStatusUseCase.kt`: Onboarding status observation stream
  - `SetOnboardingStatusUseCase.kt`: Onboarding completion persistence
  - `GetUserSettingsUseCase.kt`: Reactive user preferences and provider configs stream
  - `UpdateUserPreferencesUseCase.kt`: User settings mutations and prompt defaults
- **`domain/repository/`**: Domain contracts for data access
  - `HistoryRepository.kt`: History repository contract
  - `UserPreferencesRepository.kt`: User settings contract
  - `AIProviderConfigRepository.kt`: AI provider credentials contract
- **`domain/provider/`**: System capability abstractions
  - `DocumentMetadataProvider.kt`: File name resolution
  - `AppLocaleProvider.kt`: Locale resolution
- **`model/`**: Pure domain models without framework annotations
  - `ExtractedContent.kt`, `HistorySummary.kt`, `ProviderConfig.kt`, `SummaryData.kt`, `SummaryLength.kt`, `SummaryOutput.kt`, `SummarySource.kt`, `SummaryType.kt`, `UserPreferences.kt`, `UserSettings.kt`, `VideoSubtype.kt`
- **`exception/`**: Custom exceptions hierarchy (`SummaryException.kt`)

#### Data Layer (`data/`)
- **`local/database/`**: Room database, DAOs, entities, and mappers
  - `AppDatabase.kt`, `HistoryDao.kt`, `AIProviderConfigDao.kt`
  - `entity/`: `HistoryEntity.kt`, `AIProviderConfigEntity.kt`
  - `mapper/`: `HistoryMapper.kt`
- **`converters/`**: Room type converters
- **`local/datastore/`**: User preferences ProtoBuf DataStore and serializer
- **`provider/`**: Android-backed system providers (`AndroidDocumentMetadataProvider.kt`, `AndroidAppLocaleProvider.kt`)
- **`repository/`**: Implementations of domain repository interfaces
  - `HistoryRepositoryImpl.kt`
  - `UserPreferencesRepositoryImpl.kt`
  - `AIProviderConfigRepositoryImpl.kt`

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
- **`AppViewModel`**: App-level lifecycle, onboarding status (`isOnboarded`), intent routing
- **`SettingsViewModel`**: App configuration, AI provider credentials, model management, appearance
- **`SummaryViewModel`**: Summarization state, length result caching, invokes `SummarizeContentUseCase`
- **`HistoryViewModel`**: Reactive search query, history filtering, deletion and restore
- **`UiState.kt`**: State classes for UI rendering (`SettingsUiState`, `SummarizationState`, `AppStartAction`)

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
