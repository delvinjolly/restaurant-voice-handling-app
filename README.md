# Restaurant Voice-Handling App

A Java Spring Boot REST API that processes restaurant customer requests for food orders, menu enquiries, and table bookings.

## Features

- Restaurant menu enquiries
- Create food orders from customer messages
- Add, remove, and modify order items
- Validate item quantities and menu availability
- Confirm and cancel orders
- Handle table-booking requests
- Request validation and error handling
- PostgreSQL database integration

## Technology Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Maven
- Jakarta Bean Validation
- Git and GitHub

## Prerequisites

- Java 21 or compatible JDK
- PostgreSQL
- Git (for cloning the repository)

## Setup

Clone the repository:

```bash
git clone https://github.com/delvinjolly/restaurant-voice-handling-app.git
cd restaurant-voice-handling-app
```

Create a PostgreSQL database named `restaurant_db`.

Configure these environment variables in PowerShell:

- `DB_URL` — defaults to `jdbc:postgresql://localhost:5432/restaurant_db`
- `DB_USERNAME` — defaults to `postgres`
- `DB_PASSWORD` — your PostgreSQL password
- `DDL_AUTO` — defaults to `update`

Never commit passwords or other secrets to GitHub.

## Run the Application

Set `DB_PASSWORD` in your PowerShell session, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

The application runs at `http://localhost:8080` by default.

## API Example

**Endpoint:** `POST /api/voice`

Example JSON request:

```json
{
  "restaurantId": 1,
  "customerName": "TestCustomer",
  "customerMessage": "I want 2 chicken biriyani"
}
```

Send the request to `http://localhost:8080/api/voice`.

The restaurant ID and requested menu items must exist in your database. The response depends on the request and current order state.

## Run Tests

```powershell
.\mvnw.cmd test
```

## Project Status

Under development. Features, testing, and documentation will be improved as development continues.

## Author

Delvin Jolly
