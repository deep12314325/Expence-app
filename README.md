# Spreetail Shared Expenses

Spring Boot + React Vite + MySQL implementation for the shared expenses assignment.

## Tech Stack

- Backend: Java 21, Spring Boot 3, Spring Web, Spring Data JPA
- Frontend: React 18, TypeScript, Vite
- Database: MySQL 8
- CSV parsing: Apache Commons CSV

## Run Locally

1. Start MySQL:

   ```powershell
   docker compose up -d mysql
   ```

2. Run the backend:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

   If Maven is not installed, open the `backend` folder in IntelliJ IDEA and run `ExpensesApplication`.

3. Run the frontend:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

4. Open `http://localhost:5173`.

Seed login:

- `aisha@example.com` / `password`
- `rohan@example.com` / `password`
- `priya@example.com` / `password`

## Import

Use the app's **Import CSV** button and select `sample-data/expenses_export.csv`, or the original `expenses_export.csv` from the assignment.

The importer does not edit the CSV before import. It records anomalies in an import report and keeps questionable expenses with `needsReview=true` unless the row is impossible to parse safely.

## Important API Endpoints

- `POST /api/auth/login`
- `GET /api/groups`
- `GET /api/groups/{groupId}/members`
- `POST /api/groups/{groupId}/imports`
- `GET /api/groups/{groupId}/imports`
- `GET /api/groups/{groupId}/expenses`
- `GET /api/groups/{groupId}/balances`
- `POST /api/groups/{groupId}/payments`

## AI Used

Built with OpenAI Codex as a development collaborator. See `AI_USAGE.md` for prompts, corrections, and failure cases.
