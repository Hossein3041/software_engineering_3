# software_engineering_3

Dieses Reposotory enthält nur die hauptsächlichen Programmierdateien aus der Veranstaltung Software Engineering 3.
Man schaue sich bitte ebnenfalls die PDF-Datei an (das Plakat zum Projekt)
Die eigentliche Projektstruktur würde wie folgt aussehen (einmal herunterladen):

.
├── backend
│   └── demo
│       ├── pom.xml
│       ├── src
│       │   └── main
│       │       ├── java
│       │       │   └── com
│       │       │       └── example
│       │       │           └── app
│       │       │               ├── auth
│       │       │               │   ├── AppInitializer.java
│       │       │               │   ├── AuthenticationFilter.java
│       │       │               │   ├── AuthHelper.java
│       │       │               │   └── CorsConfig.java
│       │       │               ├── dto
│       │       │               │   ├── AppointmentDTO.java
│       │       │               │   ├── AuthResponse.java
│       │       │               │   ├── BookingRequest.java
│       │       │               │   ├── Center.java
│       │       │               │   ├── ErrorResponse.java
│       │       │               │   ├── LoginRequest.java
│       │       │               │   ├── PatientDTO.java
│       │       │               │   ├── PatientInfo.java
│       │       │               │   ├── RegisterRequest.java
│       │       │               │   ├── SelfInfo.java
│       │       │               │   ├── Slot.java
│       │       │               │   ├── TimeSlotDTO.java
│       │       │               │   ├── UserDto.java
│       │       │               │   └── VaccineDTO.java
│       │       │               ├── EmbeddedTomcatServer.java
│       │       │               ├── Exception
│       │       │               │   └── BookingException.java
│       │       │               ├── HelloServlet.java
│       │       │               ├── LoginScalabilityTest.java
│       │       │               ├── services
│       │       │               │   ├── AppointmentService.java
│       │       │               │   ├── QRCodeService.java
│       │       │               │   ├── UserService.java
│       │       │               │   └── VaccineService.java
│       │       │               ├── servlets
│       │       │               │   ├── AdminAppointmentsServlet.java
│       │       │               │   ├── AdminSessionServlet.java
│       │       │               │   ├── AdminTimeSlotManagementServlet.java
│       │       │               │   ├── AdminVaccineManagementServlet.java
│       │       │               │   ├── BookAppointmentServlet.java
│       │       │               │   ├── BookingCountServlet.java
│       │       │               │   ├── CentersServlet.java
│       │       │               │   ├── FrontendServlet.java
│       │       │               │   ├── LoginServlet.java
│       │       │               │   ├── LogoutServlet.java
│       │       │               │   ├── RegisterServlet.java
│       │       │               │   ├── SessionStatusServlet.java
│       │       │               │   ├── SlotsServlet.java
│       │       │               │   ├── UserBookingsServlet.java
│       │       │               │   └── VTLoginWithLatency.java
│       │       │               └── utils
│       │       │                   ├── AppointmentUtil.java
│       │       │                   ├── DataBaseUtil.java
│       │       │                   ├── PerformanceLogger.java
│       │       │                   └── SimpleDbLatency.java
│       │       ├── resources
│       │       │   ├── application.properties
│       │       │   └── init.sql
│       │       └── webapp
│       │           ├── index.html
│       │           ├── static
│       │           │   ├── action.css
│       │           │   ├── admin.css
│       │           │   ├── adminPage.js
│       │           │   ├── appointments.css
│       │           │   ├── bookingPage.js
│       │           │   ├── bookingStyle.css
│       │           │   ├── features.css
│       │           │   ├── fetch.js
│       │           │   ├── footer.css
│       │           │   ├── form.css
│       │           │   ├── header.css
│       │           │   ├── hero.css
│       │           │   ├── images
│       │           │   │   └── hero.jpg
│       │           │   ├── info.css
│       │           │   ├── jsconfig.json
│       │           │   ├── main-content.css
│       │           │   ├── main.js
│       │           │   ├── process.css
│       │           │   └── style.css
│       │           └── WEB-INF
│       │               └── web.xml
│       └── target
│           ├── classes
│           │   ├── application.properties
│           │   ├── com
│           │   │   └── example
│           │   │       └── app
│           │   │           ├── auth
│           │   │           │   ├── AppInitializer.class
│           │   │           │   ├── AuthenticationFilter.class
│           │   │           │   ├── AuthHelper.class
│           │   │           │   └── CorsConfig.class
│           │   │           ├── dto
│           │   │           │   ├── AppointmentDTO.class
│           │   │           │   ├── AuthResponse.class
│           │   │           │   ├── BookingRequest.class
│           │   │           │   ├── Center.class
│           │   │           │   ├── ErrorResponse.class
│           │   │           │   ├── LoginRequest.class
│           │   │           │   ├── PatientDTO.class
│           │   │           │   ├── PatientInfo.class
│           │   │           │   ├── RegisterRequest.class
│           │   │           │   ├── SelfInfo.class
│           │   │           │   ├── Slot.class
│           │   │           │   ├── TimeSlotDTO.class
│           │   │           │   ├── UserDto.class
│           │   │           │   └── VaccineDTO.class
│           │   │           ├── EmbeddedTomcatServer.class
│           │   │           ├── Exception
│           │   │           │   └── BookingException.class
│           │   │           ├── HelloServlet.class
│           │   │           ├── LoginScalabilityTest.class
│           │   │           ├── services
│           │   │           │   ├── AppointmentService.class
│           │   │           │   ├── QRCodeService.class
│           │   │           │   ├── UserService.class
│           │   │           │   └── VaccineService.class
│           │   │           ├── servlets
│           │   │           │   ├── AdminAppointmentsServlet.class
│           │   │           │   ├── AdminSessionServlet.class
│           │   │           │   ├── AdminTimeSlotManagementServlet$1.class
│           │   │           │   ├── AdminTimeSlotManagementServlet.class
│           │   │           │   ├── AdminVaccineManagementServlet$1.class
│           │   │           │   ├── AdminVaccineManagementServlet$2.class
│           │   │           │   ├── AdminVaccineManagementServlet.class
│           │   │           │   ├── BookAppointmentServlet.class
│           │   │           │   ├── BookingCountServlet.class
│           │   │           │   ├── CentersServlet.class
│           │   │           │   ├── FrontendServlet.class
│           │   │           │   ├── LoginServlet.class
│           │   │           │   ├── LogoutServlet.class
│           │   │           │   ├── RegisterServlet.class
│           │   │           │   ├── SessionStatusServlet.class
│           │   │           │   ├── SlotsServlet.class
│           │   │           │   ├── UserBookingsServlet.class
│           │   │           │   └── VTLoginWithLatency.class
│           │   │           └── utils
│           │   │               ├── AppointmentUtil.class
│           │   │               ├── DataBaseUtil.class
│           │   │               ├── PerformanceLogger.class
│           │   │               └── SimpleDbLatency.class
│           │   └── init.sql
│           ├── demo
│           │   ├── index.html
│           │   ├── META-INF
│           │   ├── static
│           │   │   ├── action.css
│           │   │   ├── admin.css
│           │   │   ├── adminPage.js
│           │   │   ├── appointments.css
│           │   │   ├── bookingPage.js
│           │   │   ├── bookingStyle.css
│           │   │   ├── features.css
│           │   │   ├── fetch.js
│           │   │   ├── footer.css
│           │   │   ├── form.css
│           │   │   ├── header.css
│           │   │   ├── hero.css
│           │   │   ├── images
│           │   │   │   └── hero.jpg
│           │   │   ├── info.css
│           │   │   ├── jsconfig.json
│           │   │   ├── main-content.css
│           │   │   ├── main.js
│           │   │   ├── process.css
│           │   │   └── style.css
│           │   └── WEB-INF
│           │       ├── classes
│           │       │   ├── application.properties
│           │       │   ├── com
│           │       │   │   └── example
│           │       │   │       └── app
│           │       │   │           ├── auth
│           │       │   │           │   ├── AppInitializer.class
│           │       │   │           │   ├── AuthenticationFilter.class
│           │       │   │           │   ├── AuthHelper.class
│           │       │   │           │   └── CorsConfig.class
│           │       │   │           ├── dto
│           │       │   │           │   ├── AppointmentDTO.class
│           │       │   │           │   ├── AuthResponse.class
│           │       │   │           │   ├── BookingRequest.class
│           │       │   │           │   ├── Center.class
│           │       │   │           │   ├── ErrorResponse.class
│           │       │   │           │   ├── LoginRequest.class
│           │       │   │           │   ├── PatientDTO.class
│           │       │   │           │   ├── PatientInfo.class
│           │       │   │           │   ├── RegisterRequest.class
│           │       │   │           │   ├── SelfInfo.class
│           │       │   │           │   ├── Slot.class
│           │       │   │           │   ├── TimeSlotDTO.class
│           │       │   │           │   ├── UserDto.class
│           │       │   │           │   └── VaccineDTO.class
│           │       │   │           ├── EmbeddedTomcatServer.class
│           │       │   │           ├── Exception
│           │       │   │           │   └── BookingException.class
│           │       │   │           ├── HelloServlet.class
│           │       │   │           ├── LoginScalabilityTest.class
│           │       │   │           ├── services
│           │       │   │           │   ├── AppointmentService.class
│           │       │   │           │   ├── QRCodeService.class
│           │       │   │           │   ├── UserService.class
│           │       │   │           │   └── VaccineService.class
│           │       │   │           ├── servlets
│           │       │   │           │   ├── AdminAppointmentsServlet.class
│           │       │   │           │   ├── AdminSessionServlet.class
│           │       │   │           │   ├── AdminTimeSlotManagementServlet$1.class
│           │       │   │           │   ├── AdminTimeSlotManagementServlet.class
│           │       │   │           │   ├── AdminVaccineManagementServlet$1.class
│           │       │   │           │   ├── AdminVaccineManagementServlet$2.class
│           │       │   │           │   ├── AdminVaccineManagementServlet.class
│           │       │   │           │   ├── BookAppointmentServlet.class
│           │       │   │           │   ├── BookingCountServlet.class
│           │       │   │           │   ├── CentersServlet.class
│           │       │   │           │   ├── FrontendServlet.class
│           │       │   │           │   ├── LoginServlet.class
│           │       │   │           │   ├── LogoutServlet.class
│           │       │   │           │   ├── RegisterServlet.class
│           │       │   │           │   ├── SessionStatusServlet.class
│           │       │   │           │   ├── SlotsServlet.class
│           │       │   │           │   ├── UserBookingsServlet.class
│           │       │   │           │   └── VTLoginWithLatency.class
│           │       │   │           └── utils
│           │       │   │               ├── AppointmentUtil.class
│           │       │   │               ├── DataBaseUtil.class
│           │       │   │               ├── PerformanceLogger.class
│           │       │   │               └── SimpleDbLatency.class
│           │       │   └── init.sql
│           │       ├── lib
│           │       │   ├── caffeine-2.9.3.jar
│           │       │   ├── checker-qual-3.32.0.jar
│           │       │   ├── error_prone_annotations-2.10.0.jar
│           │       │   ├── h2-2.2.224.jar
│           │       │   ├── jackson-annotations-2.16.1.jar
│           │       │   ├── jackson-core-2.16.1.jar
│           │       │   ├── jackson-databind-2.16.1.jar
│           │       │   ├── jbcrypt-0.4.jar
│           │       │   ├── jcl-over-slf4j-2.0.7.jar
│           │       │   ├── jna-5.13.0.jar
│           │       │   ├── jna-platform-5.13.0.jar
│           │       │   ├── mariadb-java-client-3.3.2.jar
│           │       │   ├── slf4j-api-2.0.7.jar
│           │       │   └── waffle-jna-3.3.0.jar
│           │       └── web.xml
│           ├── demo.war
│           ├── generated-sources
│           │   └── annotations
│           ├── maven-archiver
│           │   └── pom.properties
│           └── maven-status
│               └── maven-compiler-plugin
│                   └── compile
│                       └── default-compile
│                           ├── createdFiles.lst
│                           └── inputFiles.lst
└── performanceGraph.py
