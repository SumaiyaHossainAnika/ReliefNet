# 🚨 ReliefNet
### *Disaster Relief Management System for Bangladesh* 🇧🇩

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17+-blue.svg)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)
[![Firebase](https://img.shields.io/badge/Firebase-Enabled-yellow.svg)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

ReliefNet is a comprehensive Java-based disaster relief management system specifically designed for Bangladesh's unique challenges. With robust online and offline capabilities, the platform features four specialized interfaces (Authority, Volunteer, Survivor, and Non-approved Volunteer) protected by enterprise-grade security measures including email verification, two-factor authentication, and role-based access controls. The system efficiently manages resources, emergency requests, volunteer coordination, and real-time communication between survivors and authorities, providing a secure and resilient solution for strengthening disaster response in challenging environments.

---

## 🌟 Features

### 🔐 Security & Authentication
- **📧 Email Verification**: Secure user registration with email confirmation and anti-spam protection
- **🔑 Two-Factor Authentication**: Enhanced security for sensitive operations with SMS/Email OTP
- **👥 Role-Based Access Control**: Four specialized interfaces with granular permissions and access levels
- **🎫 Authority Code Validation**: Special authentication tokens for privileged roles and emergency personnel
- **🛡️ Fraud Prevention**: Advanced fraud detection algorithms with real-time monitoring and alerts

### 🖥️ User Interfaces
1. **🏛️ Authority Interface**: 
   - Emergency management dashboard with real-time crisis monitoring
   - Resource allocation and distribution tracking
   - Volunteer assignment and coordination
   - Emergency broadcast and communication center

2. **👨‍🚒 Volunteer Interface**: 
   - Personal volunteer dashboard with task assignments
   - Resource request and donation management
   - Communication with authorities and survivors
   - Activity tracking and reporting tools

3. **🆘 Survivor Interface**: 
   - Emergency request submission with location tracking
   - Real-time communication with rescue teams
   - Resource needs reporting and status updates
   - Family safety check-in and notification system

4. **⏳ Non-approved Volunteer Interface**: 
   - Registration and verification portal
   - Skills assessment and training modules
   - Limited access to non-critical volunteer activities
   - Application status tracking and updates

### 🚀 Core Functionality
- **🌐 Online/Offline Operation**: Seamless functionality in both connected and disconnected environments with automatic sync
- **📦 Resource Management**: Efficient tracking, allocation, and distribution of disaster relief resources with inventory control
- **🚨 Emergency Request Handling**: Real-time processing of emergency requests with priority-based routing and response tracking
- **👥 Volunteer Coordination**: Comprehensive volunteer management system with skills matching and deployment optimization
- **💬 Real-Time Communication**: Direct communication channels between all user types with message encryption and delivery confirmation
- **☁️ Cloud Synchronization**: Automatic data sync when connectivity is restored with conflict resolution and backup systems
- **📊 Analytics & Reporting**: Comprehensive reporting tools for disaster response analysis and performance metrics
- **🗺️ Geographic Information System**: Interactive mapping with location-based services and GPS tracking
- **📱 Mobile-First Design**: Responsive interface optimized for various devices and screen sizes

## 🛠️ Technology Stack

| Technology | Purpose | Version |
|------------|---------|---------|
| ☕ **Java** | Core application development | 11+ |
| 🖼️ **JavaFX** | Modern user interface framework | 17+ |
| 🗄️ **SQLite** | Local database storage | 3.36+ |
| 🔥 **Firebase** | Cloud synchronization and authentication | Latest |
| 📦 **Maven** | Build and dependency management | 3.6+ |
| 🎨 **ControlsFX** | Enhanced UI components | 11.1+ |
| 📧 **JavaMail** | Email service integration | 1.6+ |

## 🚀 Getting Started

### ⚡ Prerequisites
- ☕ Java 11 or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- 📦 Maven 3.6 or higher ([Download](https://maven.apache.org/download.cgi))
- 🌐 Internet connection for initial setup and cloud features

### 📥 Installation
1. **Clone the repository**
```bash
git clone https://github.com/yourusername/ReliefNet.git
cd ReliefNet
```

2. **Build the project**
```bash
mvn clean compile dependency:copy-dependencies
```

3. **Run the application**
```bash
mvn exec:java -Dexec.mainClass=com.reliefnet.app.ReliefNetApp
```

### 🎯 Quick Start Guide
1. **First Launch**: Configure your database and network settings
2. **User Registration**: Create your account with email verification
3. **Role Selection**: Choose your role (Authority/Volunteer/Survivor)
4. **Profile Setup**: Complete your profile and verification process
5. **Start Using**: Begin coordinating disaster relief efforts!

## 📁 Project Structure

```
📦 ReliefNet/
├── 📂 src/
│   ├── 📂 main/
│   │   ├── 📂 java/
│   │   │   └── 📂 com/
│   │   │       └── 📂 reliefnet/
│   │   │           ├── 📂 app/          # 🚀 Main application entry point
│   │   │           ├── 📂 controller/   # 🎮 Application controllers & logic
│   │   │           ├── 📂 database/     # 🗄️ Database management & queries
│   │   │           ├── 📂 model/        # 📊 Data models & entities
│   │   │           ├── 📂 network/      # 🌐 Network & sync management
│   │   │           ├── 📂 util/         # 🔧 Utility classes & helpers
│   │   │           └── 📂 view/         # 🖥️ User interface views
│   │   └── 📂 resources/
│   │       ├── 📂 css/                  # 🎨 Stylesheets & themes
│   │       ├── 📂 fxml/                 # 🖼️ JavaFX layouts
│   │       └── 📂 images/               # 🖼️ Application images & icons
├── 📄 pom.xml                           # 📦 Maven configuration
├── 📄 README.md                         # 📚 Project documentation
└── 📄 .gitignore                        # 🚫 Git ignore rules
```

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

1. **🍴 Fork the repository**
2. **🌿 Create a feature branch** (`git checkout -b feature/AmazingFeature`)
3. **💾 Commit your changes** (`git commit -m 'Add some AmazingFeature'`)
4. **📤 Push to the branch** (`git push origin feature/AmazingFeature`)
5. **🔀 Create a Pull Request**

### 📋 Contribution Guidelines
- Follow Java coding standards and conventions
- Write comprehensive unit tests for new features
- Update documentation for any new functionality
- Ensure compatibility with existing offline capabilities

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support & Contact

- **🐛 Bug Reports**: [Create an Issue](https://github.com/yourusername/ReliefNet/issues)
- **💡 Feature Requests**: [Create an Issue](https://github.com/yourusername/ReliefNet/issues)
- **📧 Email**: support@reliefnet.com
- **💬 Discussions**: [GitHub Discussions](https://github.com/yourusername/ReliefNet/discussions)

## 🏆 Acknowledgments

- **🇧🇩 Bangladesh Government** for disaster management guidelines
- **🌍 International disaster relief organizations** for best practices
- **👥 Open source community** for amazing libraries and tools
- **🧪 Beta testers** for valuable feedback and testing

---

<div align="center">
  <strong>🚨 ReliefNet - Saving Lives Through Technology 🚨</strong>
  <br>
  <em>Built with ❤️ for Bangladesh's disaster resilience</em>
</div>
