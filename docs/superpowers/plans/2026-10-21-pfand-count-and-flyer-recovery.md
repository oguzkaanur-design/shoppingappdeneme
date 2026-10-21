# Pfand Count Input and Flyer Reader Recovery Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user add many Pfand bottles/cans by typing a count and make the in-app official flyer screen explain/recover from unsupported or blocked retailer pages.

**Architecture:** Keep count entry in the existing Compose Pfand card and persist via the existing quantity field. Keep official retailer pages in WebView; improve diagnostics and launch/redirect behavior without scraping undocumented endpoints or rehosting flyer assets.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android WebView, existing JUnit 4.

**Spec:** User explicitly requests direct numeric Pfand count input (e.g. 35 cans) instead of repeatedly tapping plus. User reports the in-app flyer screen is blank and wants a different approach to loading offers directly from retailer sources.

## Global Constraints

- Pfand quantity accepts positive whole numbers, max 9999, and total reflects quantity × unit price.
- No data schema migration; existing PfandEntry.quantity is already persisted.
- Do not use undocumented retailer APIs, defeat access controls, or redistribute flyer content.
- Preserve official retailer source pages and show a visible actionable state if WebView cannot display them.

## Review Focus

- Empty, zero, decimal, or too-large count: do not add a record.
- Editing quantity after preset changes: maintain entered count and recalculate total.
- WebView HTTP, SSL, renderer, timeout or popup behavior: show actionable explanation, never a blank panel.
- Retailer availability: don't promise direct PDF when no public PDF/feed is verified.
- Installation/update: release signature remains the existing keystore and versionCode increases.

---

### Task 1: Direct Pfand Count

**Files:**
- Create `app/src/main/java/com/shoppingapp/feature/stats/PfandQuantity.kt`
- Modify `app/src/main/java/com/shoppingapp/feature/stats/PfandScreen.kt`
- Create `app/src/test/java/com/shoppingapp/feature/stats/PfandQuantityTest.kt`

**Interface:** `PfandQuantity.parse(raw: String): Int?`; returns a count only for `1..9999`.

- [x] Write parsing tests and observe unresolved-reference RED.
- [x] Implement parser, editable numeric field, live total and disable add for invalid values.
- [ ] Run focused then full unit tests.

### Task 2: Flyer Reader Recovery

**Files:**
- Modify `app/src/main/java/com/shoppingapp/feature/offers/OffersScreen.kt`
- Modify `app/src/main/java/com/shoppingapp/feature/offers/OfficialOfferLinks.kt` only if verified official destinations need correction.

- [ ] Verify the connected device and inspect WebView logs/errors.
- [ ] Support cookies, loading timeout, SSL/HTTP errors and renderer failure with a clear retry/fallback state.
- [ ] Keep HTTPS-only navigation and don't bypass access restrictions.

### Task 3: Ship

**Files:** `app/build.gradle.kts` plus the above.

- [ ] Build/tests/signature/manifest checks.
- [ ] Exercise count entry and at least one retailer in-app on device; record pages unavailable from retailer response.
- [ ] Increment version, push main, tag, and verify GitHub release APK.
