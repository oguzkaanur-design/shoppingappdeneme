# Gelişmiş Kişisel Market & Alışveriş Uygulaması Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a high-performance, offline-first Android shopping list application focused on the Munich market, featuring barcode scanning, price tracking, and pantry management.

**Architecture:** MVVM architecture with Room Database (offline-first), Jetpack Compose for UI, and CameraX with Google ML Kit for barcode integration.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Room, Hilt, CameraX, Retrofit.

**Spec:** `docs/superpowers/specs/2026-10-16-shopping-app-design.md`

## Global Constraints
- Platform: Android (minSDK 26)
- Language: Kotlin
- Architecture: MVVM with Hilt
- Data: Local-only, Room DB
- Currency: Euro (€)
- Language: Bilingual (DE/TR)

## Review Focus
1. **Barcode/API Failure:** Product not found in Open Food Facts. *Expectation: Prompt manual input.*
2. **Invalid Price Input:** Non-numeric entry or missing mandatory price. *Expectation: Input validation error.*
3. **Empty List State:** User view before any item is added. *Expectation: Helpful "Empty state" visual.*
4. **Data Sync Conflict:** (If feature expanded, but currently offline-only). *Expectation: Data integrity in Room.*
5. **UI Scaling:** Large font/accessibility settings. *Expectation: Responsive layout with Compose.*

---

### Task 1: Project Setup & Core Dependencies

**Files:**
- Modify: `build.gradle.kts` (project & app)
- Modify: `settings.gradle.kts`
- Modify: `AndroidManifest.xml` (permissions)

**Interfaces:**
- Produces: Base project structure with Hilt, Compose, and Room dependencies.

- [ ] **Step 1: Add dependencies to build.gradle.kts**
- [ ] **Step 2: Add Internet and Camera permissions to AndroidManifest.xml**
- [ ] **Step 3: Setup Hilt Application class**
- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts settings.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/shoppingapp/App.kt
git commit -m "feat: setup project infrastructure"
```

### Task 2: Core Data Layer (Room DB)

**Files:**
- Create: `app/src/main/java/com/shoppingapp/data/local/ShoppingDatabase.kt`
- Create: `app/src/main/java/com/shoppingapp/data/model/Entities.kt`
- Create: `app/src/main/java/com/shoppingapp/data/dao/ShoppingDao.kt`

**Interfaces:**
- Produces: DAO interface for accessing local data.

- [ ] **Step 1: Define Room entities (Product, Store, ListItem, etc.)**
- [ ] **Step 2: Create DAO with Room queries**
- [ ] **Step 3: Setup Database class**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/shoppingapp/data/
git commit -m "feat: setup database layer"
```

### Task 3: Camera & Barkod Scanning

**Files:**
- Create: `app/src/main/java/com/shoppingapp/feature/scanner/BarcodeScanner.kt`
- Create: `app/src/main/java/com/shoppingapp/feature/scanner/ScannerViewModel.kt`

**Interfaces:**
- Consumes: Google ML Kit
- Produces: `ScannerViewModel` emitting scanned barcode results.

- [ ] **Step 1: Implement CameraX preview using Compose**
- [ ] **Step 2: Integrate ML Kit barcode scanner analyzer**
- [ ] **Step 3: Link scanner results to ViewModel**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/shoppingapp/feature/scanner/
git commit -m "feat: implement barcode scanner"
```

### Task 4: UI & Navigation (Main Screen)

**Files:**
- Create: `app/src/main/java/com/shoppingapp/MainActivity.kt`
- Create: `app/src/main/java/com/shoppingapp/ui/ShoppingScreen.kt`

**Interfaces:**
- Consumes: ShoppingDao
- Produces: Main grid-based UI showing active list.

- [ ] **Step 1: Setup Jetpack Compose navigation**
- [ ] **Step 2: Implement Main Grid UI**
- [ ] **Step 3: Implement category filtering logic**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/shoppingapp/MainActivity.kt app/src/main/java/com/shoppingapp/ui/
git commit -m "feat: implement main UI and navigation"
```

### Task 5: Statistics & Pfand Integration

**Files:**
- Create: `app/src/main/java/com/shoppingapp/feature/stats/StatsViewModel.kt`
- Create: `app/src/main/java/com/shoppingapp/ui/components/PfandCounter.kt`

**Interfaces:**
- Consumes: PurchaseHistory DAO
- Produces: Statistical reports and Pfand balance.

- [ ] **Step 1: Create stats aggregation queries**
- [ ] **Step 2: Design and implement Pfand counter component**
- [ ] **Step 3: Display Euro stats**
- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/shoppingapp/feature/stats/
git commit -m "feat: add stats and Pfand support"
```
