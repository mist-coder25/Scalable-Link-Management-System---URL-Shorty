# BlinkURL (Smart Link Shortener) - Project Documentation

## 1. Project Overview
**Name:** BlinkURL
**Goal:** A robust, scalable SaaS URL shortener featuring intelligent redirection, detailed analytics, user management, and a seamless guest mode.
**Current State:** Functional MVP with advanced features (Guest Mode, Analytics, Geo-routing).

## 2. Technology Stack
- **Backend Framework:** Spring Boot 3.x (Java 17+)
- **Build Tool:** Maven
- **Database:** PostgreSQL (Primary), Redis (Caching - *configured but currently using DB fallbacks for some paths*)
- **Frontend:** Vanilla HTML5, CSS3 (Custom Design System), JavaScript (ES6+ Modules)
- **Security:** Spring Security (Crypto for password protected links, API Key auth implemented but potentially partially integrated)
- **Dependencies:**
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-data-redis`
  - `spring-boot-starter-security`
  - *Note: Lombok was explicitly removed and replaced with standard POJOs.*

## 3. Project Structure & Architecture
### Backend (`src/main/java/com/urlshortener`)
- **Controller Layer:**
  - `UrlController.java`: Handles API endpoints for shortening (`/api/shorten`), management, and redirection logic.
  - `DashboardController.java`: Serves dashboard data (`/api/dashboard/*`).
  - `AuthController.java`: Handles Login/Signup.
  - `ApiKeyController.java`: Manages API key generation for developers.
- **Service Layer (Business Logic):**
  - `UrlService.java`: Core logic for creating links, Base62 encoding, caching, and recording clicks. Includes Guest Mode logic (7-day provisional expiry).
  - `RedirectService.java`: Intelligent routing logic (Country/Device detection) used during expansion.
  - `AnalyticsService.java`: Aggregates usage data (daily clicks, browser stats, OS stats) for the dashboard.
  - `RateLimiterService.java`: IP-based rate limiting using a token bucket or sliding window approach (custom implementation).
- **Model Layer (Domain):**
  - `UrlMapping.java`: The core entity. Stores `originalUrl`, `shortCode`, `clickCount`, `expiresAt`, `geoRules`, `deviceRules`.
  - `User.java`: User account data.
  - `ClickEvent.java`: High-volume entity recording individual click details (IP, Referrer, User-Agent).
  - `ApiKey.java`: For API access.
- **DTOs:**
  - `LinkDto`, `AnalyticsResponse`, `DetailedStatsResponse`: Data transfer objects for API responses.

### Frontend (`src/main/resources/static`)
- **`index.html`**: The main landing page. Contains the "Hero" section, Guest Mode/Login toggles, and Quick Shortener.
- **`app.js`**: Frontend logic for `index.html`. Handles API calls to `/api/shorten`, manages Guest Mode session storage, and updates the UI dynamically.
- **`dashboard.html / dashboard.js`**: User area for managing links and viewing charts.
- **`style.css`**: Global styles using a custom CSS variable system (no Tailwind/Bootstrap). Features glassmorphism and animated backgrounds.

## 4. Completed Work & Development History
### Phase 1: Stabilization (Build Fixes)
- **Issue:** The project initially suffered from massive build failures due to missing Lombok dependencies and syntax errors in `AnalyticsService`.
- **Action:** 
  - Implementation of standard Getters/Setters/Constructors across all Models and DTOs.
  - Fixed syntax errors in `AnalyticsService.java` (undefined methods, incorrect stream logic).
  - Resolved `RateLimiterService` compilation issues.
  - **Result:** 'Build Success' using Maven.

### Phase 2: Core Feature Refinement
- **Guest Mode:**
  - **Backend:** Verified `UrlService` logic to enforce a 7-day expiry for anonymous users (`user == null`).
  - **Frontend:** Implemented a visible "Guest Mode" implementation. 
    - *Correction:* The "Guest Mode" button was hidden or missing from the Navbar. We explicitly reordered the HTML elements in `index.html` to place "Guest Mode" between "Features" and "Login" for better visibility.
- **Analytics & Optimization:**
  - Optimized N+1 query issues by aggregating stats in SQL/Service layer rather than looping in Controllers.
  - Added specific endpoints for detailed breakdown (Browser, OS, Country).

### Phase 3: Deployment & Verification
- **Resource Syncing:** Used `mvn process-resources` to ensure HTML/JS changes in `src` are reflected in `target` without a full rebuild.
- **Verification:** Used browser automation to verify the Homepage loads, the DOM contains necessary elements, and the styling is applied.

## 5. Current Implementation Details (Deep Dive)
- **Redirection Logic:** `UrlController` checks:
  1. Active Status & Expiry.
  2. Click Limits.
  3. Password Protection (redirects to `password.html` if needed).
  4. Geo/Device Rules (redirects to specific targets if criteria matched).
- **Storage:**
  - PostgreSQL stores persistent relational data.
  - Redis is setup for caching hot URLs (`url:{shortCode}`) to reduce database load on redirection.

## 6. How to Run & Continue
1. **Start Database:** Ensure Postgres (Port 5432) and Redis (Port 6379) are running.
2. **Build:** `mvn clean install`
3. **Run:** `mvn spring-boot:run` or `java -jar target/url-shortener-*.jar`
4. **Access:** `http://localhost:8080`
