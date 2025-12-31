# CourseSphere

CourseSphere is a **JavaFX-based Course Management System** developed as part of the **Object-Oriented Design & Programming (OODP) Sessional – Course 2104**.

The project demonstrates the practical application of **object-oriented principles**, **JavaFX-based UI design**, **file handling**, and **client–server communication concepts** within a structured academic system.

---

## 📌 Project Overview

CourseSphere is designed to manage academic courses with **role-based access control** for different users.  
The system provides a clean, interactive desktop interface built using **JavaFX**, enabling users to interact with course data efficiently.

The application focuses on:
- Proper **OOP design**
- Modular architecture
- User-friendly UI
- Maintainable and scalable code structure

---

## 👥 User Roles & Features

### 🛠 Admin
- Add and remove students
- Add and remove teachers
- Add, update, and delete courses
- Assign teachers to courses
- Manage course and user data safely with file handling

### 👨‍🏫 Teacher
- View assigned courses
- Approve or reject student course requests
- Access relevant course information

### 🎓 Student
- View available courses
- Search courses by name or ID
- Apply for courses
- View application status

---

## ⚙️ Technologies Used

- **Java (JDK 21)**
- **JavaFX**
- **FXML**
- **CSS (JavaFX styling)**
- **Maven**
- **File Handling**
- **Socket Programming (LAN-based communication – if applicable)**
- **Git & GitHub (Version Control)**

---

## 🧠 Object-Oriented Concepts Applied

This project strongly emphasizes **OODP principles**, including:

- **Encapsulation** – Controlled access to data using getters/setters  
- **Abstraction** – Clear separation of responsibilities  
- **Inheritance** – Reusable class structures where applicable  
- **Polymorphism** – Method overriding and dynamic behavior  
- **Modular Design** – Separate controllers, models, views, and utilities  

---

## 📁 Project Structure

CourseSphere/
├── database/
│   └── (text files for storing application data)
│
├── src/
│   └── main/
│       ├── java/
│       │   └── files/
│       │       ├── Classes/
│       │       │   └── (model and data classes)
│       │       │
│       │       ├── Controllers/
│       │       │   └── (JavaFX controller classes)
│       │       │
│       │       ├── Server/
│       │       │   └── (server-side logic)
│       │       │
│       │       ├── Main.java
│       │       ├── RealServer.java
│       │       ├── Request.java
│       │       ├── ServerReadThread.java
│       │       ├── ServerWriteThread.java
│       │       │
│       │       └── module-info.java
│       │
│       └── resources/
│           ├── fxml/
│           ├── css/
│           └── images/
│
├── uploaded_files/
│   └── (runtime user uploads – ignored by Git)
│
├── target/
│   └── (Maven build output – ignored by Git)
│
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md


---

## ▶️ How to Run the Project

### Prerequisites
- **Java JDK 21**
- **Maven**
- **IntelliJ IDEA** (recommended)
- **JavaFX version 23/24**

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/shahriar6130/CourseSphere.git
