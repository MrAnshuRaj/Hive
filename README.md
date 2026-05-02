# Hive

Hive is a native Android app written in Java with XML layouts. It currently has two main product areas:

- Photo cleanup: scan for duplicate and similar images, select what to remove, and delete safely.
- Video compression: select device videos, configure compression settings, compress to MP4/H.264, and save the results back to device storage.

This README explains the Java-side file structure and architecture of the app.

## Architecture Overview

The app uses a simple Activity-based architecture:

- Activities handle screen UI, user interaction, and navigation.
- Repository classes handle MediaStore queries, persistence, and feature-specific data access.
- Plain data/model classes carry state between layers.
- RecyclerView adapters render lists and grouped content.
- In-memory singleton session stores keep flow state across multiple activities.

There is no ViewModel, Room, DI framework, or Compose layer in the current app. The project is intentionally straightforward and MVP-oriented.

## Main User Flows

### Photo Cleanup Flow

`Home -> ScanSetup -> Scanning -> ScanResults -> DeleteConfirm -> CleanupSuccess`

What happens:

- The user chooses how to scan photos.
- The app scans MediaStore images in the background.
- Results are grouped into duplicate and similar sets.
- The user selects which items to delete.
- The app performs safe deletion using Android-compatible delete requests.
- A success screen shows freed space.

### Video Compression Flow

`Home -> VideoSelect -> VideoSetup -> VideoCompressing -> VideoResult`

What happens:

- The user selects one or more videos from device storage.
- The user chooses a preset or manual advanced options.
- The app resolves final output settings from source metadata plus user choices.
- Compression runs through Media3 Transformer to MP4/H.264.
- Results are saved to `Movies/SafKaro`.
- The result screen shows output size savings and sharing/playback actions.

## Package Structure

Java source lives under:

`app/src/main/java/com/balraksh/safkaro`

Packages:

- `adapters`: RecyclerView adapters used by screens.
- `data`: plain model classes for the photo cleanup flow and shared media models.
- `repository`: system-facing data and persistence classes.
- `ui`: activities grouped by feature/screen.
- `utils`: helper utilities shared across features.
- `video`: video compression domain models and engine classes.

## Root Entry Point

### `MainActivity.java`

Launcher activity.

Responsibility:

- Starts `PermissionActivity` if required media permissions are missing.
- Otherwise opens `HomeActivity`.

## `adapters` Package

### `ResultGroupAdapter.java`

Adapter for grouped scan results on the photo cleanup results screen.

Responsibility:

- Binds duplicate or similar groups to the results RecyclerView.
- Creates nested horizontal thumbnail lists via `ThumbnailAdapter`.
- Updates UI when selection state changes.

### `ThumbnailAdapter.java`

Horizontal thumbnail adapter used inside each result group.

Responsibility:

- Renders image thumbnails for a duplicate/similar group.
- Marks the best image in the group.
- Toggles selection state for non-best images.

### `VideoListAdapter.java`

RecyclerView adapter for the video selection screen.

Responsibility:

- Displays device videos with thumbnail, duration, size, and selection state.
- Supports tap selection.
- Supports long-press actions routed back to the activity.
- Supports inline delete action on each card.

## `data` Package

### `BucketOption.java`

Represents an image folder/album option loaded from MediaStore.

### `CleanupHistoryEntry.java`

Represents one past cleanup event stored in preferences.

### `CleanupOutcome.java`

Represents the result of a delete operation.

Contains:

- number of deleted items
- number of failed items
- total freed bytes

### `DuplicateGroup.java`

Concrete image result group for exact duplicates.

Extends:

- `MediaGroup`

### `MediaGroup.java`

Base class for grouped image scan results.

Responsibility:

- Holds a list of media items for a group.
- Tracks the best item.
- Computes space-to-save for selected items.

### `MediaImageItem.java`

Represents one image from MediaStore used in the scan/cleanup flow.

Contains:

- MediaStore id
- content URI
- display name
- size
- timestamp
- dimensions
- bucket info
- optional perceptual hash

### `ScanConfig.java`

Represents the user’s scan settings.

Responsibility:

- Stores scan mode and enabled detectors.
- Carries selected bucket info when needed.
- Writes to and reads from intents.

### `ScanMode.java`

Enum describing which images to scan.

Examples:

- all images
- selected folder
- screenshots only

### `ScanProgress.java`

Progress snapshot emitted during scanning.

Contains:

- progress percent
- scanned item count
- match count
- stage label

### `ScanResult.java`

Final result of a scan operation.

Contains:

- duplicate groups
- similar groups
- counts
- potential space savings
- timestamp

### `SimilarGroup.java`

Concrete image result group for perceptually similar images.

Extends:

- `MediaGroup`

### `VideoItem.java`

Represents a video from MediaStore for the video compression flow.

Contains:

- MediaStore id
- content URI
- display name
- size
- duration
- width/height

## `repository` Package

### `CleanupPreferences.java`

SharedPreferences-backed persistence layer for cleanup history and storage metrics.

Responsibility:

- stores total freed bytes
- stores last scan potential bytes
- stores recent cleanup history entries

### `MediaRepository.java`

Core repository for the photo cleanup flow.

Responsibility:

- loads image folders from MediaStore
- queries image items from MediaStore
- finds duplicate groups using file hashing
- finds similar groups using perceptual hashing
- reports scanning progress

This is the main engine for the image-cleaning feature.

### `ScanSessionStore.java`

Singleton in-memory state holder for the active photo scan session.

Responsibility:

- stores current `ScanConfig`
- stores current `ScanResult`
- tracks selected image ids
- exposes selected bytes and selected items
- stores the last cleanup outcome for the success screen

This is the glue between the scan, results, delete, and success screens.

### `VideoCompressionSessionStore.java`

Singleton in-memory state holder for the video compression flow.

Responsibility:

- stores selected videos
- stores active compression request
- stores latest compression progress
- stores final compression results
- broadcasts updates to listeners like `VideoCompressingActivity`

This is the glue between select, setup, progress, and result screens.

### `VideoMediaRepository.java`

Repository for MediaStore video operations.

Responsibility:

- loads device videos from MediaStore
- reads richer metadata using `MediaMetadataRetriever` and `MediaExtractor`
- saves compressed output files back into user-visible storage

Output location:

- `Movies/SafKaro`

## `ui` Package

### `BaseEdgeToEdgeActivity.java`

Base activity shared by most app screens.

Responsibility:

- enables edge-to-edge window layout
- applies system bar insets to the activity content

### `ui/home/HomeActivity.java`

Main dashboard screen.

Responsibility:

- shows storage summary
- opens the photo cleanup flow
- opens the video compression flow
- opens quick clean when a previous scan result exists

### `ui/permission/PermissionActivity.java`

Permission onboarding screen.

Responsibility:

- requests media permissions
- handles denied/permanently denied states
- routes to home when access is granted

### `ui/setup/ScanSetupActivity.java`

Photo scan configuration screen.

Responsibility:

- lets the user choose scan scope
- lets the user enable duplicate/similar detection
- lets the user choose a folder when needed
- starts `ScanningActivity`

### `ui/scanning/ScanningActivity.java`

Progress screen for photo scanning.

Responsibility:

- runs the scan on a background executor
- updates progress UI
- stores the result into `ScanSessionStore`
- opens `ScanResultsActivity`

### `ui/results/ScanResultsActivity.java`

Photo scan result screen.

Responsibility:

- shows duplicate and similar tabs
- renders grouped results
- lets the user select/deselect items
- starts delete confirmation

### `ui/confirm/DeleteConfirmActivity.java`

Delete confirmation and execution screen.

Responsibility:

- confirms deletion
- performs deletion through MediaStore-compatible APIs
- handles recoverable security flows
- builds `CleanupOutcome`
- saves history through `CleanupPreferences`
- opens `CleanupSuccessActivity`

### `ui/cleanup/CleanupSuccessActivity.java`

Final screen after deletion completes.

Responsibility:

- shows freed storage
- shows partial-failure note if needed
- routes back to home

### `ui/video/VideoSelectActivity.java`

First screen of the video compression flow.

Responsibility:

- loads actual device videos
- supports multi-select
- supports per-item actions like play/open/share/delete
- writes chosen videos into `VideoCompressionSessionStore`
- opens `VideoSetupActivity`

### `ui/video/VideoSetupActivity.java`

Compression configuration screen.

Responsibility:

- shows selected video summary
- offers presets: Quick, Balanced, Max
- offers expandable advanced options card
- resolves the final compression request
- starts `VideoCompressingActivity`

### `ui/video/VideoCompressingActivity.java`

Live progress screen for compression.

Responsibility:

- listens to `VideoCompressionSessionStore`
- renders progress, stage, filename, and estimated time
- routes to `VideoResultActivity` when done
- allows cancel or simple backgrounding

### `ui/video/VideoResultActivity.java`

Final result screen for compression.

Responsibility:

- shows preview and size savings
- supports play/share actions
- supports restarting the flow with “Compress More”

## `utils` Package

### `EdgeToEdgeHelper.java`

Utility for system bar and inset handling.

### `FormatUtils.java`

Formatting helper.

Responsibility:

- formats storage sizes
- formats durations
- formats percentages

### `ImageHashUtils.java`

Image hashing utility used by the photo cleanup flow.

Responsibility:

- computes SHA-256 hashes for exact duplicate detection
- computes perceptual hashes for similar image detection
- compares perceptual hashes

### `PermissionHelper.java`

Centralized permission utility.

Responsibility:

- returns required media permissions by Android version
- checks permission state
- checks legacy write permission for older Android versions
- opens app settings when needed

## `video` Package

This package contains the domain and engine layer for video compression.

### `ResolvedVideoCompressionSettings.java`

Represents the final output settings for one source video after all rules are applied.

Contains:

- output width
- output height
- target bitrate
- target frame rate
- target short side

### `VideoBitrateOption.java`

Enum for compression-strength bitrate behavior.

Current mapping:

- `LOW` = mild reduction
- `MEDIUM` = balanced reduction
- `HIGH` = aggressive reduction

### `VideoCompressionManager.java`

Core compression orchestrator.

Responsibility:

- processes the selected videos sequentially
- loads metadata
- resolves final output settings
- builds Media3 Transformer compositions
- exports MP4/H.264 video
- polls and emits progress
- saves results
- emits item and batch completion callbacks

This is the central engine of the video compression feature.

### `VideoCompressionPreset.java`

Enum for user-facing preset modes:

- `QUICK`
- `BALANCED`
- `MAX`

### `VideoCompressionProgress.java`

Model representing current compression progress.

Contains:

- current index in batch
- total count
- completed count
- failed count
- per-item progress
- overall progress
- estimated time remaining
- current filename
- current stage

### `VideoCompressionRequest.java`

User-selected compression request.

Contains:

- selected preset
- selected resolution option
- selected fps option
- selected bitrate option

### `VideoCompressionResult.java`

Final result model for one compressed video.

Contains:

- source name and URI
- input size
- output size
- duration
- output dimensions
- bitrate used
- fps used
- success/failure state
- error message if failed
- output URI/display name if successful

### `VideoCompressionSettingsResolver.java`

Rules engine that converts source metadata plus user choices into final compression settings.

Responsibility:

- prevents upscaling
- clamps FPS to source FPS
- estimates bitrate when source bitrate is unavailable
- applies bitrate reduction rules
- ensures output dimensions are encoder-safe

### `VideoCompressionStage.java`

Enum for progress stages:

- preparing
- extracting
- encoding
- muxing
- saving
- completed

### `VideoFpsOption.java`

Enum for target FPS choices.

### `VideoMetadata.java`

Richer metadata model used by the compression engine.

Contains:

- source URI
- display name
- source size
- duration
- width/height
- rotation
- bitrate
- frame rate

### `VideoResolutionOption.java`

Enum for target resolution choices.

## How the Layers Work Together

### Photo Cleanup

1. `HomeActivity` opens `ScanSetupActivity`.
2. `ScanSetupActivity` creates `ScanConfig`.
3. `ScanningActivity` calls `MediaRepository.scanImages(...)`.
4. `MediaRepository` returns `ScanResult`.
5. `ScanSessionStore` keeps that result and the user selection state.
6. `ScanResultsActivity` renders groups through `ResultGroupAdapter` and `ThumbnailAdapter`.
7. `DeleteConfirmActivity` deletes selected items and stores a `CleanupOutcome`.
8. `CleanupSuccessActivity` displays the result.

### Video Compression

1. `HomeActivity` opens `VideoSelectActivity`.
2. `VideoSelectActivity` loads device videos via `VideoMediaRepository`.
3. Selected videos are stored in `VideoCompressionSessionStore`.
4. `VideoSetupActivity` builds a `VideoCompressionRequest`.
5. `VideoCompressionSessionStore` starts `VideoCompressionManager`.
6. `VideoCompressionManager` loads `VideoMetadata`, resolves settings, compresses, and emits progress.
7. `VideoCompressingActivity` listens to session updates and renders progress.
8. `VideoResultActivity` shows saved output info and follow-up actions.

## Current Design Characteristics

The app favors:

- simple Java classes over heavy abstraction
- direct Activity navigation over a navigation framework
- singleton session stores over database-backed transient state
- repositories for system access
- plain models for feature data

This makes the codebase relatively easy to follow for MVP development, though it also means more logic lives directly in activities than in a more layered enterprise architecture.

## Summary

If you want a quick mental model of the app:

- `ui` = screens
- `repository` = Android system/data access
- `data` = photo cleanup models
- `video` = video compression models and engine
- `adapters` = RecyclerView rendering
- `utils` = helper functions

And the two key state hubs are:

- `ScanSessionStore` for the photo cleanup flow
- `VideoCompressionSessionStore` for the video compression flow
