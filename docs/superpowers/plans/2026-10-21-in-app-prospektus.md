# In-App Prospektüs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the nonworking external “Aç” action with a polished market selector that opens each retailer’s live official flyer page inside the Android app.

**Architecture:** Keep the existing Kotlin/Jetpack Compose offers feature. Display branded market cards and host the selected official HTTPS page in a full-screen Android WebView, with loading/error/retry and in-reader back navigation. Do not scrape, cache, reproduce, or redistribute retailer flyers; show their official live pages.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android WebView/AndroidView; existing JUnit 4 tests. No new dependencies.

**Spec:** User approved the clickable prospectus mockup and requested real online flyers rendered in-app instead of opening another app/browser. Target the five existing markets: REWE, EDEKA, ALDI SÜD, Lidl, PENNY.

## Global Constraints

- Tapping a market opens the live page inside the app, with no external browser Intent.
- Keep the existing five official retailer URLs unless verification shows a broken address.
- Only HTTPS top-level navigation is allowed in the in-app viewer; reject unsafe schemes.
- No scraped/rehosted flyer assets; offline and failed pages must show actionable UI.
- Keep existing app theme, Kotlin/JVM compatibility, and dependency set.

## Review Focus

- BlueStacks/device has no external browser: retailer cards must still open in-app.
- Slow/no network: show progress then a retryable error rather than a blank screen.
- Retailer redirects: remain within WebView for HTTPS pages and block unsafe schemes.
- Android back: navigate WebView history first, then dismiss reader.
- WebView disposal: destroy the view when its reader closes.

---

### Task 1: HTTPS Navigation Policy

**Files:**
- Create: `app/src/main/java/com/shoppingapp/feature/offers/OfferNavigationPolicy.kt`
- Create: `app/src/test/java/com/shoppingapp/feature/offers/OfferNavigationPolicyTest.kt`

**Interfaces:** `OfferNavigationPolicy.allowsInAppNavigation(rawUrl: String): Boolean` accepts only parseable HTTPS URLs with a host.

- [x] Write unit tests for official HTTPS pages and reject HTTP, `intent:`, `javascript:`, and malformed URLs.
- [x] Verify RED, then implement the URL policy and verify GREEN.

### Task 2: In-App Offers UI

**Files:**
- Modify: `app/src/main/java/com/shoppingapp/feature/offers/OffersScreen.kt`
- Modify: `app/src/main/java/com/shoppingapp/feature/offers/OfficialOfferLinks.kt`
- Modify: `app/src/main/AndroidManifest.xml` only if inspection/build requires additional permission.

- [x] Replace external ACTION_VIEW buttons with five selectable branded cards.
- [x] Add a full-screen Compose dialog hosting WebView, progress/error/retry, web history/back, and close actions.
- [x] Handle only approved HTTPS navigation in WebView; never launch external browser apps.

### Task 3: Verification and Delivery

- [x] Run the complete debug unit test suite and signed release build.
- [x] Installed and launched signed release on the connected SM-S928B. Interactive WebView validation remains limited: the phone display was dozing/locked during capture, and the emulator transport was unreliable.
- [ ] Commit and push the app changes, increment the release version, and publish a signed GitHub APK so the phone can install it.
