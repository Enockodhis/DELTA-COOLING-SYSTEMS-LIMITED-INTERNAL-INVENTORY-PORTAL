# 🧊 Delta Cooling Systems Internal Inventory Portal

![Firebase](https://img.shields.io/badge/Frontend-Firebase%20Hosting-orange?logo=firebase)
![Next.js](https://img.shields.io/badge/Framework-Next.js-black?logo=nextdotjs)
![Java](https://img.shields.io/badge/Backend-Java%20(Spring%20Boot)-blue?logo=java)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-blue?logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-green)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

---

## 📖 Overview

The **Delta Internal Inventory Portal** is a secure and centralized system designed for **Delta Cooling Systems Limited** to manage air conditioner products stored in the warehouse.  

This platform ensures that even in the CEO’s absence, staff members can:
- Search and view product information by scanning a **QR code** or entering a **unique product code**
- Retrieve product descriptions, stock quantity, and specifications
- Access item details stored in the database rather than relying on external sites like Amazon

It aims to streamline inventory management, reduce manual dependency, and provide instant access to product information.

---

## 🚀 Core Features

✅ **Secure Authentication**
- Email + Password Login  
- 2FA via Email OTP or Google Authenticator  

📦 **Product Management**
- Add, edit, or remove product entries  
- Auto-generate and print QR codes for items  

📱 **QR Code Scanning**
- Scan any QR code to open the product’s internal details page  

🔍 **Product Search**
- Find products instantly using their unique item code  

🧾 **Activity Logs**
- Track which staff accessed or modified an item  

👩‍💻 **Role-Based Access**
- Admin, Staff, and Viewer roles with distinct privileges  

☁️ **Firebase Deployment**
- Frontend hosted on Firebase for high availability and SSL support  

---

## 🖼️ Project Visuals

<img width="1536" height="1024" alt="ChatGPT Image Oct 20, 2025, 05_21_28 PM" src="https://github.com/user-attachments/assets/a1bba039-6cfd-42cc-af8c-abfbe64e5ccc" />

## 🧠 System Architecture

           ┌────────────────────────┐
           │   Frontend (Next.js)   │
           │ - Firebase Hosting     │
           │ - React UI Components  │
           │ - QR Scanner + Auth UI │
           └──────────┬─────────────┘
                      │ REST API Calls
           ┌──────────┴─────────────┐
           │   Backend (Java, API)  │
           │ - Spring Boot          │
           │ - JWT Auth Middleware  │
           │ - QR/Product Controller│
           └──────────┬─────────────┘
                      │
           ┌──────────┴─────────────┐
           │  PostgreSQL Database   │
           │ - users, products, qr  │
           │ - logs, auth_tokens    │
           └────────────────────────┘

---

## 🧩 Tech Stack

| Layer | Technology |
|-------|-------------|
| **Frontend** | Next.js (React, TypeScript) |
| **Backend** | Java (Spring Boot) |
| **Database** | PostgreSQL |
| **Authentication** | JWT + Firebase Email OTP + Google Authenticator |
| **Deployment** | Firebase Hosting (Frontend), Firebase Cloud Run (Backend) |
| **QR Management** | qrcode.js / react-qr-reader |

---
## Frontend Setup (Next.js)

cd frontend
npm install
npm run dev

Create .env.local in /frontend
NEXT_PUBLIC_API_URL=https://<your-backend-domain>/api
NEXT_PUBLIC_FIREBASE_API_KEY=<firebase-api-key>
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=<firebase-auth-domain>

## Backend Setup (Java - Spring Boot)

cd backend
./mvnw clean install
./mvnw spring-boot:run

application.properties:
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/delta_inventory
spring.datasource.username=postgres
spring.datasource.password=yourpassword
jwt.secret=your_jwt_secret


## Database Schema (PostgreSQL)

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255),
    role VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255),
    description TEXT,
    qr_code_url TEXT,
    amazon_link TEXT,
    stock_quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

## 🔐 Authentication Flow
1. User logs in using email and password.
2. A verification code is sent to the registered email or verified through Google Authenticator.
3. On success, the backend issues a JWT token.
4. Token is stored in local storage for session management.

## 📱 QR Scanning Flow
1. Each product box in the warehouse has a unique QR code linked to its code.
2. Scanning redirects to:
   https://portal.deltacooling.co.ke/product/{code}
3. The frontend calls:
   GET /api/products/{code}
The product details are fetched and displayed — name, description, image, quantity, and Amazon link if available.

## 📁 Folder Structure

delta-internal-portal/
├── frontend/                # Next.js app
│   ├── pages/
│   ├── components/
│   ├── public/
│   └── utils/
├── backend/                 # Java Spring Boot API
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── pom.xml
└── README.md

## ☁️ Firebase Deployment
Deploy Frontend
cd frontend
npm run build
firebase login
firebase init
firebase deploy

Deploy Backend (Cloud Run)
cd backend
gcloud builds submit --tag gcr.io/<your-project-id>/delta-backend
gcloud run deploy delta-backend --image gcr.io/<your-project-id>/delta-backend --platform managed

## 🧭 Future Enhancements

🧾 Generate and print QR codes for products

🧠 AI-powered smart search

📸 Product image uploads

📊 Data analytics dashboard

🔄 Sync with external APIs for automatic stock updates

## 👨‍💻 Author
Enock Odhiambo
Software Engineer

## 🪶 License
This project is licensed under the MIT License – you are free to use and modify it.

## ⭐ How to Contribute

1. Fork this repository
2. Create a new branch:
git checkout -b feature/your-feature
3. Commit your changes:
git commit -m "Added new feature"
4. Push to your fork and open a Pull Request

## 🏷️ Tags
Inventory Management · QR Code System · Next.js · Java · PostgreSQL · Firebase · Internal Portal · Delta Cooling Systems

## Licence

---

## 🪶`LICENSE.md` (MIT License)

```markdown
MIT License

Copyright (c) 2025 Delta Cooling Systems Limited

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights  
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell  
copies of the Software, and to permit persons to whom the Software is  
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in  
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER  
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,  
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN  
THE SOFTWARE.

