# साथी (Saathi) — Aapka Digital Saathi

> **An offline-first elder companion app for 75 million elderly Indians** — preventing medicine mix-ups and financial scams, running entirely on-device with zero cloud dependency.

[![Hackathon](https://img.shields.io/badge/Neighborhood%20Hacks%202026-Submission-purple)](https://neighborhood-hacks-2026.devpost.com/)
[![Track](https://img.shields.io/badge/Track-Health%20%26%20Well--being-green)](https://neighborhoodhacks.org/)
[![Platform](https://img.shields.io/badge/Platform-Android%2026%2B-blue)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## The Problem (Backed by National Data)

India has **75 million elderly people with chronic illness** (LASI Wave-1, IIPS/MoHFW/Harvard, 2020):
- **49% take 5+ pills daily** (polypharmacy) — multiple specialists prescribe the same salt under different brand names
- **52% don't take medicines correctly** — partly due to poor health literacy
- **50%+ cannot read English** — yet all medicine labels are printed in English

And India's **Digital Arrest scam crisis** (I4C / MHA, 2024):
- ₹120.3 Crore lost in just January–April 2024 (4 months)
- **70%+ victims are senior citizens**
- A 73-year-old Bengaluru woman lost ₹3.16 Crore: *"They were wearing police uniforms with name badges... My hands were shaking so much I could barely read the OTP. I had nobody in the room to ask."*

**Existing apps fail the elderly:**
- Medisafe, MyTherapy → Schedule reminders only, no salt-level duplicate detection
- 1mg, Practo → E-commerce platforms, not safety tools  
- Truecaller → Phone ID only, no document scanning
- Google Lookout → General accessibility, not India-specific health domain
- **No app combines medicine safety + scam detection offline for Hindi speakers**

---

## Four Genuine Innovations

### 🔴 Innovation 1: Ruko Mode — India's First Elder Panic-Break Protocol

When Saathi detects scam signals in a photographed document, it does **not** just show a warning badge. It triggers a **mandatory 30-minute full-screen pause**:

- Plays calm Hindi audio: *"Ruko! Saathi ne khatarnak cheezein notice ki hain. Koi bhi paise mat bhejiye."*
- Shows a large countdown timer (cannot be dismissed with a swipe)
- Requires typing "SAATHI" to exit (unlikely under scammer pressure)
- One-tap sends a WhatsApp alert to a saved family number

**Why this works:** Every anti-scam app today gives a passive warning. Scammers succeed because they create *manufactured urgency*. Ruko Mode inserts a 30-minute calm delay that **frustrates the scammer's own timeline**. Elder Financial Abuse research consistently shows that the primary predictor of being scammed is decision-making under manufactured time pressure — this is the first feature that directly addresses that root mechanism.

---

### 🟡 Innovation 2: Duplicate Pill Alert — Clinical Safety, No Competitor Has This

Every medicine app tells you *what* a pill is. Saathi is the **first to detect duplicate active ingredients across different brand names in a single scanning session**:

1. User scans **Glycomet 500** → Database maps to salt: `Metformin Hydrochloride`
2. User scans **Glucophage 500** → Database maps to salt: `Metformin Hydrochloride`  
3. Saathi shows: 🔴 *"Dono mein ek hi cheez hai — Metformin. Dono saath mat lijiye!"*

LASI Wave-1 explicitly documents duplicate-salt polypharmacy as a leading cause of medicine harm among Indian seniors seeing multiple specialists. This feature addresses a real documented hazard that **no competitor app handles**.

---

### 📋 Innovation 3: Chemist Parchi — Digital-to-Offline Bridge

Every "generic alternative" feature in India assumes the user will shop online. Saathi recognizes that Indian seniors trust their **local neighborhood chemist** who has known them for 20 years.

Saathi generates a **shareable image card** the senior physically shows at their local chemist:

```
┌─────────────────────────────────────────┐
│           📋 CHEMIST PARCHI             │
│                                         │
│  Bhaiya, yeh dena:                      │
│  Telmisartan 40mg (Jan Aushadhi)        │
│                                         │
│  Brand: ₹148 → Jan Aushadhi: ₹18       │
│  Bachat: ₹130 per patta                 │
│                                         │
│                    Powered by Saathi App│
└─────────────────────────────────────────┘
```

No health app in India has mapped its UX to the social trust pattern of the local chemist relationship.

---

### 🎯 Innovation 4: 5-Step Calm Disclosure — Clinical Protocol, Not a Chatbot

Instead of a generative AI that might hallucinate (dangerous for medical advice), Saathi uses a **scripted 5-step state machine** based on the WHO Teach-Back Method:

| Step | What Saathi Says | Source |
|:---|:---|:---|
| S1 — Warm Open | *"Namaste! Yeh kaun si cheez hai aapke haath mein?"* | Psychological safety |
| S2 — Context | *"Doctor ne inhe kis cheez ke liye diya?"* | Avoids assumption |
| S3 — Explanation | Retrieved from SQLite: *"Yeh BP ki goli hai..."* | **Verified DB row** |
| S4 — How to Take | Retrieved from SQLite: *"Khane ke baad ek goli..."* | **Verified DB row** |
| S5 — Teach-Back | *"Achha, toh yeh kab leni hai?"* | Comprehension check |

**A SQLite row cannot hallucinate. This is a deliberate AI safety design choice.**

---

## Technical Architecture

```
Android App (Kotlin + Jetpack Compose)
│
├── Camera Layer
│   └── CameraX → Google ML Kit Text Recognition v2
│       ├── Latin (English medicine labels, legal notices)
│       └── Devanagari (Hindi medicine names, notices)
│       → BUNDLED: zero network, zero API key, runs offline
│
├── Intelligence Layer  
│   ├── Room + SQLite FTS
│   │   ├── medicines table: 100 core Indian chronic-disease drugs
│   │   │   (brand_name, generic_salt, hindi_purpose, hindi_dosage,
│   │   │    brand_mrp, jan_aushadhi_mrp, schedule_class)
│   │   └── scam_phrases table: 44 patterns from I4C/MHA advisories
│   │
│   ├── Duplicate Pill Detector (session ViewModel, O(n) salt comparison)
│   └── Scam Score Engine (weighted phrase matching, threshold=6)
│
├── Voice Layer
│   ├── Android TextToSpeech (Hindi-IN, speechRate=0.82f for elderly)
│   └── Android SpeechRecognizer (on-device, EXTRA_PREFER_OFFLINE=true)
│
└── Output Layer
    ├── Jetpack Compose UI (88dp+ touch targets, high-contrast colors)
    ├── Chemist Parchi (Canvas-generated Bitmap → share sheet)
    └── Ruko Mode (full-screen Compose overlay + WhatsApp deep link)
```

### Why Zero APIs = Better for This Use Case

| Approach | Risk | Saathi's Choice |
|:---|:---|:---|
| LLM for medical advice | Hallucination = patient harm | ❌ Dropped entirely |
| Cloud OCR | Privacy: elderly upload documents to internet | ❌ Dropped |
| External pharmacy API | Requires internet, fails in poor connectivity | ❌ Dropped |
| On-device ML Kit | Bundled, offline, GDPR-safe | ✅ Used |
| Local SQLite | Cannot hallucinate, instant, no network | ✅ Used |

---

## Data Sources

**Medicine Data:**
- CDSCO Drug Database (Central Drugs Standard Control Organisation, cdsco.gov.in)
- NPPA Ceiling Prices (National Pharmaceutical Pricing Authority, nppaindia.nic.in)
- PMBJP Jan Aushadhi Catalogue (janaushadhi.gov.in)

**Scam Phrase Data:**
- I4C (Indian Cyber Crime Coordination Centre) Digital Arrest advisories
- MHA (Ministry of Home Affairs) elder fraud circulars
- Published Lok Sabha Q&A on cybercrime demographics (2024)

**Empirical Evidence:**
- LASI Wave-1 (Longitudinal Ageing Study in India, IIPS/MoHFW/Harvard, 2020)
- I4C Annual Report 2024
- Cybercrime statistics: ₹11,333 Crore total, Jan–Sep 2024

---

## Setup & Build

### Prerequisites
- Android Studio Ladybug (2024.2.x) or newer
- Android SDK 37 (API 37)
- Java 11+ (bundled with Android Studio)
- Physical Android device or emulator running API 26+

### Build Steps

```bash
# Clone the repo
git clone https://github.com/[your-username]/Saathi.git
cd Saathi

# Open in Android Studio and click "Sync Project with Gradle Files"
# Then Run → Run 'app'
```

### First Launch
The app seeds its local database (~100 medicines + 44 scam patterns) on first launch. This takes ~1 second. No internet connection needed.

---

## Project Structure

```
app/src/main/java/com/saathi/app/
├── SaathiApplication.kt        # App class — DB initialization
├── MainActivity.kt              # Single activity
├── ui/
│   ├── SaathiNavGraph.kt       # Navigation graph
│   ├── theme/                  # Elder-optimized colors, typography
│   ├── home/HomeScreen.kt      # 3-card landing screen
│   ├── medicine/
│   │   ├── MedicineScreen.kt   # Scanner + Disclosure + Parchi
│   │   └── MedicineViewModel.kt # Duplicate alert, TTS, Parchi generation
│   └── document/
│       ├── DocumentScreen.kt   # Notice scanner + Ruko Mode overlay
│       └── DocumentViewModel.kt # Scam scoring, Ruko countdown
└── data/
    ├── model/Medicine.kt        # Room entity
    ├── model/ScamPhrase.kt      # Room entity
    ├── db/
    │   ├── SaathiDatabase.kt   # Room database
    │   ├── MedicineDao.kt      # Queries incl. duplicate salt check
    │   ├── ScamPhraseDao.kt    # Weighted phrase matching
    │   └── DatabaseSeeder.kt   # 100 medicines + 44 scam patterns
    └── repository/
        └── SaathiRepository.kt # Single data access point
```

---

## Accessibility Design

| Feature | Implementation | Standard |
|:---|:---|:---|
| Touch targets | Min 88dp height on all interactive elements | Material 3 Large Components |
| Text size | Min 18sp body text (16sp is standard) | WHO guideline for 60+ |
| Contrast ratio | All pairs meet WCAG AA (4.5:1) | WCAG 2.1 |
| Language | Hindi primary + English subtitles | Bilingual for 50%+ non-English elderly |
| Voice | TTS reads every key result aloud | For low-literacy users |
| No login | Zero account creation | Barrier-free for elderly |
| Offline | 100% offline after install | Rural India connectivity |

---

## Team

**Naman Swami** — Solo developer, high school student, India  
Built in ~80 hours for Neighborhood Hacks 2026

---

## License

MIT License — See [LICENSE](LICENSE)

---

*"Aapka data kabhi bahar nahi jaata." — Saathi App*
