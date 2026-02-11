# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Yuca is a Spring Boot 3.5.9 application built with Java 21 and Maven. The project uses:
- **MVC (Model-View-Controller)** architecture
- **MyBatis-Plus 3.5.9** for ORM and database operations
- **Redis** for caching
- **PostgreSQL** as the primary database
- **Lombok** for reducing boilerplate code

## Project Structure

The project is organized as a single module with feature-based packaging:

```
yuca/
├── pom.xml                          # Maven POM
└── src/main/java/org/yuca/yuca/
    ├── YucaApplication.java         # Spring Boot application entry point
    ├── common/                      # Common utilities (shared components)
    │   ├── annotation/              # Custom annotations (@RequireLogin, @SkipAuth)
    │   ├── constant/                # Constants (RedisKey, etc.)
    │   ├── exception/               # Exception handling (BusinessException, GlobalExceptionHandler)
    │   ├── interceptor/             # Interceptors (JwtAuthenticationFilter)
    │   └── response/                # Common response wrappers (Result, ErrorCode)
    ├── config/                      # Configuration classes
    │   ├── PasswordEncoderConfig.java  # Password encryption config
    │   ├── SwaggerConfig.java         # API documentation config
    │   └── WebConfig.java             # Web MVC config
    ├── security/                    # Security components
    │   ├── JwtTokenProvider.java    # JWT token generation and validation
    │   └── TokenCache.java          # Token cache interface
    └── user/                        # User feature module
        ├── controller/              # User REST controllers
        ├── service/                 # User business services
        ├── mapper/                  # User MyBatis-Plus mappers
        ├── entity/                  # User database entities
        ├── dto/                     # User DTOs
        │   ├── request/             # Request DTOs
        │   ├── response/            # Response DTOs
        │   └── internal/            # Internal DTOs
        └── cache/                   # User cache services
```

## Build and Development Commands

### Build
```bash
./mvnw clean install
```

### Run the application
```bash
./mvnw spring-boot:run
```

### Run tests
```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=YucaApplicationTests
```

### Package
```bash
./mvnw package
```

## MVC Development Guidelines

### Creating a New Feature

When adding a new feature (e.g., "order" module):

1. **Create feature directory**:
   ```bash
   mkdir -p src/main/java/org/yuca/yuca/order/{controller,service,mapper,entity,dto/{request,response,internal},cache}
   ```

2. **Create Entity**: `src/main/java/org/yuca/yuca/order/entity/Order.java` with MyBatis-Plus annotations
3. **Create Mapper**: `src/main/java/org/yuca/yuca/order/mapper/OrderMapper.java`
4. **Create Service**: `src/main/java/org/yuca/yuca/order/service/OrderService.java`
5. **Create DTOs**: in `order/dto/request/` and `order/dto/response/`
6. **Create Controller**: `src/main/java/org/yuca/yuca/order/controller/OrderController.java`

### Key Principles

- **Feature-based organization**: Each feature has its own package
- **Common components**: Shared utilities in `common/` package
- **Configuration**: App-level configs in `config/` package
- **Security**: Security components in `security/` package
- **Controllers** are thin - handle HTTP and delegate to services
- **Services** contain business logic
- **Mappers** handle database operations using MyBatis-Plus
- **Entities** represent database tables
- **DTOs** separate API layer from business logic

## MyBatis-Plus Configuration

MyBatis-Plus is configured with:
- Automatic camel case mapping (underscore to camel case)
- Auto-increment ID generation
- Logical delete support (deleted field: 1=deleted, 0=active)
- SQL logging enabled
- Mapper location: `classpath*:/mapper/**/*.xml`
- Entity package: `org.yuca.yuca.*.entity`

When creating entities:
- Use `@TableName` to specify the database table name
- Use `@TableId(type = IdType.AUTO)` for auto-increment primary keys
- Use `@TableLogic` for logical delete fields
- Use `@TableField` for column mappings if needed

## Database Configuration

PostgreSQL connection:
- Host: 47.94.247.12:5432
- Database: yuca
- Credentials configured in application.yml

Redis connection:
- Host: 47.94.247.12:6379
- Password configured in application.yml

## Notes

- Lombok is configured as an annotation processor in `maven-compiler-plugin`
- The Spring Boot Maven plugin is configured to exclude Lombok from the final artifact
- Default server port: 8500
- Cache TTL: 1 hour (3600000ms)
