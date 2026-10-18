<!--
  Smart Parking Allocation System
  Repository: https://github.com/okayyyabhishek/Smart-Parking-System
-->

<div align="center">

  <h1>🅿️ Smart Parking Allocation System</h1>
  <p><strong>Intelligent bay allocation. Priority-based queues. Automatic reallocation.</strong></p>
  <p>A role-separated parking management application built with Java 21 and Spring Boot, with core Data Structures & Algorithms driving the allocation workflow.</p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.3.2" />
    <img src="https://img.shields.io/badge/Spring%20Security-RBAC-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
    <img src="https://img.shields.io/badge/Database-H2-4479A1?style=for-the-badge&logo=databricks&logoColor=white" alt="H2 Database" />
    <img src="https://img.shields.io/badge/Frontend-Vanilla%20JS-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="Vanilla JavaScript" />
  </p>

  <p>
    <a href="https://github.com/okayyyabhishek/Smart-Parking-System"><strong>Explore Repository</strong></a>
    ·
    <a href="https://github.com/okayyyabhishek/Smart-Parking-System/issues">Report a Bug</a>
    ·
    <a href="https://github.com/okayyyabhishek/Smart-Parking-System/issues">Request a Feature</a>
  </p>

</div>

---

## 🚦 Overview

**Smart Parking Allocation System** streamlines parking operations through server-side allocation rules and classic data structures. Instead of letting users choose any bay, the system evaluates vehicle compatibility, ranks available bays, and assigns the best eligible slot. When no compatible bay is available, the vehicle is placed in a priority-aware waiting queue.

The interface is designed around a road-signage-inspired visual language with a dark road palette, warm curb tones, and signal yellow accents.

### What makes it useful?

- **Automatic bay allocation** — compatible available bays are selected by the allocation engine.
- **Compatibility before priority** — priority never overrides physical bay suitability.
- **Priority-aware waiting queue** — emergency/high/normal priority ordering, with FIFO ordering for vehicles at the same priority.
- **Automatic reallocation** — when a vehicle exits, the engine searches the queue and allocates the released bay to the first compatible waiting vehicle.
- **Role-separated portals** — dedicated views for Super Admin, Operator, and Viewer.
- **Privacy-aware responses** — role-specific DTOs and masked vehicle details in restricted/public views.
- **Audit visibility** — administrative audit events and CSV export.
- **DSA-focused architecture** — HashMap, PriorityQueue, deque-based stack/queue, filtering, and comparators are used for clear algorithmic behavior.

> **Core principle:** A vehicle never chooses its own slot. The server validates compatibility and owns the allocation decision.

---

## 🖼️ Interface Preview

The application includes a secure role-based sign-in screen and a Super Admin dashboard for facility monitoring and parking operations.

<div align="center">
  <img src="screenshots/Screenshot%202026-10-18%20095556.png" alt="Smart Parking login interface" width="90%" />
  <br />
  <em>Secure sign-in for Super Admin, Operator, and Viewer roles.</em>
</div>

<br />

<div align="center">
  <img src="screenshots/Screenshot%202026-10-18%20092756.png" alt="Smart Parking administration dashboard" width="90%" />
  <br />
  <em>Super Admin dashboard showing bay availability, occupancy, waiting queue, and facility actions.</em>
</div>
---

## ✨ Feature Highlights

| Feature | How it works |
|---|---|
| 🅿️ Smart allocation | Filters bays by vehicle compatibility before ranking candidate slots. |
| 🚙 Vehicle compatibility | Supports NORMAL, EV, LARGE, and ACCESSIBLE bay preferences and restrictions. |
| ⏳ Waiting queue | Uses a priority queue with FIFO sequencing among vehicles of equal priority. |
| 🔁 Automatic reallocation | A released bay can be assigned to the first compatible waiting vehicle. |
| 📊 Operations dashboard | Summarizes bay availability, occupancy, waiting vehicles, and traffic counters. |
| 🧑‍💼 Role-based access | Separates Super Admin, Operator, and Viewer access. |
| 🔐 Privacy controls | Masks sensitive vehicle details in restricted views and uses role-specific DTOs. |
| 🧾 Session history | Captures parking entry/exit information and maintains recent-exit history. |
| 🛠️ Maintenance mode | Prevents bays marked for maintenance from being assigned. |
| 📋 Audit log | Records key operations; administrators can export audit data as CSV. |
| 🌗 UI theme | Road-inspired interface with a high-contrast yellow accent palette. |

---

## 👥 Access Model

The application separates access by operational responsibility.

| Role | Intended use | Access area |
|---|---|---|
| **SUPER_ADMIN** | Facility oversight, bay and user management, audit logs, maintenance | `/admin/**` |
| **OPERATOR** | Gate entry/exit, bay operations, waiting queue | `/operator/**` |
| **VIEWER** | Read-only bay and status information | `/viewer/**` |

Security protections documented by the project include role-specific controllers and DTOs, masked vehicle/contact details in restricted views, session expiry, failed-login lockout, and structured validation errors.

> The demo credentials below are for local development only. Change or disable seeded credentials before exposing the application to any network.

---

## 🧠 Data Structures & Algorithms

DSA is part of the system's core behavior, not just documentation.

| Data structure / algorithm | Use case | Typical complexity |
|---|---|---|
| **HashMap** | Vehicle lookup and duplicate detection by normalized plate | Average lookup: **O(1)** |
| **PriorityQueue (binary heap)** | Priority-based waiting queue | Enqueue / poll: **O(log n)**; peek: **O(1)** |
| **ArrayDeque as Stack** | Recent exit history using LIFO behavior | Push / pop: **O(1)** |
| **ArrayDeque as Queue** | FIFO sequence for equal-priority queue demonstrations | Add / remove at ends: **O(1)** |
| **Linear search** | Slot-search DSA demonstration | Search: **O(n)** |
| **Compatibility filtering + comparator** | Filters valid bays and ranks candidates | Filter: **O(n)**; sorting candidates: **O(m log m)** |

### Vehicle-to-bay compatibility

- **CAR:** prefers a NORMAL bay; may fall back to LARGE.
- **EV:** prefers an EV charging bay; may fall back to NORMAL.
- **SUV:** requires a LARGE bay.
- **ACCESSIBLE:** prefers an ACCESSIBLE bay; may fall back to NORMAL.
- **MAINTENANCE / OCCUPIED:** never eligible for a new allocation.

### Allocation lifecycle

```text
Vehicle arrives
     │
     ▼
Normalize & validate vehicle details
     │
     ▼
Find available, compatible bays
     │
     ├── Compatible bay exists ──► Rank candidates ──► Allocate bay
     │
     └── No compatible bay ──────► Add to priority queue
                                               │
                                               ▼
Vehicle exits ◄── Session closes ◄── Compatible waiting vehicle receives released bay
```

### Automatic reallocation

When a vehicle exits, the application closes the parking session, records the exit, releases the bay, and checks waiting vehicles for compatibility. An incompatible queue entry is not supposed to be assigned to an unsuitable bay; the engine searches for an eligible vehicle and preserves deferred queue entries.

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend framework | Spring Boot 3.3.2 |
| Persistence | Spring Data JPA |
| Authentication / authorization | Spring Security |
| Database | H2 |
| Frontend | HTML5, CSS3, JavaScript ES Modules |
| Build tool | Apache Maven / Maven Wrapper |
| Testing | JUnit-based automated test suites |

---

## 🚀 Getting Started

### Prerequisites

- JDK **21+**
- Apache Maven **3.8+**, or use the included Maven Wrapper if available
- A modern web browser

### 1. Clone the repository

```bash
git clone https://github.com/okayyyabhishek/Smart-Parking-System.git
cd Smart-Parking-System
```

### 2. Run the test/build lifecycle

With Maven installed:

```bash
mvn clean verify
```

If the repository contains a Maven Wrapper, use one of these commands instead:

**Windows**
```powershell
.\mvnw.cmd clean verify
```

**macOS / Linux**
```bash
./mvnw clean verify
```

### 3. Start the application

```bash
mvn spring-boot:run
```

Or use the wrapper:

**Windows**
```powershell
.\mvnw.cmd spring-boot:run
```

**macOS / Linux**
```bash
./mvnw spring-boot:run
```

### 4. Open the application

When the application starts successfully, open:

| Page | Local URL |
|---|---|
| Login | [http://localhost:8080/login.html](http://localhost:8080/login.html) |
| Super Admin | [http://localhost:8080/admin/index.html](http://localhost:8080/admin/index.html) |
| Operator | [http://localhost:8080/operator/index.html](http://localhost:8080/operator/index.html) |
| Viewer | [http://localhost:8080/viewer/index.html](http://localhost:8080/viewer/index.html) |

If your local configuration uses a different port or context path, use the address shown in the startup logs.

---

## 🔑 Demo Accounts

The repository documentation describes the following seeded accounts for development/test use:

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@Pass1234` | SUPER_ADMIN |
| `operator` | `Operator@Pass1234` | OPERATOR |
| `viewer` | `Viewer@Pass1234` | VIEWER |

Seed data is profile-dependent. If these accounts are unavailable, check the project's `DataSeeder.java` and active Spring profile.

**Security note:** Do not use these example credentials in a deployed environment. Replace demo credentials, review profile settings, and configure production-grade secrets and persistence before deployment.

---

## 🧪 Suggested End-to-End Demo

1. Sign in as **Operator** and register a vehicle with a supported type.
2. Confirm the system selects a compatible bay rather than allowing manual bay selection.
3. Fill the compatible bay capacity and submit another vehicle to show the waiting queue.
4. Add a higher-priority vehicle and verify the queue order.
5. Process a vehicle exit and check whether a compatible waiting vehicle receives the released bay.
6. Sign in as **Viewer** and verify that the public-facing information is read-only and sensitive details are masked.
7. Sign in as **Super Admin** to review bay maintenance controls and audit records.

---

## ✅ Testing

Run the test suite before changing allocation or access-control behavior:

```bash
mvn clean verify
```

The existing project documentation describes automated coverage for allocation compatibility, priority ordering, waiting-queue behavior, vehicle exits, automatic reallocation, duplicate and malformed plates, statistics, maintenance exclusions, RBAC restrictions, masking, login lockout, and audit entries.

Always use the test output from your current checkout as the source of truth for the actual number of passing tests.

---

## ⚠️ Scope & Limitations

The current project documentation identifies these boundaries:

- **No connected parking sensors:** entry and exit are initiated through the web interface/API; physical IoT sensors are not integrated.
- **No ANPR/CCTV pipeline:** vehicle plates are entered by an operator rather than recognized automatically.
- **No payment gateway:** fee collection and online payment integration are outside the current scope.
- **H2 database:** suitable for local evaluation; a production deployment should assess a persistent production database such as PostgreSQL.
- **No enterprise SSO:** authentication uses the application's configured Spring Security flow rather than an enterprise identity provider.

---

## 🗺️ Potential Next Steps

- Connect real occupancy sensors or gate hardware.
- Add parking tariffs, invoices, and payment integration.
- Add camera-based number-plate recognition with explicit privacy safeguards.
- Introduce PostgreSQL migrations, backups, and deployment profiles.
- Add CI workflows for automated build and test validation.
- Add monitoring, structured operational metrics, and deployment documentation.

These are possible future enhancements, not claims about current functionality.

---

## 🤝 Contributing

Contributions, bug reports, and feature suggestions are welcome.

1. Fork the repository.
2. Create a focused branch: `git checkout -b feature/your-feature`
3. Make your change and run `mvn clean verify`.
4. Commit with a descriptive message.
5. Open a pull request describing the change and test results.

For bugs or suggestions, use [GitHub Issues](https://github.com/okayyyabhishek/Smart-Parking-System/issues).

---

## 📄 License

No license information was confirmed from the current README. Add a `LICENSE` file and update this section before granting reuse rights.

---

<div align="center">

  <strong>Built around a simple rule: compatibility first, priority second.</strong>

  <br />
  <sub>Smart Parking Allocation System · Java 21 · Spring Boot · Data Structures & Algorithms</sub>

</div>
