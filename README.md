🏥 Patient Management System

A cloud-ready, Java-based Patient Management System built using a microservices architecture with Spring Boot, Docker, and AWS CDK (Infrastructure as Code).
The project demonstrates enterprise-level design principles such as API Gateway security, JWT authentication, automated CI/CD, and ephemeral environment provisioning using AWS and LocalStack.

🚀 Overview

This system enables secure management of patient data, authentication, billing, and analytics within a scalable distributed architecture.
It uses Spring Cloud Gateway for routing and authentication, Kafka for event-driven communication, and AWS CDK to automate the deployment and destruction of resources — ensuring consistent, clean, and reproducible environments.
-----------------------------------------------------------------------------------------------------


🧩 Architecture Overview
                       ┌────────────────────────────────────┐
                       │          API Gateway               │
                       │  • Routes requests to services     │
                       │  • Validates JWT tokens            │
                       └────────────────────────────────────┘
                                       │
        ┌──────────────────────────────┼──────────────────────────────┐
        ▼                              ▼                              ▼
┌────────────────┐          ┌──────────────────┐           ┌────────────────┐
│ Auth Service   │          │ Patient Service  │           │ Billing Service│
│ • Login/Auth   │          │ • CRUD Operations│           │ • gRPC endpoint│
│ • Token Gen    │          │ • Sends Kafka Msg│           │ • Creates bills│
└────────────────┘          └──────────────────┘           └────────────────┘
                                       │
                                       ▼
                       ┌────────────────────────────────────┐
                       │        Analytics Service           │
                       │  • Consumes Kafka events           │
                       │  • Generates reports & insights    │
                       └────────────────────────────────────┘

                       
-----------------------------------------------------------------------------------------------------

🧰 Tech Stack:
Backend	:     Java 21, Spring Boot
Security:   	Spring Security, JWT
Gateway	:     Spring Cloud Gateway
Communication:	REST, gRPC, Kafka
Database:     PostgreSQL, H2 (testing)
Infrastructure:	AWS CDK, CloudFormation, LocalStack
Containerization: Docker, Amazon ECR
Build Tool: 	Maven
-----------------------------------------------------------------------------------------------------
✨ Key Features

Microservices Architecture: Independent, modular services communicating via REST, gRPC, and Kafka.

API Gateway & JWT Security: Centralized authentication and secure routing of client requests.

Infrastructure as Code (IaC): Automated resource provisioning using AWS CDK with **“deploy and destroy”** strategy.

Ephemeral Environments: Every deployment starts fresh and auto-destroys on stack deletion — no leftover resources.

Automated CI/CD Pipeline: Fully containerized build and deploy process using Docker, Amazon ECR, and LocalStack.

Analytics via Kafka: Real-time event consumption and patient data insights.
-----------------------------------------------------------------------------------------------------

⚙️ Setup & Installation
🧾 Prerequisites

Java 21+

Docker Desktop

AWS CLI configured

LocalStack installed

Maven 3.9+
-----------------------------------------------------------------------------------------------------

🛠️ Steps to Run Locally
1️⃣ Clone the Repository
git clone https://github.com/Chiranth-P/Patient-Management-System.git
cd Patient-Management-System

2️⃣ Build & Push Docker Images
./build-and-push-aws.sh


Builds and pushes all microservice Docker images to Amazon ECR (via LocalStack simulation).

3️⃣ Deploy Infrastructure
./localstack-deploy.sh


Uses AWS CDK to provision cloud resources and deploy all services.
Infrastructure removal policies are set to destroy, ensuring clean teardown.

4️⃣ Run All Services
docker-compose up
-----------------------------------------------------------------------------------------------------

🔗 API Endpoints:
🧑‍💼 Auth Service:
Method	Endpoint	Description
POST	/auth/login	Authenticates user & returns JWT token
GET	/auth/validate	Validates JWT token authenticity

🧍‍♂️ Patient Service:
Method	Endpoint	Description
GET	/api/patients	Retrieves all patient records
POST	/api/patients	Adds new patient data
PUT	/api/patients/{id}	Updates patient record
DELETE	/api/patients/{id}	Deletes a patient entry
-----------------------------------------------------------------------------------------------------

💳 Billing Service (gRPC)
Method	Description
CreateBillingAccount	Creates billing account via gRPC communication
🧠 System Highlights

🌩️ AWS CDK IaC: Complete cloud infrastructure automation with destroy-on-delete resource policies.

🐳 Dockerized Workflow: Unified build and deployment using shell scripts across all microservices.

⚡ Event-Driven Design: Kafka ensures asynchronous communication between patient and analytics services.

🔐 Security-First Approach: Centralized authentication with API Gateway and JWT validation.

☁️ LocalStack Integration: Enables local AWS simulation for full-stack development and testing.

📈 Future Enhancements

🧾 Add email/SMS notifications for appointment reminders.

📊 Expand analytics dashboards using Grafana or AWS QuickSight.

💬 Introduce real-time updates via WebSockets.

🌐 Deploy to live AWS using CDK Pipelines for continuous delivery.
