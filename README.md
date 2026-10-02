# Smart Parking Allocation System

> **Course:** Data Structure and Algorithms (Summer Training, Board Infinity)  
> **Student:** Suhani Parashar  
> **Technology Stack:** Java 21, Spring Boot 3.3.2, Spring Data JPA, Spring Security, H2 Database, Vanilla HTML5/CSS3/ES Modules.  
> **Core Principle:** *"A vehicle never chooses its own slot."* Algorithmic server-side validation, allocation, queuing, and automatic reallocation.

---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Role-Separated Architecture & Security Hierarchy](#2-role-separated-architecture--security-hierarchy)
3. [Data Structures & Algorithms (DSA) Map (Viva Defense)](#3-data-structures--algorithms-dsa-map-viva-defense)
4. [Central Smart Feature: Automatic Reallocation](#4-central-smart-feature-automatic-reallocation)
5. [Setup & Running Instructions](#5-setup--running-instructions)
6. [Seed Accounts & Credentials](#6-seed-accounts--credentials)
7. [Step-by-Step Viva / Demo Walkthrough](#7-step-by-step-viva--demo-walkthrough)
8. [QA Test Case Verification Matrix (Real Results)](#8-qa-test-case-verification-matrix-real-results)
9. [Known Limitations](#9-known-limitations)
10. [Viva Defense Q&A Cheat Sheet](#10-viva-defense-qa-cheat-sheet)

---

## 1. Project Overview

The **Smart Parking Allocation System** is an algorithmic, production-hardened local web application that automates parking lot operations with mathematical guarantees. The facility enforces strict server-side rules:

- Drivers and operators **never pick parking bays manually**.
- Compatibility is verified **before priority**. A high-priority vehicle is never placed in an incompatible bay (e.g., an SUV is never assigned to a small compact slot).
- If no compatible bay is free, the vehicle enters an in-memory **PriorityQueue** ordered by priority level (`EMERGENCY > HIGH > NORMAL`) and FIFO arrival sequence.
- When an occupied bay is vacated, the engine performs **Automatic Reallocation** by popping the highest-priority compatible vehicle from the waiting queue and assigning the slot immediately.
- The UI follows a bespoke, accessible design system inspired by highway signage and road markings (Highway Gothic typography, slate road surfaces `#121417`, curb cream `#F3F0E8`, and road marking yellow `#F2B705`) with zero external frameworks and zero AI fonts.

---

## 2. Role-Separated Architecture & Security Hierarchy

To guarantee data privacy (PII protection) and operational control, the system implements a strict three-tier role hierarchy:

```
┌─────────────────────────────────────────────────────────────┐
│                 SUPER_ADMIN (Main Control)                  │
│  /admin/**  — Full Control, Bays, Users, Audit Logs (CSV)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    OPERATOR (Gate Desk)                     │
│  /operator/** — Entry/Exit Gate, Bays, Masked Contact DTOs  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                     VIEWER (Public Board)                   │
│  /viewer/** — Read-Only Live Bays, Zero PII (Masked Plates) │
└─────────────────────────────────────────────────────────────┘
```

### Security & Hardening Controls
- **Role Isolation:** Each role has dedicated controllers, services, URL prefixes, and **distinct DTO classes**. Entities are never exposed directly to clients.
- **Zero-PII Public Board:** Viewer APIs and UI never return `ownerName`, `contact`, or raw license plates (`PB10••1234` format only).
- **Operator Masking:** Operator endpoints display masked contacts (`••••••3210`) preserving only the last 4 digits.
- **Brute-Force Lockout:** Any account with 5 consecutive failed login attempts is automatically locked for 5 minutes (`HTTP 423 Locked`).
- **Session Expiry & Idempotency:** Spring Security session cookie (`JSESSIONID`, `HttpOnly`, `SameSite=Strict`) expires after 30 minutes of inactivity. Frontend displays a clean session-expired notification banner on `/login.html?expired=true`.
- **Friendly Validation:** No Java exception stack traces ever reach the client (`GlobalExceptionHandler` transforms exceptions into structured JSON error envelopes with code and field errors).
- **Custom Error Pages:** Dedicated `/403.html` (Restricted Area) and `/404.html` (Route Not Found) styled with highway signage aesthetics.

---

## 3. Data Structures & Algorithms (DSA) Map (Viva Defense)

All core data structures reside in the isolated package `com.smartparking.ds`, completely decoupled from JPA entities and Spring Web annotations.

| Data Structure / Algorithm | Class Name | Time Complexity | Space Complexity | Where Used in UI & System | Viva Rationale / Justification |
|---|---|---|---|---|---|
| **HashMap** | `VehicleLookupManager` | Search: **O(1)**<br>Insert: **O(1)** | **O(v)** | Operator vehicle lookup, search bar, duplicate detection | Instant O(1) lookup by normalized alphanumeric license plate (`PB10AB1234`), avoiding expensive full table scans. |
| **PriorityQueue** (Binary Heap) | `WaitingVehiclePriorityQueue` | Enqueue: **O(log k)**<br>Peek: **O(1)**<br>Poll: **O(log k)** | **O(k)** | Waiting bay queue display, Gate allocation fallback | Enforces priority dispatching (`EMERGENCY > HIGH > NORMAL`) with FIFO fallback for equal priority vehicles using a custom `Comparator`. |
| **ArrayDeque as Stack** (LIFO) | `ParkingHistoryStack` | Push: **O(1)**<br>Pop: **O(1)**<br>Snapshot: **O(s)** | **O(s)** (Capped at 50) | Operator "Recent Exits" drawer, Dashboard quick view | Demonstrates Last-In-First-Out behavior: the vehicle that exited most recently is immediately visible at the top of the history feed. |
| **Linear Search** | `SlotSearchManager` | Find: **O(n)** | **O(1)** | Slot lookup demo in `ds/` package | Deliberately included to contrast theoretical **O(n)** linear scanning against HashMap **O(1)** direct indexing during viva examination. |
| **Custom Comparator** & Filtering | `ParkingAllocationManager` + `SlotRankComparator` | Filter: **O(n)**<br>Sort: **O(m log m)** | **O(m)** | Gate Entry automatic bay assignment | Implements the compatibility matrix and ranks compatible slots by preference (exact type > compatible fallback > lowest slot number). |
| **ArrayDeque as Queue** (FIFO) | `EqualPriorityFifoQueueDemoTest` | Enqueue: **O(1)**<br>Dequeue: **O(1)** | **O(q)** | Core DSA demonstration suite | Proves fair first-come, first-served queue semantics when vehicles share identical priority levels. |

### Vehicle-to-Slot Compatibility Matrix
1. **CAR:** Prefers `NORMAL`; falls back to `LARGE`.
2. **EV:** Prefers `EV` (charging bay); falls back to `NORMAL`.
3. **SUV:** Allowed **only** in `LARGE` (physical clearance requirement).
4. **ACCESSIBLE:** Prefers `ACCESSIBLE` (wide aisle, ramp proximity); falls back to `NORMAL`.
5. **MAINTENANCE / OCCUPIED:** **Never** assigned under any circumstance.

---

## 4. Central Smart Feature: Automatic Reallocation

When a vehicle exits the facility, the released slot does not simply sit idle:

```
[Vehicle Exits] ──► [Release Bay A-04 (LARGE)]
                           │
                           ▼
             [Inspect Waiting PriorityQueue]
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
[Head is Incompatible (CAR)]    [Head is Compatible (SUV)]
             │                           │
  [Temporarily Defer Head]               ▼
             │               [Poll SUV & Assign Bay A-04]
             ▼                           │
   [Find Next Compatible]                ▼
             │               [Update Session to ACTIVE]
             ▼                           │
[Re-insert Deferred Entries]             ▼
                             [Bay Marked OCCUPIED]
```

1. The exiting vehicle closes its active `ParkingSession`, records the exit timestamp, computes parking duration, and pushes an `ExitRecord` onto `ParkingHistoryStack`.
2. `waitingQueueService.pollCompatible(releasedSlotType)` traverses the waiting priority queue.
3. If an incompatible vehicle is at the head of the queue (e.g., a compact `CAR` waiting for a `NORMAL` bay while a `LARGE` bay is released), the algorithm **skips the incompatible vehicle without dropping it**, locates the next eligible vehicle (e.g. an `SUV`), allocates the slot, and restores the skipped vehicles in their exact original priority positions.
4. The waiting session transitions to `ACTIVE`, the bay is marked `OCCUPIED`, and an audit trail entry is persisted.

---

## 5. Setup & Running Instructions

### Prerequisites
- **Java Development Kit (JDK):** Version 21 or higher
- **Apache Maven:** Version 3.8+ (or use the included Maven wrapper)
- **Web Browser:** Any modern browser (Chrome, Edge, Firefox, Safari)

### 1. Build and Run Tests
Run the entire build lifecycle and execute all 92 automated tests:
```bash
mvn clean verify
```
*Expected result:* `BUILD SUCCESS`, 92 tests run, 0 failures, 0 errors.

### 2. Launch Application
Start the Spring Boot application using the default profile:
```bash
mvn spring-boot:run
```
*Port:* The application runs on `http://localhost:8080`.

### 3. Open in Browser
- Landing Page / Sign In: [http://localhost:8080](http://localhost:8080) or [http://localhost:8080/login.html](http://localhost:8080/login.html)
- Super Admin Portal: [http://localhost:8080/admin/index.html](http://localhost:8080/admin/index.html)
- Operator Gate Portal: [http://localhost:8080/operator/index.html](http://localhost:8080/operator/index.html)
- Viewer Public Board: [http://localhost:8080/viewer/index.html](http://localhost:8080/viewer/index.html)

---

## 6. Seed Accounts & Credentials

The application automatically seeds demonstration accounts on startup in `dev` and `test` profiles (via `DataSeeder.java`):

| Username | Password | Role | Permitted Access |
|---|---|---|---|
| `admin` | `Admin@Pass1234` | `SUPER_ADMIN` | `/admin/**`, `/operator/**`, `/viewer/**` (Full control) |
| `operator` | `Operator@Pass1234` | `OPERATOR` | `/operator/**`, `/viewer/**` (Gate Operations) |
| `viewer` | `Viewer@Pass1234` | `VIEWER` | `/viewer/**` (Public read-only display) |

### Baseline Demo Parking Bays
- `A-01` (NORMAL) — Zone A, Floor 0
- `A-02` (NORMAL) — Zone A, Floor 0
- `A-03` (EV) — Zone A, Floor 0 (EV Charging Station)
- `A-04` (LARGE) — Zone A, Floor 0 (High-clearance SUV Bay)
- `A-05` (ACCESSIBLE) — Zone A, Floor 0 (Accessible / Ramp Proximity)

---

## 7. Step-by-Step Viva / Demo Walkthrough

Follow this scripted 5-minute live demonstration for course instructors and examiners:

### Step 1: Sign in as Operator
1. Navigate to `http://localhost:8080/login.html`.
2. Sign in with `operator` / `Operator@Pass1234`.
3. Notice the **Operator Gate Desk** loads with 5 available bays, empty waiting queue, and Highway Gothic typography.

### Step 2: Park Vehicles & Demonstrate Compatibility Allocation
1. Click **Park Vehicle** (`/operator/entry.html`).
2. Enter: Plate `PB10AB1234`, Type `CAR`, Priority `NORMAL`. Click **Allocate Bay**.
   - Result: Assigned to `A-01` (Type: NORMAL).
3. Enter: Plate `PB10XY4567`, Type `EV`, Priority `NORMAL`. Click **Allocate Bay**.
   - Result: Assigned to `A-03` (Type: EV).
4. Enter: Plate `PB08CD9876`, Type `SUV`, Priority `NORMAL`. Click **Allocate Bay**.
   - Result: Assigned to `A-04` (Type: LARGE — the only compatible slot for SUV).

### Step 3: Trigger the Waiting Queue & Priority Jump
1. Attempt to enter another SUV: Plate `PB08CD2222`, Type `SUV`, Priority `NORMAL`.
   - Result: No available LARGE bay. System displays notification: *"No compatible AVAILABLE slot. Enqueued at position #1"*.
2. Now enter an emergency/priority SUV: Plate `PB08EM9999`, Type `SUV`, Priority `HIGH`.
   - Result: System adds `PB08EM9999` to `WaitingVehiclePriorityQueue`.
3. Check the **Waiting Queue** (`/operator/waiting.html`):
   - `PB08EM9999` (HIGH) has **jumped ahead** to Position #1!
   - `PB08CD2222` (NORMAL) is displaced to Position #2.

### Step 4: Demonstrate Central Smart Feature (Automatic Reallocation)
1. In the **Bays** view (`/operator/slots.html`), click slot `A-04` (occupied by `PB08CD9876`).
2. Click **Process Vehicle Exit**.
3. Observe the prompt and confirm exit.
4. **Immediate smart reallocation occurs:**
   - Slot `A-04` is vacated.
   - The system automatically polls `PB08EM9999` from the queue and reallocates `A-04` to it.
   - Waiting queue size drops by 1.
   - The Exit drawer updates with the completed session and duration.

### Step 5: Verify Privacy in the Viewer Portal
1. Open a new Incognito / Private window and log in as `viewer` / `Viewer@Pass1234`.
2. View the Live Bay Monitor (`/viewer/slots.html`) and History (`/viewer/history.html`).
3. Notice:
   - License plates are masked: `PB08••9999`.
   - Owner names and phone numbers are **completely absent** from both DOM and network JSON responses.

### Step 6: Super Admin Main Control
1. Sign in as `admin` / `Admin@Pass1234`.
2. Navigate to **Maintenance** (`/admin/slots.html`) and toggle Maintenance mode on slot `A-05`.
   - Result: Bay turns into Maintenance Orange barrier mode (`MAINTENANCE`) and will never be assigned.
3. Navigate to **Audit Logs** (`/admin/audit.html`):
   - Inspect logged events (`PARK_ENTRY`, `PARK_EXIT`, `PARK_REALLOCATED`, `SLOT_MAINTENANCE`).
   - Click **Export CSV** to download `audit_logs.csv`.

---

## 8. QA Test Case Verification Matrix (Real Results)

All test cases are verified automatically via `Phase8HardeningAndQaVerificationTest.java` and individual test suites. **No results are fabricated.**

| Test # | Test Case Description | Verified Behavior / Assertion | Status | Automated Test Method |
|---|---|---|---|---|
| **TC-01** | Add parking slot | Unique slot `Z-99` is saved; duplicate slot numbers throw exception. | **PASS** | `testCase01_addParkingSlot` |
| **TC-02** | Add vehicle | Vehicle accepted with normalized uppercase license plate (`dl 04 ab 9999` -> `DL04AB9999`). | **PASS** | `testCase02_addVehicle` |
| **TC-03** | Park normal vehicle | Normal `CAR` gets allocated to lowest compatible `NORMAL` bay (`A-01`). | **PASS** | `testCase03_parkNormalVehicle` |
| **TC-04** | Park EV | Electric vehicle prefers dedicated charging bay `A-03` (EV). | **PASS** | `testCase04_parkEV` |
| **TC-05** | Park large vehicle | SUV gets allocated to `A-04` (LARGE); never assigned to smaller bays. | **PASS** | `testCase05_parkLargeVehicle` |
| **TC-06** | Priority vehicle | High-priority vehicle jumps ahead of normal-priority vehicle in queue. | **PASS** | `testCase06_priorityOrdering` |
| **TC-07** | No compatible slot | When bays are full, vehicle enters waiting PriorityQueue with position #1. | **PASS** | `testCase07_noCompatibleSlotEntersWaitingQueue` |
| **TC-08** | Vehicle exit | Exit clears bay, marks it `AVAILABLE`, and calculates parking duration. | **PASS** | `testCase08_vehicleExit` |
| **TC-09** | Automatic reallocation | Vehicle exit releases bay and reallocates it immediately to eligible waiting vehicle. | **PASS** | `testCase09_automaticReallocation` |
| **TC-10** | Duplicate vehicle | Already parked vehicle receives friendly validation: *"Vehicle is already parked in slot..."*. | **PASS** | `testCase10_duplicateVehicleFriendlyMessage` |
| **TC-11** | Invalid vehicle plate | Malformed license plate `$$$` returns `HTTP 400` with `VALIDATION_FAILED` envelope. | **PASS** | `testCase11_invalidVehicleValidationError` |
| **TC-12** | Search vehicle | `VehicleLookupManager` `HashMap` finds vehicle in O(1) matching `pb10 ab-1234`. | **PASS** | `testCase12_searchVehicleLookup` |
| **TC-13** | Search slot | Linear scan `SlotSearchManager` accurately locates slot `A-03` in O(n). | **PASS** | `testCase13_searchSlotLinear` |
| **TC-14** | Sorting records | Database queries return parking slots sorted deterministically by slot number. | **PASS** | `testCase14_sortingSlotRecords` |
| **TC-15** | Parking duration | System computes non-negative duration in seconds from entry to exit timestamps. | **PASS** | `testCase15_parkingDurationCalculation` |
| **TC-16** | Real-time statistics | Dashboard counts (total, occupied, available, waiting) calculated from live DB data. | **PASS** | `testCase16_statisticsCalculatedFromDb` |
| **TC-17** | Maintenance slot | Slot in `MAINTENANCE` status is strictly excluded from allocation. | **PASS** | `testCase17_maintenanceSlotNeverAssigned` |
| **TC-18** | Viewer RBAC isolation | `VIEWER` calling `/api/admin/**` or `/api/operator/**` receives `HTTP 403 Forbidden`. | **PASS** | `testCase18_viewerCallsAdminOrOperatorApiReturns403` |
| **TC-19** | Viewer zero-PII scan | Viewer JSON responses contain 0 occurrences of `ownerName`, `contact`, or raw plates. | **PASS** | `testCase19_viewerJsonScanZeroPii` |
| **TC-20** | Operator contact masking | Operator endpoints mask contact numbers into `••••••3210` format. | **PASS** | `testCase20_operatorContactMasking` |
| **TC-21** | Anonymous access protection | Unauthenticated requests to protected endpoints return `HTTP 401 Unauthorized`. | **PASS** | `testCase21_anonymousAccessReturns401` |
| **TC-22** | Brute force lockout | 5 failed login attempts trigger `HTTP 423 Locked` for 5 minutes. | **PASS** | `testCase22_lockoutAfterFiveFailures` |
| **TC-23** | Comprehensive audit trail | Vehicle entry and exit record structured audit rows (`PARK_ENTRY`, `PARK_EXIT`). | **PASS** | `testCase23_auditEntryOnParkExit` |
| **TC-24** | Design system token check | Tokens CSS contains highway palette; banned generic fonts (Inter/Roboto) are absent. | **PASS** | `testCase24_themeTokensAndNoBannedFonts` |
| **TC-25** | Smart reallocation skip | Reallocation skips incompatible head of queue and assigns slot to first compatible vehicle. | **PASS** | `testCase25_reallocationSkipsIncompatibleHead` |

---

## 9. Known Limitations

In accordance with academic integrity and the course scope, the following boundaries are formally noted:

1. **Hardware & Sensors:** No physical IoT geomagnetic/ultrasonic barrier sensors or loop detectors are connected. Gate arrival and departure are triggered through the web interface or REST API.
2. **Payments & Tariffs:** Fee calculation and payment gateway integration (Razorpay/Stripe) are out of scope. The system tracks physical and temporal occupancy duration only.
3. **Computer Vision / ANPR:** Automatic Number Plate Recognition via CCTV cameras is not implemented; vehicle numbers are entered by the operator and normalized programmatically.
4. **Single-Node Embedded Database:** The system uses an in-memory/file-backed H2 database intended for local evaluation and defense. Production deployment would target PostgreSQL.
5. **No Enterprise SSO:** Authentication relies on internal BCrypt password hashes and session cookies rather than SAML/OAuth2 enterprise identity providers.

---

## 10. Viva Defense Q&A Cheat Sheet

Prepare for viva questions with these concise, mathematically sound responses:

### Q1: Why did you use a PriorityQueue instead of simply sorting an ArrayList or running a SQL ORDER BY query?
> **Answer:** Sorting an ArrayList or querying the database takes **O(k log k)** or triggers a disk I/O scan on every check. A `PriorityQueue` maintains heap order continuously. Enqueuing takes **O(log k)** and inspecting the highest-priority vehicle takes **O(1)**. When a slot opens, the dispatch decision is instantaneous.

### Q2: Why is vehicle lookup O(1) but slot search O(n)?
> **Answer:** Vehicle lookups happen frequently during gate entry to check for duplicate/active parking sessions. By hashing the normalized license plate into a `HashMap<String, VehicleSnapshot>`, we achieve **O(1)** average-time lookup. Conversely, slot search is kept linear **O(n)** in `SlotSearchManager` to demonstrate linear search algorithms as requested by the academic syllabus, while also fitting parking layouts where total bays (n) rarely exceed a few hundred.

### Q3: What happens if the server restarts while vehicles are in the waiting queue?
> **Answer:** We implemented **startup rebuild hardening** (`WaitingQueueService.rebuildFromDatabase()`). Although the PriorityQueue lives in memory for sub-millisecond execution, all waiting states are backed by the `ParkingSession` database table. On application boot, the system queries active waiting sessions ordered by `enqueuedAt` and repopulates the PriorityQueue.

### Q4: How do you prevent a high-priority vehicle from taking the wrong slot?
> **Answer:** The rule is **"Compatibility precedes priority"**. In `ParkingAllocationManager`, we first filter all available bays against the vehicle's compatibility matrix. Only compatible bays are ranked. If no compatible bay exists, the vehicle enters the waiting queue regardless of its priority.

### Q5: How is PII (Personally Identifiable Information) protected from public viewers?
> **Answer:** We use **role-specific Data Transfer Objects (DTOs)** and Jackson JSON serialization filtering. The `ViewerSlotDto`, `ViewerWaitingDto`, and `ViewerVehicleDto` do not contain `ownerName` or `contact` fields. Furthermore, license plate numbers are masked into `PB10••1234` format at the mapper level before serialization.
