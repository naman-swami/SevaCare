# SevaCare — Proactive Senior Safety Companion

> **An offline-first, dignity-preserving Android safety companion for seniors** — preventing accidental duplicate prescription overdoses, breaking cyber-extortion panic, and keeping trusted family circles connected with zero invasive surveillance.

[![Hackathon](https://img.shields.io/badge/Neighborhood%20Hacks%202026-Submission-purple)](https://neighborhood-hacks-2026.devpost.com/)
[![Track](https://img.shields.io/badge/Track-Health%20%26%20Well--being-green)](https://neighborhoodhacks.org/)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-blue)](https://developer.android.com/)
[![Architecture](https://img.shields.io/badge/Architecture-100%25%20On--Device%20(Offline--First)-success)](#privacy--architecture)

---

## 💡 The Real Problem: Why SevaCare Exists

SevaCare was inspired by a personal crisis observed firsthand in my grandfather's medicine organizer. 

He was prescribed five daily prescription pills by two different medical specialists. One morning, I opened his weekly box and noticed two completely different commercial strips: **Glycomet 500** (prescribed by his cardiologist) and **Glucophage 500** (given by his general physician). Despite different brand names, different package colors, and different pharmaceutical company logos, both pills contained the exact same active generic salt: **Metformin Hydrochloride**. He had been double-dosing for over a week without realizing it. 

When asked, he explained:
> *"The boxes look completely different, and all the writing on the packaging is in tiny English and technical jargon that my eyes cannot read."*

Researching this issue revealed a widespread public health crisis:
1. **The Global Polypharmacy Hazard:** According to the **World Health Organization (WHO)** and global longitudinal aging studies, over **40% of older adults take 5 or more prescription pills daily (polypharmacy)**. Specialists frequently prescribe the identical chemical salt under different commercial brand names, leading to accidental overdoses and emergency hospitalizations.
2. **The Cyber Extortion Epidemic:** Financial impersonation scams targeting seniors living alone are rising exponentially. Fraudsters impersonate police, tax, or legal authorities ("Digital Arrest" video calls), manufacturing artificial panic and demanding immediate digital fund transfers before the senior has time to consult their children.
3. **The Failure of Existing Apps:**
   - **Pill Reminders:** Rely on manual typing through tiny buttons and push commercial ads.
   - **Scam Checkers:** Require high digital literacy (inspecting SSL certificates or URLs).
   - **Family Trackers:** Impose invasive 24/7 GPS or video surveillance, trading an elder's dignity for family peace of mind.

---

## 🛡️ Core Innovations & How SevaCare Works

### 1. 💊 Duplicate-Salt Overdose Detection (Camera-First)
- **Active Ingredient Cross-Check:** Points the camera at any medicine blister strip or box. Rather than matching marketing brand names, SevaCare extracts and maps active generic pharmaceutical salts via a pre-seeded local Room database.
- **Instant Clinical Warning:** Scanning *Glycomet 500* followed by *Glucophage 500* triggers an immediate warning:
  > *"Wait — both medicines contain Metformin. Do not take both at the same time! Consult your doctor or pharmacist."*
- **Guided Hands-Free Scanner:** Trembling hands struggle with tiny shutter buttons. Powered by **CameraX** with real-time frame stability analysis (Laplacian sharpness), SevaCare automatically captures the packaging when steady for 800ms.

### 2. 🛑 "Ruko Mode" Panic Brake for Cyber Extortion
- **Breaking Manufactured Urgency:** When an elder receives an intimidating legal threat or scans a fraudulent UPI/payment QR code, one tap triggers **Ruko Mode** — a mandatory 30-minute full-screen cooling-off screen.
- **Calming De-escalation:** Plays soothing voice reassurance in plain language (*"Pause. Do not worry. Do not send any money. Let us call your family first."*).
- **Deliberate Exit Barrier:** Locks payment flows and requires waiting 60 seconds before allowing an exit by confirming: *"I have spoken to my family."*
- **Family Secret Word:** Encrypted in Android Keystore to defend against AI voice-cloning scams (*"Ask them for the family secret word before transferring funds"*).

### 3. 🕊️ Neighbourhood & Family Circle (Zero Surveillance)
- **Dignity Over Surveillance:** Zero continuous GPS tracking. Zero video cameras.
- **Routine Learning:** Learns the elder's normal morning medicine and wake-up habits locally.
- **Deviation Ladder:** If one check-in is missed, an on-device gentle reminder sounds. If two consecutive check-ins are missed, SevaCare dispatches a non-panicked SMS to designated family members or trusted neighbours:
  > *"Dad has not checked in this morning. Please give him a quick call."*

### 4. 🧏 Geriatric Design System & Accessibility
- **Large Touch Targets:** Minimum **≥88dp** for primary buttons and **≥64dp** for all controls.
- **Tactile 3D Pill Buttons:** Custom Compose buttons with a 4dp physical collapse and haptic feedback.
- **Geriatric Voice Pacing:** Spoken audio runs at an unhurried **0.82x speed** for older ears.
- **WCAG AAA Contrast:** High-contrast palette designed for cataract and low-vision recovery.
- **3D Mascot Companion:** A friendly sparrow mascot guides each interaction with supportive expressions and gestures.

---

## 🏗️ System Architecture

```
┌──────────────────────────────────────────────────────────┐
│              Geriatric UI & Accessibility Layer          │
│   Jetpack Compose 1.7 • 88dp Touch Targets • Haptics     │
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│                 Edge Perception (100% Offline)           │
│   CameraX Live Analysis • Laplacian Sharpness • ML Kit   │
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│               Deterministic Core Safety Engine           │
│   Room SQLite Database • 350+ Formulation Mappings       │
│   Duplicate-Salt Matcher • Ephemeral Scam Scorer         │
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│                 Safety & Emergency Protection            │
│   Ruko Mode Cooling-Off • SMSManager Dispatcher          │
│   Sensor Fall Heuristics • Android Keystore Security     │
└──────────────────────────────────────────────────────────┘
```

- **100% Local-First:** All scanning, duplicate matching, scam scoring, and speech synthesis operate in Airplane Mode with zero network dependencies.
- **Ephemeral Privacy:** Message and notification evaluation takes place in volatile memory and is never written to disk.

---

## 📊 Competition Status & Honest Disclaimer

> ⚠️ **Hackathon Transparency Note:** Built solo over 4 days (working 2 to 3 hours per evening) for **Neighborhood Hacks 2026**. 
> - **Verified & Passing:** The core Android architecture, Room database with duplicate-salt detection, guided CameraX scanner, Ruko Mode state machine, Family Circle SMS dispatch logic, and **41 automated unit tests** are completely verified.
> - **Prototypes Under Active Development:** Advanced modules (like real-world sensor fall detection and 3D mascot runtime `.glb` integration) are functional prototypes operating with safe 2D vector fallbacks to ensure complete stability.
> - **Medical Disclaimer:** SevaCare is an educational and proactive safety companion, not a certified diagnostic medical device. Final prescription decisions belong to a qualified doctor or pharmacist.

---

## 🛠️ Built With

* **Android Studio & Kotlin:** Native Android architecture (Target SDK 35, Min SDK 26).
* **Jetpack Compose 1.7:** Accessible, high-contrast, tactile UI components.
* **CameraX:** Hands-free auto-capture with frame stability analysis.
* **Room SQLite Database:** Pre-seeded offline generic-to-brand medicine mappings.
* **Google Antigravity:** AI pair programmer used to scaffold boilerplate, verify accessibility contrast, and generate unit test suites.
* **Tripo Studio:** 3D character pipeline used to model and rig the sparrow mascot companion.

---

## 🚀 Getting Started & Building the App

### Prerequisites
* Android Studio Ladybug / Meerkat (or newer)
* Microsoft OpenJDK 17 LTS
* Android SDK Platform 35

### Build Steps
```bash
# Clone the repository
git clone https://github.com/naman-swami/SevaCare.git
cd SevaCare

# Run unit test suite
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```
The compiled output is located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📜 License
Distributed under the **MIT License**. Built with care for **Neighborhood Hacks 2026**.
