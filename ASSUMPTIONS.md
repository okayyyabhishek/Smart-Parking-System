# Project Assumptions (Smart Parking Allocation System)

The following baseline assumptions are established per `00_MASTER_PROMPT.md` and `04_TRD.md` where the source academic PDF was silent:

| # | Assumption | Where Used | Description |
|---|---|---|---|
| A1 | Priority levels: `EMERGENCY(3) > HIGH(2) > NORMAL(1)` | TRD §4, `Priority.java`, `WaitingVehiclePriorityQueue.java` | Priority affects only waiting queue order, never compatibility. Accessible vehicles default to `HIGH`. |
| A2 | Compatibility & preference matrix | TRD §5, `ParkingAllocationManager.java` | `CAR` -> NORMAL, LARGE<br>`EV` -> EV, NORMAL<br>`SUV` -> LARGE only<br>`ACCESSIBLE` -> ACCESSIBLE, NORMAL. Incompatible slot types are never assigned. |
| A3 | Slot zone & floor for demo slots A-01..A-05 | `DataSeeder.java` | Zone `A`, Floor `0`. |
| A4 | Waiting queue rebuild at startup | `WaitingQueueService.java` | PriorityQueue lives in memory and is initialized/rebuilt from `ParkingSession` records where `status = WAITING` ordered by `enqueuedAt`. |
| A5 | Vehicle types: `CAR`, `EV`, `SUV`, `ACCESSIBLE` | TRD §2, `VehicleType.java` | Controlled enum set for vehicles entering the facility. |
| A6 | Authentication model | `SecurityConfig.java` | Spring Security session cookie (`JSESSIONID`, `HttpOnly`, `SameSite=Strict`), CSRF protection, BCrypt password hashing. Seed credentials loaded from environment variables/dev profile. |
