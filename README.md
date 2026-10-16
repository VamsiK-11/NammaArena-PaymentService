# NammaArena - Payment Service

Payment Management Service for the NammaArena microservices architecture. Handles payments for turf bookings using Stripe.

## Tech Stack
- Java 17
- Spring Boot 3.2.0
- Spring Security
- JWT (jjwt 0.11.5)
- Spring Data JPA
- Spring Cloud OpenFeign
- Stripe
- MySQL

## Port
`8084`

## Getting Started

### Prerequisites
- Java 17
- Maven 3.9+
- MySQL running on port 3306
- Eureka Server running on port 8761
- Stripe account with API keys

### Environment Variables
```
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_PUBLISHABLE_KEY=your_stripe_publishable_key
```

### Run Locally
```bash
mvn clean package -DskipTests
java -jar target/*.jar
```

### Run with Docker
```bash
docker build -t nammaarena-payment-service .
docker run -p 8084:8084 nammaarena-payment-service
```

## API Endpoints
| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/payments/create` | Create a payment intent | User only |
| POST | `/api/payments/confirm` | Confirm a payment | User only |

## Part of NammaArena Microservices
| Service | Repository |
|---|---|
| Eureka Server | [NammaArena-EurekaServer](https://github.com/VamsiK-11/NammaArena-EurekaServer) |
| API Gateway | [NammaArena-APIGateway](https://github.com/VamsiK-11/NammaArena-APIGateway) |
| Auth Service | [NammaArena-AuthService](https://github.com/VamsiK-11/NammaArena-AuthService) |
| Turf Service | [NammaArena-TurfService](https://github.com/VamsiK-11/NammaArena-TurfService) |
| Booking Service | [NammaArena-BookingService](https://github.com/VamsiK-11/NammaArena-BookingService) |
| Payment Service | [NammaArena-PaymentService](https://github.com/VamsiK-11/NammaArena-PaymentService) |
