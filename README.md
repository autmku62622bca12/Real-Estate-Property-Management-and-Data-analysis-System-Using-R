# Real Estate Property Management and Online Booking System

Full-stack MCA project: **HTML5 / CSS3 / JavaScript** frontend, **Java Spring Boot** backend, **MySQL** database, **R** data analysis. Developed and run in **Visual Studio Code**.

| Role | What they can do |
|------|------------------|
| **Admin** | Dashboard counts, manage customers / agents / properties / categories, verify properties, view site visits & enquiries, reports + R charts |
| **Agent** | Register (admin approval needed), add/edit/delete properties, upload room-wise + 360° images, reply to enquiries, approve / reject / reschedule site visits |
| **Customer** | Register, search & filter properties, favourites, enquiries, site-visit booking, virtual tour, notifications |

## 1. Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| JDK | 17 or newer | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| MySQL Server | 8.x | running on `localhost:3306` |
| R | 4.x (for analysis) | `Rscript --version` |
| VS Code extensions | Extension Pack for Java, Spring Boot Extension Pack, Lombok Annotations Support, R (REditorSupport) | see `.vscode/extensions.json` |

## 2. Database setup (MySQL)

Configured in `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/real_estate_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
spring.datasource.username=root
spring.datasource.password=12345
spring.jpa.hibernate.ddl-auto=update
```

* Make sure MySQL is running and the `root` password is `12345` (or change it in the file above).
* The database `real_estate_db` and **all tables are created automatically** by JPA/Hibernate on the first start.
* Prefer manual creation? Run `database/schema.sql` (`mysql -u root -p < database/schema.sql`).
* **Sample data** (users, 8 categories, 25 properties with images, enquiries, visits, favourites, notifications) is inserted automatically on the first run when the `users` table is empty (`app.seed.enabled=true`). Set it to `false` to start with an empty database.

## 3. Run the application in VS Code

1. `File → Open Folder…` and open the `real-estate-project` folder.
2. Open the VS Code terminal and run:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   (or press **Terminal → Run Task → Run backend (Spring Boot)**, or use the *Spring Boot Dashboard* ▶ button on `RealEstateApplication`).
3. Open **http://localhost:8080** in the browser.

> Always start the backend **from the `backend` folder**. Spring Boot serves the `frontend/` folder (`spring.web.resources.static-locations=file:../frontend/`), saved uploads in `uploads/` and R charts in `r-analysis/charts/`, so frontend and backend run on the same port - no CORS setup and no Node.js needed.

### Demo logins (sample data)

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@realestate.com` | `Admin@123` |
| Agent (approved) | `ravi.agent@realestate.com` (also `priya.agent@…`, `suresh.agent@…`) | `Agent@123` |
| Agent (pending approval) | `karthik.agent@realestate.com` | `Agent@123` |
| Customer | `arun@customer.com` (also `meena@`, `vignesh@`, `divya@`, `ismail@`, `sanjay@customer.com`) | `Customer@123` |

## 4. R analysis

R reads the same MySQL database and writes PNG charts to `r-analysis/charts/` (shown in **Admin → Dashboard** and **Admin → Reports & R Analysis**).

```bash
cd r-analysis
Rscript install_packages.R      # once: DBI, RMariaDB, dplyr, ggplot2, scales
Rscript property_analysis.R     # price analysis + property analysis (5 charts)
Rscript booking_analysis.R      # monthly visits/enquiries, popular locations/categories (4 charts)
# or both:  Rscript run_all.R
```

* DB settings are at the top of `r-analysis/db_config.R` (defaults: localhost, `real_estate_db`, root / 12345; override with env vars `DB_HOST DB_PORT DB_NAME DB_USER DB_PASSWORD`).
* CSV summaries are saved in `r-analysis/reports/`.
* **From the website:** Admin → *Reports & R Analysis* → **Run R Analysis** executes both scripts through `Rscript`. If `Rscript` is not on PATH, set the full path in `application.properties`, e.g. `app.rscript.path=C:/Program Files/R/R-4.3.2/bin/Rscript.exe`.
* Charts: property count by category (bar), average price by location (bar), sale vs rent (pie), BHK vs average price (bar), property availability (bar), monthly site visits (line), monthly enquiries (bar), popular locations, most viewed categories.

## 5. Project structure

```text
real-estate-project/
├── backend/                         Spring Boot (Maven)
│   ├── pom.xml
│   └── src/main/
│       ├── resources/application.properties
│       └── java/com/realestate/
│           ├── controller/   REST controllers (Auth, Public, Customer, Agent, Admin, Notification)
│           ├── service/      business logic (Property, Enquiry, SiteVisit, Notification, Report, R runner …)
│           ├── repository/   Spring Data JPA repositories
│           ├── entity/       JPA entities  -> MySQL tables
│           ├── dto/          request / response objects with validation
│           ├── config/       SecurityConfig, WebConfig, DataSeeder (sample data)
│           ├── security/     AppPrincipal, SecurityUtil
│           ├── exception/    ApiException + GlobalExceptionHandler (JSON error messages)
│           └── util/
├── frontend/                        plain HTML / CSS / JS (no framework)
│   ├── index, properties, property-details, about, contact, login, register .html
│   ├── admin/ agent/ customer/      dashboards (8 + 4 + 4 pages)
│   ├── css/style.css
│   └── js/  api.js (fetch helpers) · layout.js (navbar, sidebar, notifications) · propertyForm.js
├── r-analysis/                      property_analysis.R, booking_analysis.R, db_config.R, charts/
├── database/schema.sql              optional manual schema
├── uploads/                         uploaded images (created automatically)
└── README.md
```

Layered flow: `Controller → Service → Repository → Entity → MySQL`.

## 6. Database tables

`users` (all roles, `role` column) · `agents` (agent profile + approval status) · `property_categories` · `properties` · `property_images` (room type + 360° flag) · `favourites` · `enquiries` · `site_visits` · `notifications` · `contact_messages`. Foreign keys connect them; no data is duplicated (e.g. agent details are read from `users` through `properties.agent_id`). Admin and customer data live in `users` (no separate tables are needed because they have no extra columns). Report numbers are computed live with SQL, and R produces the chart images.

## 7. Feature checklist

* **Auth & security:** registration, login/logout (HTTP session cookie), BCrypt password hashing, role-based authorisation (`/api/admin/**`, `/api/agent/**`, `/api/customer/**`), agent approval gate, active/deactivated accounts, Bean-Validation on every request, JPA parameterised queries (no SQL injection), upload validation (JPG/PNG only, ≤ 5 MB, real image check, random file names), HTML escaping in the UI, session-fixation protection.
* **Properties:** statuses *Pending Verification / Available / Sold / Rented / Inactive*; only **verified + available** properties are public; "Include sold / rented" in the search filter shows closed ones with a badge.
* **Search:** location, keyword, type, category, sale/rent, min/max price, BHK, min area, furnishing, parking, availability, sorting - results refresh live with JavaScript while you type.
* **Virtual Property Tour** (property details page): room-wise gallery (Living Room, Bedroom, Kitchen, Bathroom, Balcony, Parking, Exterior) with next/previous + arrow keys; 360° images use the [Pannellum](https://pannellum.org) viewer loaded from cdnjs (internet needed for the 360° view only).
* **Notifications:** bell icon in every dashboard (auto-refresh every 30 s) for all events listed in the requirements.
* **Site visits:** cannot be booked for sold / rented / unverified properties; agents approve / reject / reschedule / complete; customers see status + notes.

## 8. Main REST endpoints

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/auth/register/customer`, `POST /api/auth/register/agent` (multipart), `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| Public | `GET /api/public/properties?...`, `/properties/{id}`, `/properties/featured`, `/categories`, `/locations`, `/stats`, `POST /contact` |
| Customer | `/api/customer/dashboard`, `/favourites`, `/enquiries`, `/visits` |
| Agent | `/api/agent/properties` (+ `/images`, `/status`), `/enquiries` (+ `/reply`), `/visits` (+ `/action`) |
| Admin | `/api/admin/dashboard`, `/customers`, `/agents`, `/properties` (+ `/verify`), `/categories`, `/visits`, `/enquiries`, `/reports`, `/reports/run-r` |
| Notifications | `GET /api/notifications`, `POST /api/notifications/{id}/read`, `/read-all` |

## 9. Try this flow (for your demo / viva)

1. Register a new **agent** → login → see "waiting for approval".
2. Login as **admin** → *Agents* → Approve (agent gets a notification).
3. Login as that agent → *My Properties* → **Add Property** → upload images per room type (tick "360°" for panorama images).
4. Admin → *Properties* → **Approve** the new property → it appears on the public *Properties* page.
5. Login as a **customer** → search → open the property → favourite, send enquiry, **Book Site Visit**, start **Virtual Tour**.
6. Agent → *Enquiries* → reply; *Site Visits* → approve / reschedule → customer sees notifications.
7. Admin → *Reports & R Analysis* → **Run R Analysis** → charts appear on the admin dashboard.

## 10. Troubleshooting

| Problem | Fix |
|---------|-----|
| `Access denied for user 'root'` | Change `spring.datasource.password` in `application.properties` to your MySQL password |
| `Communications link failure` | Start MySQL service; check port 3306 |
| Pages show 404 / no styles | Start the backend from the **backend** folder (`cd backend && mvn spring-boot:run`) |
| Lombok errors in VS Code | Install *Lombok Annotations Support for VS Code*, then *Java: Clean Java Language Server Workspace* |
| Port 8080 busy | Set `server.port=8081` in `application.properties` |
| R: `Rscript` not found | Add R's `bin` folder to PATH or set `app.rscript.path` |
| R: cannot install RMariaDB on Linux | `sudo apt install libmariadb-dev` first |
| R: "No properties found" | Run the Spring Boot app once so the sample data is created |

## 11. Notes for production

CSRF protection is disabled because the site is same-origin and uses a session cookie - enable CSRF tokens and HTTPS (`server.servlet.session.cookie.secure=true`) before any real deployment; add login-attempt throttling; change the demo passwords; move DB credentials to environment variables.
