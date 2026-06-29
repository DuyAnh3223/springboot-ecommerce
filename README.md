# 🛒 ABTechZone - Spring Boot E-commerce API

A RESTful E-commerce Backend built with **Spring Boot 3** following Clean Architecture principles.
This project is being developed as a personal portfolio to practice backend development and prepare for Java Backend internship opportunities.

> 🚧 **Project Status:** In Development

---

## ✨ Features

### Authentication & Authorization

* [ ] User Registration
* [ ] Login with JWT
* [ ] Refresh Token
* [ ] Role-based Authorization (ADMIN / CUSTOMER)

### User

* [ ] User Profile
* [ ] Address Management
* [ ] Change Password

### Product

* [ ] Product CRUD
* [ ] Category Management
* [ ] Product Search
* [ ] Product Pagination & Sorting

### Shopping Cart

* [ ] Add Product to Cart
* [ ] Update Quantity
* [ ] Remove Item
* [ ] Checkout

### Order

* [ ] Create Order
* [ ] Order History
* [ ] Order Status

### Voucher

* [ ] Voucher CRUD
* [ ] Apply Voucher

### Admin

* [ ] Dashboard
* [ ] User Management
* [ ] Product Management
* [ ] Order Management

---

## 🛠 Tech Stack

* Java 21
* Spring Boot 3
* Spring Security
* Spring Data JPA (Hibernate)
* MySQL
* JWT Authentication
* Maven
* Lombok
* MapStruct *(planned)*
* Docker *(planned)*
* Swagger / OpenAPI *(planned)*

---

## 📂 Project Structure

```text
src
├── config
├── constant
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── exception
├── mapper
├── repository
├── security
├── service
│   └── impl
├── specification
└── util
```

---

## 🚀 Getting Started

### Clone project

```bash
git clone https://github.com/DuyAnh3223/springboot-ecommerce.git
```

### Run database

Configure your MySQL connection in:

```properties
application.yml
```

### Run project

```bash
mvn spring-boot:run
```

or

```bash
./mvnw spring-boot:run
```

---

## 📌 Development Roadmap

* [x] Project Initialization
* [x] Authentication
* [ ] User Module
* [ ] Product Module
* [ ] Category Module
* [ ] Cart Module
* [ ] Order Module
* [ ] Voucher Module
* [ ] Payment Integration
* [ ] Docker
* [ ] Unit Testing
* [ ] CI/CD
* [ ] Deployment

---

## 📖 API Documentation

Coming soon...

Swagger UI:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 📸 Screenshots

Coming soon...

---

## 🧪 Testing

Coming soon...

---

## 📄 License

This project is created for learning and portfolio purposes.

---

## 👨‍💻 Author

**Duy Anh**

GitHub: https://github.com/DuyAnh3223
