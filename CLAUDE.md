# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Yuca is a full-stack web application consisting of:
- **Backend**: Spring Boot 3.5.9 (Java 21, Maven) - REST API server
- **Frontend**: Vue 3 + TypeScript + Vite - Single-page application

The backend uses MVC architecture with MyBatis-Plus ORM, PostgreSQL database, and Redis caching. The frontend uses Naive UI component library, Pinia for state management, and Vue Router for navigation.

## Repository Structure

```
yuca/
├── yuca-backend/          # Spring Boot backend
│   ├── src/main/java/org/yuca/yuca/
│   │   ├── common/        # Shared utilities, annotations, exceptions
│   │   ├── config/        # Spring configuration classes
│   │   ├── security/      # JWT authentication, token management
│   │   └── user/          # User feature module (controller, service, mapper, etc.)
│   ├── src/main/resources/
│   │   └── application.yml # Configuration file
│   ├── pom.xml            # Maven POM
│   └── CLAUDE.md          # Backend-specific documentation
└── yuca-frontend/         # Vue 3 frontend
    ├── src/
    │   ├── api/           # API client functions (axios)
    │   ├── assets/        # Static assets (styles, images)
    │   ├── composables/   # Vue composables
    │   ├── router/        # Vue Router configuration
    │   ├── stores/        # Pinia state management
    │   ├── types/         # TypeScript type definitions
    │   ├── utils/         # Utility functions
    │   ├── views/         # Page components
    │   ├── components/    # Reusable components
    │   └── main.ts        # Application entry point
    ├── .env.development   # Development environment variables
    ├── .env.production    # Production environment variables
    ├── package.json       # NPM dependencies and scripts
    ├── vite.config.ts     # Vite configuration
    └── tsconfig.json      # TypeScript configuration
```

## Backend Development Commands

### Build
```bash
cd yuca-backend
./mvnw clean install
```

### Run the application
```bash
cd yuca-backend
./mvnw spring-boot:run
```
Server runs on port **8500**.

### Run tests
```bash
cd yuca-backend
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=YucaApplicationTests
```

### Package
```bash
cd yuca-backend
./mvnw package
```

See `yuca-backend/CLAUDE.md` for detailed backend architecture.

## Frontend Development Commands

### Install dependencies
```bash
cd yuca-frontend
npm install
```

### Development server
```bash
cd yuca-frontend
npm run dev
```
Dev server runs on port **5173** with API proxy to backend port 8500.

### Build for production
```bash
cd yuca-frontend
npm run build
```
Builds output to `dist/` directory using `vue-tsc` for type checking.

### Preview production build
```bash
cd yuca-frontend
npm run preview
```

## Frontend Architecture

### Technology Stack
- **Vue 3**: Composition API with `<script setup>` syntax
- **TypeScript**: Full type safety
- **Vite**: Build tool and dev server
- **Naive UI**: Component library with theming
- **Pinia**: State management (stores in `src/stores/`)
- **Vue Router**: Route guards for authentication
- **Axios**: HTTP client with interceptors

### Key Architectural Patterns

**API Client** (`src/api/`):
- Centralized Axios instance with base URL and timeout
- Request interceptor: adds JWT token from localStorage to Authorization header
- Response interceptor: handles 200 codes, extracts data, handles 401 auth errors
- API methods organized by feature (e.g., `user.ts`)

**State Management** (`src/stores/`):
- Pinia stores using Composition API style
- `user.ts`: authentication state, token persistence via localStorage
- Stores provide reactive state and actions

**Routing** (`src/router/index.ts`):
- Lazy-loaded route components
- Route meta: `title` and `requireAuth` flags
- Navigation guard: redirects unauthenticated users to login with redirect query

**Environment Configuration**:
- Development: API base URL from `VITE_API_BASE_URL` (default: `http://localhost:8500`)
- Production: `/api` path (expects reverse proxy to backend)
- Vite dev server proxies `/api` to backend at `http://localhost:8500`

**Styling**:
- Naive UI theme with custom primary color (#14b8a6 teal)
- Global styles in `src/assets/styles/main.css`
- CSS modules or component styles via `<style>` blocks

### Creating a New Feature in Frontend

1. **Create API methods** in `src/api/`:
   ```   yuca-frontend/src/api/feature.ts
   import request from './index'
   export const getItems = () => request.get('/api/items')
   ```

2. **Create TypeScript types** in `src/types/` if needed

3. **Create views/components** in `src/views/` or `src/components/`

4. **Add routes** to `src/router/index.ts` with appropriate meta flags

5. **Create Pinia store** in `src/stores/` if state management needed

### Authentication Flow

1. User logs in via `Login.vue` (calls `user.ts` API)
2. Backend returns JWT token
3. Token stored in `localStorage` and Pinia store
4. Axios request interceptor adds `Authorization: Bearer <token>` header
5. Response interceptor redirects to login on 401 response
6. Router guard checks `requireAuth` meta and token presence

### Development Notes

- Use `@` alias for imports from `src/` directory
- Vue 3 Composition API is preferred over Options API
- All components should use `<script setup lang="ts">` syntax
- TypeScript strict mode is enabled
- Vite handles Hot Module Replacement (HMR) automatically
- Naive UI components auto-imported via global plugin setup

### API Integration

Backend returns `Result` structure: `{ code: number, data: any, message: string }`
- `code: 200` indicates success
- Frontend Axios interceptor extracts `data` field automatically
- Errors throw with `message` text

### Port Configuration

- Frontend dev server: **5173**
- Backend API server: **8500**
- Vite proxies `/api` to `http://localhost:8500`
