# Digital Equb (ዲጂታል ዕቁብ) - Android Manager

Digital Equb is a local-first, privacy-focused Android application designed to modernize the management of Ethiopian Rotating Savings and Credit Associations (Equb). It eliminates the need for manual ledger books while ensuring full transparency and security between organizers and members.

## 🌟 Key Features

- **Consensus-Based Management**: Dual-role system (Chairman & Co-Chair) ensuring data integrity through manual sync.
- **Automated SMS Parsing**: Intelligent extraction of transaction details (Bank, Amount, Reference, Sender) from CBE, Telebirr, Dashen, and Awash SMS notifications.
- **Role-Based Security**: PIN-protected access for Chairman and Co-Chair roles with audit trails for every action.
- **Flexible Cycles**: Support for Weekly, Bi-weekly, and Monthly contribution cycles.
- **Contribution Tracking**: Real-time status of members (Paid, Partial, Unpaid) with automated "nth contribution" acknowledgments.
- **Third-Party Payments**: Support for recording payments made via relatives or anonymous sources.
- **Offline First**: Entirely functional without an internet connection, storing all data securely in a local Room database.
- **Audit Logging**: Immutable (local) logs of all administrative actions for transparency.

## 🏗️ System Design Principles

### 1. Offline-First Architecture
The app follows a local-first philosophy. There is no central server. This ensures:
- **Privacy**: Financial data never leaves the organizers' devices.
- **Reliability**: Works in areas with poor or no connectivity.
- **Cost**: No cloud hosting fees for the Equb group.

### 2. Consensus & Sync Model
Data synchronization between the **Chairman (Master)** and **Co-Chair (Auditor)** happens via secure JSON export/import. The Co-Chair acts as a live backup and verifier. The app calculates "diffs" during import to show exactly what changed (new members, new payments, etc.).

### 3. MVVM Architecture
- **View (Jetpack Compose)**: Declarative UI built for ease of use in fast-paced collection environments.
- **ViewModel (StateFlow)**: Manages UI state and business logic using reactive patterns.
- **Repository**: Single source of truth abstracting the Room database.
- **Room Database**: Structured persistence with relational integrity (Members -> Installments).

### 4. SMS Regex Engine
A custom parsing engine uses regex patterns to identify financial transactions from various Ethiopian banks, allowing organizers to "confirm" payments with a single tap instead of manual typing.

## 📊 Business Logic

### Contribution Tracking
- **The Cycle**: Each Equb has a `contribution` amount and a `cycleType`.
- **Ordinals**: The system tracks how many payments a member has made in the current cycle (e.g., "3rd contribution") and provides feedback.
- **Status Mapping**:
  - `Fully Paid`: Sum of installments >= Base contribution.
  - `Partial`: 0 < Sum < Base contribution.
  - `Unpaid`: Sum = 0.

### Role Permissions
| Feature | Chairman | Co-Chair | Member |
| :--- | :---: | :---: | :---: |
| View Reports | ✅ | ✅ | ✅ |
| Record Payment | ✅ | ✅ | ❌ |
| Verify Payment | ✅ | ❌ | ❌ |
| Add/Delete Member | ✅ | ✅ | ❌ |
| Setup Equb | ✅ | ❌ | ❌ |

### Handling Third-Party Payments
The `Installment` entity includes an optional `senderName`. If Mr. Abebe's wife pays via her account, the organizer records the payment under Abebe but attaches his wife's name as the sender for audit reconciliation.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Database**: Room (with KSP)
- **Async**: Coroutines & Flow
- **JSON**: Org.json for lightweight export/import
- **Build System**: Gradle Kotlin DSL

## 🚀 Getting Started

1. **Clone the Repo**: `git clone https://github.com/your-username/DigitalEqub_android.git`
2. **Setup**: Open in Android Studio (Ladybug or later).
3. **Build**: Run `./gradlew assembleDebug` to generate an APK.
4. **Environment**: If using Firebase features, add your `google-services.json` to the `/app` folder.

---
*Developed with focus on community trust and digital inclusion.*
