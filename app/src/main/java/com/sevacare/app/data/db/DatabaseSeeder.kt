package com.sevacare.app.data.db

import com.sevacare.app.data.model.Medicine
import com.sevacare.app.data.model.ScamPhrase

/**
 * Pre-populates the SevaCare offline database on first install.
 *
 * MEDICINE DATA SOURCES (all public domain / government data):
 * - CDSCO Drug Database (cdsco.gov.in)
 * - NPPA Ceiling Prices (nppaindia.nic.in)
 * - PMBJP Jan Aushadhi Catalogue (janaushadhi.gov.in)
 * - Standard Indian pharmacopoeial data
 *
 * SCAM PHRASE SOURCES:
 * - I4C (Indian Cyber Crime Coordination Centre) advisories
 * - MHA "Digital Arrest" awareness bulletins
 * - Published Lok Sabha questions on cybercrime demographics
 *
 * ~100 most-prescribed medicines for India's chronic disease burden:
 * Diabetes · Hypertension · Cardiac · Thyroid · Respiratory · Pain/Inflammation
 */
object DatabaseSeeder {

    suspend fun seed(database: SevaCareDatabase) {
        val medicineDao = database.medicineDao()
        val scamPhraseDao = database.scamPhraseDao()

        // Only seed if empty
        if (medicineDao.getAllMedicines().isNotEmpty()) return

        seedMedicines(medicineDao)
        seedScamPhrases(scamPhraseDao)
    }

    private suspend fun seedMedicines(dao: MedicineDao) {
        // Note: Room doesn't have a direct "insertAll" on the DAO in this setup;
        // we use a helper that directly inserts via the DB.
        // This will be handled via the Application class using a proper DAO insert call.
        // Data is defined here and inserted via SevaCareApplication.
    }

    private suspend fun seedScamPhrases(dao: ScamPhraseDao) {
        // Same pattern — inserted via SevaCareApplication on first run.
    }

    // ============================================================
    // MEDICINE DATA — 100 core Indian chronic-disease medicines
    // ============================================================
    val medicines = listOf(

        // ── DIABETES ──────────────────────────────────────────────
        Medicine(
            brandName = "Glycomet 500",
            genericSalt = "Metformin Hydrochloride",
            purposeHindi = "Yeh khoon mein sugar kam karne ki goli hai. Madhumeh (diabetes) ke liye di jaati hai.",
            howToTakeHindi = "Khane ke saath, din mein 2 baar, subah aur raat.",
            brandMrp = 28f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Metformin 500mg"
        ),
        Medicine(
            brandName = "Glucophage 500",
            genericSalt = "Metformin Hydrochloride",
            purposeHindi = "Yeh khoon mein sugar kam karne ki goli hai. Madhumeh (diabetes) ke liye di jaati hai.",
            howToTakeHindi = "Khane ke saath, din mein 2 baar, subah aur raat.",
            brandMrp = 35f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Metformin 500mg"
        ),
        Medicine(
            brandName = "Glycomet GP1",
            genericSalt = "Metformin + Glimepiride",
            purposeHindi = "Do dawaaiyon ka milaap — sugar control ke liye. Ek saath kaam karte hain.",
            howToTakeHindi = "Khane se pehle, subah naashte ke waqt, ek goli.",
            brandMrp = 98f, janAushadhiMrp = 22f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Metformin + Glimepiride 1mg"
        ),
        Medicine(
            brandName = "Amaryl 1",
            genericSalt = "Glimepiride",
            purposeHindi = "Yeh bhi sugar ki goli hai. Pancreas ko zyada insulin banana sikhata hai.",
            howToTakeHindi = "Subah naashte se pehle, ek goli roz.",
            brandMrp = 55f, janAushadhiMrp = 8f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Glimepiride 1mg"
        ),
        Medicine(
            brandName = "Jardiance 10",
            genericSalt = "Empagliflozin",
            purposeHindi = "Yeh nayi kism ki sugar ki dawaai hai. Peshab ke zariye sugar bahar nikaalti hai.",
            howToTakeHindi = "Subah naashte ke saath, ek goli roz.",
            brandMrp = 285f, janAushadhiMrp = 85f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Empagliflozin 10mg"
        ),
        Medicine(
            brandName = "Januvia 50",
            genericSalt = "Sitagliptin",
            purposeHindi = "Yeh bhi sugar ki goli hai. Khana khane ke baad sugar zyada nahi badhne deti.",
            howToTakeHindi = "Khane ke saath ya baad mein, din mein ek baar.",
            brandMrp = 245f, janAushadhiMrp = 55f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Sitagliptin 50mg"
        ),
        Medicine(
            brandName = "Voglibose 0.3",
            genericSalt = "Voglibose",
            purposeHindi = "Khana khane ke baad sugar badhne se rokti hai.",
            howToTakeHindi = "Khana shuru karne ke saath hi khayein, teen baar roz.",
            brandMrp = 48f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Voglibose 0.3mg"
        ),
        Medicine(
            brandName = "Pioglit 15",
            genericSalt = "Pioglitazone",
            purposeHindi = "Yeh insulin ko zyada asar karne mein madad karta hai.",
            howToTakeHindi = "Khane ke saath ya baad mein, din mein ek baar.",
            brandMrp = 75f, janAushadhiMrp = 15f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Pioglitazone 15mg"
        ),

        // ── HYPERTENSION / BP ────────────────────────────────────
        Medicine(
            brandName = "Telma 40",
            genericSalt = "Telmisartan",
            purposeHindi = "Yeh BP (blood pressure) kam karne ki goli hai. Dil ko surakshit rakhti hai.",
            howToTakeHindi = "Roz subah khane se pehle ya baad mein, ek goli.",
            brandMrp = 148f, janAushadhiMrp = 18f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Telmisartan 40mg"
        ),
        Medicine(
            brandName = "Telsartan 40",
            genericSalt = "Telmisartan",
            purposeHindi = "Yeh BP (blood pressure) kam karne ki goli hai. Dil ko surakshit rakhti hai.",
            howToTakeHindi = "Roz subah khane se pehle ya baad mein, ek goli.",
            brandMrp = 135f, janAushadhiMrp = 18f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Telmisartan 40mg"
        ),
        Medicine(
            brandName = "Amlong 10",
            genericSalt = "Amlodipine Besylate",
            purposeHindi = "Yeh nason ko dhila karta hai jisse BP kam hota hai.",
            howToTakeHindi = "Roz ek goli, khane se pehle ya baad mein.",
            brandMrp = 45f, janAushadhiMrp = 6f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Amlodipine 10mg"
        ),
        Medicine(
            brandName = "Amlodipine 5",
            genericSalt = "Amlodipine Besylate",
            purposeHindi = "Yeh nason ko dhila karta hai jisse BP kam hota hai.",
            howToTakeHindi = "Roz ek goli, khane se pehle ya baad mein.",
            brandMrp = 35f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Amlodipine 5mg"
        ),
        Medicine(
            brandName = "Stamlo 5",
            genericSalt = "Amlodipine Besylate",
            purposeHindi = "Yeh nason ko dhila karta hai jisse BP kam hota hai.",
            howToTakeHindi = "Roz ek goli, khane se pehle ya baad mein.",
            brandMrp = 68f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Amlodipine 5mg"
        ),
        Medicine(
            brandName = "Envas 5",
            genericSalt = "Enalapril Maleate",
            purposeHindi = "Yeh BP kam karne aur dil ki rakshe karne ki dawaai hai.",
            howToTakeHindi = "Roz subah, ek goli, khane se pehle.",
            brandMrp = 55f, janAushadhiMrp = 7f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Enalapril 5mg"
        ),
        Medicine(
            brandName = "Losartan 50",
            genericSalt = "Losartan Potassium",
            purposeHindi = "Yeh BP kam karne ki goli hai. Kidney ko bhi surakshit rakhti hai.",
            howToTakeHindi = "Roz ek ya do baar, khane se pehle ya baad mein.",
            brandMrp = 62f, janAushadhiMrp = 9f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Losartan 50mg"
        ),
        Medicine(
            brandName = "Atenolol 50",
            genericSalt = "Atenolol",
            purposeHindi = "Yeh dil ki dhadkan dhimi karta hai aur BP kam karta hai.",
            howToTakeHindi = "Roz subah, ek goli, khane ke saath.",
            brandMrp = 28f, janAushadhiMrp = 4f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Atenolol 50mg"
        ),
        Medicine(
            brandName = "Telvas AM",
            genericSalt = "Telmisartan + Amlodipine",
            purposeHindi = "Do BP ki dawaaiyon ka milaap — zyada asar ke liye.",
            howToTakeHindi = "Roz subah, ek goli.",
            brandMrp = 175f, janAushadhiMrp = 28f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Telmisartan + Amlodipine"
        ),
        Medicine(
            brandName = "Olmecip 20",
            genericSalt = "Olmesartan Medoxomil",
            purposeHindi = "Yeh BP kam karne ki aur dil ki dawaai hai.",
            howToTakeHindi = "Roz ek goli, khane ke saath.",
            brandMrp = 95f, janAushadhiMrp = 12f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Olmesartan 20mg"
        ),
        Medicine(
            brandName = "Indapamide SR",
            genericSalt = "Indapamide",
            purposeHindi = "Yeh peshab badhane wali goli hai jo BP bhi kam karti hai.",
            howToTakeHindi = "Subah naashte ke saath, ek goli roz.",
            brandMrp = 72f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Hypertension",
            janAushadhiName = "Indapamide SR 1.5mg"
        ),

        // ── CARDIAC ──────────────────────────────────────────────
        Medicine(
            brandName = "Ecosprin 75",
            genericSalt = "Aspirin",
            purposeHindi = "Yeh khoon ke thakke banana rokti hai. Dil ke daure se bachav ke liye di jaati hai.",
            howToTakeHindi = "Roz ek goli, khane ke baad.",
            brandMrp = 18f, janAushadhiMrp = 2f,
            scheduleClass = "OTC", category = "Cardiac",
            janAushadhiName = "Aspirin 75mg"
        ),
        Medicine(
            brandName = "Atorva 10",
            genericSalt = "Atorvastatin",
            purposeHindi = "Yeh khoon mein cholesterol kam karne ki goli hai. Dil ki nason ko saaf rakhti hai.",
            howToTakeHindi = "Roz raat ko sone se pehle, ek goli.",
            brandMrp = 85f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Atorvastatin 10mg"
        ),
        Medicine(
            brandName = "Lipitor 20",
            genericSalt = "Atorvastatin",
            purposeHindi = "Yeh khoon mein cholesterol kam karne ki goli hai. Dil ki nason ko saaf rakhti hai.",
            howToTakeHindi = "Roz raat ko sone se pehle, ek goli.",
            brandMrp = 155f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Atorvastatin 20mg"
        ),
        Medicine(
            brandName = "Rosuvastatin 10",
            genericSalt = "Rosuvastatin Calcium",
            purposeHindi = "Yeh bhi cholesterol kam karne ki goli hai.",
            howToTakeHindi = "Roz ek goli, din ke kisi bhi waqt.",
            brandMrp = 125f, janAushadhiMrp = 15f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Rosuvastatin 10mg"
        ),
        Medicine(
            brandName = "Clopivas 75",
            genericSalt = "Clopidogrel",
            purposeHindi = "Yeh khoon thakka nahi baanne deti. Angioplasty ya heart attack ke baad di jaati hai.",
            howToTakeHindi = "Roz ek goli, khane ke saath ya baad mein.",
            brandMrp = 95f, janAushadhiMrp = 12f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Clopidogrel 75mg"
        ),
        Medicine(
            brandName = "Cardace 2.5",
            genericSalt = "Ramipril",
            purposeHindi = "Yeh dil ko majboot rakhta hai aur BP bhi kam karta hai.",
            howToTakeHindi = "Roz ek baar, khane ke saath.",
            brandMrp = 68f, janAushadhiMrp = 8f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Ramipril 2.5mg"
        ),
        Medicine(
            brandName = "Metolar XR 25",
            genericSalt = "Metoprolol Succinate",
            purposeHindi = "Yeh dil ki dhadkan ko theek rakhta hai aur BP kam karta hai.",
            howToTakeHindi = "Roz subah, ek goli, khane ke saath. Muh mein ghulaye nahi.",
            brandMrp = 88f, janAushadhiMrp = 12f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Metoprolol Succinate 25mg"
        ),
        Medicine(
            brandName = "Concor 2.5",
            genericSalt = "Bisoprolol Fumarate",
            purposeHindi = "Yeh dil ki dhadkan ko normal rakhta hai.",
            howToTakeHindi = "Roz subah, ek goli, khane ke saath.",
            brandMrp = 115f, janAushadhiMrp = 14f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Bisoprolol 2.5mg"
        ),
        Medicine(
            brandName = "Isordil 5",
            genericSalt = "Isosorbide Dinitrate",
            purposeHindi = "Yeh seene ke dard (angina) ke liye di jaati hai. Dil ki nason ko khol deti hai.",
            howToTakeHindi = "Jab dard ho tab, zabaan ke neeche rakh dein.",
            brandMrp = 32f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Isosorbide Dinitrate 5mg"
        ),
        Medicine(
            brandName = "Digoxin 0.25",
            genericSalt = "Digoxin",
            purposeHindi = "Yeh dil ki dhadkan ko theek karne ki purani aur important dawaai hai.",
            howToTakeHindi = "Doctor ke bataye hue time par, ekdum sahi matra mein lena zaroori hai.",
            brandMrp = 22f, janAushadhiMrp = 4f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Digoxin 0.25mg"
        ),
        Medicine(
            brandName = "Frusemide 40",
            genericSalt = "Furosemide",
            purposeHindi = "Yeh peshab badhane wali goli hai. Seene ya pair mein paani aane par di jaati hai.",
            howToTakeHindi = "Subah khane ke baad, ek goli. Dopahar ke baad mat lena — nahi to raat ko baar baar uthna padega.",
            brandMrp = 15f, janAushadhiMrp = 2f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Furosemide 40mg"
        ),

        // ── THYROID ──────────────────────────────────────────────
        Medicine(
            brandName = "Thyronorm 50",
            genericSalt = "Levothyroxine Sodium",
            purposeHindi = "Yeh thyroid ki kami poori karne ki goli hai. Thakaan aur sardi dur karti hai.",
            howToTakeHindi = "Roz subah khali pet, paani ke saath. Naashte se aadha ghanta pehle.",
            brandMrp = 42f, janAushadhiMrp = 6f,
            scheduleClass = "H", category = "Thyroid",
            janAushadhiName = "Levothyroxine 50mcg"
        ),
        Medicine(
            brandName = "Eltroxin 50",
            genericSalt = "Levothyroxine Sodium",
            purposeHindi = "Yeh thyroid ki kami poori karne ki goli hai. Thakaan aur sardi dur karti hai.",
            howToTakeHindi = "Roz subah khali pet, paani ke saath. Naashte se aadha ghanta pehle.",
            brandMrp = 38f, janAushadhiMrp = 6f,
            scheduleClass = "H", category = "Thyroid",
            janAushadhiName = "Levothyroxine 50mcg"
        ),
        Medicine(
            brandName = "Carbimazole 5",
            genericSalt = "Carbimazole",
            purposeHindi = "Yeh thyroid gland ki zyada kaam karne ki bimari mein di jaati hai.",
            howToTakeHindi = "Doctor ke bataye hue time par, roz teeno waqt.",
            brandMrp = 35f, janAushadhiMrp = 7f,
            scheduleClass = "H", category = "Thyroid",
            janAushadhiName = "Carbimazole 5mg"
        ),

        // ── RESPIRATORY / ASTHMA / COPD ──────────────────────────
        Medicine(
            brandName = "Asthalin Inhaler",
            genericSalt = "Salbutamol",
            purposeHindi = "Yeh inhaler saans phoolne par foran rahat deta hai.",
            howToTakeHindi = "Jab saans phoole tab 1-2 baar pump karein. Doctor ke bina roz mat lein.",
            brandMrp = 65f, janAushadhiMrp = 18f,
            scheduleClass = "H", category = "Respiratory",
            janAushadhiName = "Salbutamol Inhaler"
        ),
        Medicine(
            brandName = "Seroflo 250",
            genericSalt = "Salmeterol + Fluticasone",
            purposeHindi = "Yeh combination inhaler daasthma aur saans ki takleef ke liye hai. Roz leni hai.",
            howToTakeHindi = "Subah aur raat, ek baar, inhaler se.",
            brandMrp = 398f, janAushadhiMrp = 95f,
            scheduleClass = "H", category = "Respiratory",
            janAushadhiName = "Salmeterol + Fluticasone 250mcg"
        ),
        Medicine(
            brandName = "Deriphyllin",
            genericSalt = "Theophylline + Etofylline",
            purposeHindi = "Yeh saans ki naliyon ko kholta hai. Asthma aur COPD mein di jaati hai.",
            howToTakeHindi = "Khane ke baad, din mein 2 baar.",
            brandMrp = 38f, janAushadhiMrp = 8f,
            scheduleClass = "H", category = "Respiratory",
            janAushadhiName = "Theophylline + Etofylline"
        ),
        Medicine(
            brandName = "Tiova Inhaler",
            genericSalt = "Tiotropium Bromide",
            purposeHindi = "Yeh COPD ke liye roz lene wali inhaler hai. Ek baar roz kaafi hai.",
            howToTakeHindi = "Roz subah ek baar, handihaler mein capsule daal kar lein.",
            brandMrp = 685f, janAushadhiMrp = 180f,
            scheduleClass = "H", category = "Respiratory",
            janAushadhiName = "Tiotropium 18mcg"
        ),
        Medicine(
            brandName = "Budesonide Inhaler",
            genericSalt = "Budesonide",
            purposeHindi = "Yeh saans ki naliyon ki sujan kam karta hai. Roz lena zaroori hai.",
            howToTakeHindi = "Subah aur raat, ek baar each, inhaler se.",
            brandMrp = 245f, janAushadhiMrp = 55f,
            scheduleClass = "H", category = "Respiratory",
            janAushadhiName = "Budesonide 200mcg Inhaler"
        ),
        Medicine(
            brandName = "Montelukast 10",
            genericSalt = "Montelukast Sodium",
            purposeHindi = "Yeh allergy aur asthma mein saans ki takleef rokti hai.",
            howToTakeHindi = "Roz raat ko sone se pehle, ek goli.",
            brandMrp = 95f, janAushadhiMrp = 15f,
            scheduleClass = "H", category = "Respiratory",
            janAushadhiName = "Montelukast 10mg"
        ),

        // ── PAIN / INFLAMMATION ───────────────────────────────────
        Medicine(
            brandName = "Combiflam",
            genericSalt = "Ibuprofen + Paracetamol",
            purposeHindi = "Yeh dard aur bukhaar ki common goli hai.",
            howToTakeHindi = "Khane ke baad lein. Khali pet bilkul mat lein.",
            brandMrp = 28f, janAushadhiMrp = 5f,
            scheduleClass = "OTC", category = "Pain",
            janAushadhiName = "Ibuprofen + Paracetamol"
        ),
        Medicine(
            brandName = "Crocin 650",
            genericSalt = "Paracetamol",
            purposeHindi = "Yeh bukhaar aur halke dard ke liye goli hai.",
            howToTakeHindi = "Din mein teen baar, khane ke baad. Ek din mein 4 se zyada mat lein.",
            brandMrp = 35f, janAushadhiMrp = 4f,
            scheduleClass = "OTC", category = "Pain",
            janAushadhiName = "Paracetamol 650mg"
        ),
        Medicine(
            brandName = "Diclofenac 50",
            genericSalt = "Diclofenac Sodium",
            purposeHindi = "Yeh haddiyon aur jodon ke dard ke liye di jaati hai.",
            howToTakeHindi = "Khane ke saath, din mein 2-3 baar. Kidney problem ho toh doctor se poochein.",
            brandMrp = 22f, janAushadhiMrp = 4f,
            scheduleClass = "H", category = "Pain",
            janAushadhiName = "Diclofenac 50mg"
        ),
        Medicine(
            brandName = "Voveran SR 100",
            genericSalt = "Diclofenac Sodium",
            purposeHindi = "Yeh haddiyon aur jodon ke dard ke liye di jaati hai.",
            howToTakeHindi = "Khane ke saath, din mein 1-2 baar. Kidney problem ho toh doctor se poochein.",
            brandMrp = 55f, janAushadhiMrp = 4f,
            scheduleClass = "H", category = "Pain",
            janAushadhiName = "Diclofenac SR 100mg"
        ),
        Medicine(
            brandName = "Etoricoxib 60",
            genericSalt = "Etoricoxib",
            purposeHindi = "Yeh jodon ke dard aur sujan ke liye di jaati hai. Pet ke liye thodi safe hai.",
            howToTakeHindi = "Din mein ek baar, khane ke saath.",
            brandMrp = 98f, janAushadhiMrp = 18f,
            scheduleClass = "H", category = "Pain",
            janAushadhiName = "Etoricoxib 60mg"
        ),
        Medicine(
            brandName = "Pregabalin 75",
            genericSalt = "Pregabalin",
            purposeHindi = "Yeh naso ke dard aur nerve pain ke liye di jaati hai.",
            howToTakeHindi = "Subah aur raat, din mein 2 baar. Neend aa sakti hai.",
            brandMrp = 125f, janAushadhiMrp = 22f,
            scheduleClass = "H1", category = "Pain",
            janAushadhiName = "Pregabalin 75mg"
        ),
        Medicine(
            brandName = "Lyrica 75",
            genericSalt = "Pregabalin",
            purposeHindi = "Yeh naso ke dard aur nerve pain ke liye di jaati hai.",
            howToTakeHindi = "Subah aur raat, din mein 2 baar. Neend aa sakti hai.",
            brandMrp = 285f, janAushadhiMrp = 22f,
            scheduleClass = "H1", category = "Pain",
            janAushadhiName = "Pregabalin 75mg"
        ),

        // ── GASTRO ───────────────────────────────────────────────
        Medicine(
            brandName = "Pan 40",
            genericSalt = "Pantoprazole Sodium",
            purposeHindi = "Yeh pet mein tezaab (acidity) kam karne ki goli hai. Ulcer mein bhi di jaati hai.",
            howToTakeHindi = "Khane se aadha ghanta pehle, subah.",
            brandMrp = 65f, janAushadhiMrp = 8f,
            scheduleClass = "H", category = "Gastro",
            janAushadhiName = "Pantoprazole 40mg"
        ),
        Medicine(
            brandName = "Omez 20",
            genericSalt = "Omeprazole",
            purposeHindi = "Yeh pet mein tezaab (acidity) kam karne ki goli hai.",
            howToTakeHindi = "Khane se aadha ghanta pehle, subah aur/ya raat.",
            brandMrp = 48f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Gastro",
            janAushadhiName = "Omeprazole 20mg"
        ),
        Medicine(
            brandName = "Rantac 150",
            genericSalt = "Ranitidine",
            purposeHindi = "Yeh acidity aur pet dard ke liye di jaati hai.",
            howToTakeHindi = "Khane se pehle ya baad mein, din mein 2 baar.",
            brandMrp = 22f, janAushadhiMrp = 3f,
            scheduleClass = "OTC", category = "Gastro",
            janAushadhiName = "Ranitidine 150mg"
        ),
        Medicine(
            brandName = "Domperidone 10",
            genericSalt = "Domperidone",
            purposeHindi = "Yeh ulti rokne aur pet ki harkaat theek karne ki goli hai.",
            howToTakeHindi = "Khane se aadha ghanta pehle, din mein 3 baar.",
            brandMrp = 18f, janAushadhiMrp = 3f,
            scheduleClass = "H", category = "Gastro",
            janAushadhiName = "Domperidone 10mg"
        ),

        // ── KIDNEY / UROLOGY ─────────────────────────────────────
        Medicine(
            brandName = "Tamsulosin 0.4",
            genericSalt = "Tamsulosin Hydrochloride",
            purposeHindi = "Yeh peshab karne mein takleef (prostate problem) mein di jaati hai. Sirf mard ke liye.",
            howToTakeHindi = "Roz raat ko, khane ke baad, ek capsule.",
            brandMrp = 125f, janAushadhiMrp = 18f,
            scheduleClass = "H", category = "Urology",
            janAushadhiName = "Tamsulosin 0.4mg"
        ),

        // ── BONE / OSTEOPOROSIS ──────────────────────────────────
        Medicine(
            brandName = "Calcirol 60000",
            genericSalt = "Cholecalciferol (Vitamin D3)",
            purposeHindi = "Yeh Vitamin D ki kami poori karta hai. Haddiyon ko mazboot karta hai.",
            howToTakeHindi = "Hafte mein ek baar, khane ke baad. Doctor ke bataye schedule ke anusaar.",
            brandMrp = 42f, janAushadhiMrp = 8f,
            scheduleClass = "OTC", category = "Bone",
            janAushadhiName = "Vitamin D3 60000 IU"
        ),
        Medicine(
            brandName = "Shelcal 500",
            genericSalt = "Calcium Carbonate + Vitamin D3",
            purposeHindi = "Yeh calcium ki kami poori karta hai. Haddiyon ke kamzor hone (osteoporosis) mein di jaati hai.",
            howToTakeHindi = "Khane ke saath, din mein 1-2 baar.",
            brandMrp = 78f, janAushadhiMrp = 12f,
            scheduleClass = "OTC", category = "Bone",
            janAushadhiName = "Calcium Carbonate 500mg + Vit D3"
        ),
        Medicine(
            brandName = "Alendronate 70",
            genericSalt = "Alendronic Acid",
            purposeHindi = "Yeh haddiyon ko tootne se bachati hai. Osteoporosis ke liye di jaati hai.",
            howToTakeHindi = "Hafte mein ek baar, subah khali pet, paani ke saath. Aadha ghanta lait mat jaayein.",
            brandMrp = 158f, janAushadhiMrp = 35f,
            scheduleClass = "H", category = "Bone",
            janAushadhiName = "Alendronate 70mg"
        ),

        // ── ANXIETY / SLEEP ───────────────────────────────────────
        Medicine(
            brandName = "Lonazep 0.5",
            genericSalt = "Clonazepam",
            purposeHindi = "Yeh neend aane mein madad karta hai aur anxiety bhi kam karta hai. Habit lagti hai — doctor ke bina band mat karein.",
            howToTakeHindi = "Roz raat ko sone se pehle, ek goli.",
            brandMrp = 28f, janAushadhiMrp = 5f,
            scheduleClass = "H1", category = "Anxiety",
            janAushadhiName = "Clonazepam 0.5mg"
        ),
        Medicine(
            brandName = "Alprax 0.25",
            genericSalt = "Alprazolam",
            purposeHindi = "Yeh anxiety aur tension ke liye di jaati hai. Aadat lagti hai — doctor ki salah ke bina band mat karein.",
            howToTakeHindi = "Din mein 1-3 baar, doctor ke bataye anusaar.",
            brandMrp = 22f, janAushadhiMrp = 5f,
            scheduleClass = "H1", category = "Anxiety",
            janAushadhiName = "Alprazolam 0.25mg"
        ),

        // ── ANEMIA ───────────────────────────────────────────────
        Medicine(
            brandName = "Fefol",
            genericSalt = "Ferrous Sulphate + Folic Acid",
            purposeHindi = "Yeh khoon ki kami (anemia) ke liye di jaati hai. Khoon badhata hai.",
            howToTakeHindi = "Khane ke baad, din mein 1-2 baar.",
            brandMrp = 38f, janAushadhiMrp = 5f,
            scheduleClass = "OTC", category = "Anemia",
            janAushadhiName = "Ferrous Sulphate + Folic Acid"
        ),
        Medicine(
            brandName = "B12 Injection",
            genericSalt = "Methylcobalamin",
            purposeHindi = "Yeh Vitamin B12 ki kami poori karta hai. Nason ke liye zaroori hai.",
            howToTakeHindi = "Doctor ke bataye schedule par injection ke roop mein.",
            brandMrp = 45f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Anemia",
            janAushadhiName = "Methylcobalamin Injection"
        ),

        // ── ANTIBIOTICS (COMMON) ──────────────────────────────────
        Medicine(
            brandName = "Augmentin 625",
            genericSalt = "Amoxicillin + Clavulanic Acid",
            purposeHindi = "Yeh infection ki goli hai. Doctor ne bataye pura course khatam karein.",
            howToTakeHindi = "Khane ke saath, har 8 ghante mein, 5-7 din.",
            brandMrp = 245f, janAushadhiMrp = 55f,
            scheduleClass = "H", category = "Antibiotic",
            janAushadhiName = "Amoxicillin + Clavulanate 625mg"
        ),
        Medicine(
            brandName = "Azithral 500",
            genericSalt = "Azithromycin",
            purposeHindi = "Yeh bacterial infection ki goli hai. Sirf 3-5 din ka course hota hai.",
            howToTakeHindi = "Roz ek goli, khane ke saath ya baad mein, 3-5 din.",
            brandMrp = 95f, janAushadhiMrp = 20f,
            scheduleClass = "H", category = "Antibiotic",
            janAushadhiName = "Azithromycin 500mg"
        ),

        // ── SKIN ──────────────────────────────────────────────────
        Medicine(
            brandName = "Betnovate C",
            genericSalt = "Betamethasone + Clioquinol",
            purposeHindi = "Yeh skin ki allergy aur khujli ke liye cream hai.",
            howToTakeHindi = "Theek jagah par din mein 2-3 baar lagayein. Chehra par zyada mat lagayein.",
            brandMrp = 52f, janAushadhiMrp = 12f,
            scheduleClass = "H", category = "Skin",
            janAushadhiName = "Betamethasone + Clioquinol Cream"
        ),
        Medicine(
            brandName = "Cetrizine 10",
            genericSalt = "Cetirizine Hydrochloride",
            purposeHindi = "Yeh allergy, khujli aur naak behne ke liye di jaati hai.",
            howToTakeHindi = "Roz raat ko sone se pehle, ek goli. Neend aati hai.",
            brandMrp = 18f, janAushadhiMrp = 3f,
            scheduleClass = "OTC", category = "Skin",
            janAushadhiName = "Cetirizine 10mg"
        ),

        // ── EYES ──────────────────────────────────────────────────
        Medicine(
            brandName = "Latanoprost Eye Drops",
            genericSalt = "Latanoprost",
            purposeHindi = "Yeh aankh ke dab (glaucoma) ke liye drop hai.",
            howToTakeHindi = "Roz raat ko, ek-ek drop dono aankhon mein.",
            brandMrp = 185f, janAushadhiMrp = 45f,
            scheduleClass = "H", category = "Eyes",
            janAushadhiName = "Latanoprost 0.005% Eye Drops"
        ),
        Medicine(
            brandName = "Timolol Eye Drops",
            genericSalt = "Timolol Maleate",
            purposeHindi = "Yeh bhi aankh ke pressure (glaucoma) ke liye drop hai.",
            howToTakeHindi = "Subah aur raat, ek-ek drop.",
            brandMrp = 45f, janAushadhiMrp = 12f,
            scheduleClass = "H", category = "Eyes",
            janAushadhiName = "Timolol 0.5% Eye Drops"
        ),

        // ── NEUROLOGICAL ──────────────────────────────────────────
        Medicine(
            brandName = "Syndopa Plus",
            genericSalt = "Levodopa + Carbidopa",
            purposeHindi = "Yeh Parkinson ki bimari ke liye di jaati hai. Kaanpna aur chal phirna theek karta hai.",
            howToTakeHindi = "Din mein 3-4 baar, khane se aadha ghanta pehle.",
            brandMrp = 185f, janAushadhiMrp = 45f,
            scheduleClass = "H", category = "Neurological",
            janAushadhiName = "Levodopa + Carbidopa"
        ),
        Medicine(
            brandName = "Gabapentin 300",
            genericSalt = "Gabapentin",
            purposeHindi = "Yeh naso ke dard aur ek prakar ki epilepsy ke liye di jaati hai.",
            howToTakeHindi = "Din mein 3 baar, doctor ke bataye anusaar.",
            brandMrp = 95f, janAushadhiMrp = 18f,
            scheduleClass = "H", category = "Neurological",
            janAushadhiName = "Gabapentin 300mg"
        ),

        // ── SUPPLEMENTS ───────────────────────────────────────────
        Medicine(
            brandName = "Limcee 500",
            genericSalt = "Vitamin C (Ascorbic Acid)",
            purposeHindi = "Yeh Vitamin C ki kami poori karta hai aur immunity badhata hai.",
            howToTakeHindi = "Roz ek tablet, khane ke baad. Paani mein ghol kar pee sakte hain.",
            brandMrp = 28f, janAushadhiMrp = 4f,
            scheduleClass = "OTC", category = "Supplements",
            janAushadhiName = "Vitamin C 500mg"
        ),
        Medicine(
            brandName = "Becosules",
            genericSalt = "Vitamin B Complex + Vitamin C",
            purposeHindi = "Yeh B vitamins ka milaap hai. Thakaan door karta hai.",
            howToTakeHindi = "Roz ek capsule, khane ke baad.",
            brandMrp = 38f, janAushadhiMrp = 6f,
            scheduleClass = "OTC", category = "Supplements",
            janAushadhiName = "Vitamin B Complex + Vit C"
        ),

        // ── URIC ACID / GOUT ──────────────────────────────────────
        Medicine(
            brandName = "Zyloric 100",
            genericSalt = "Allopurinol",
            purposeHindi = "Yeh khoon mein uric acid kam karta hai. Gout (jodon mein tez dard) rokta hai.",
            howToTakeHindi = "Khane ke baad, din mein ek baar. Khub paani piyein.",
            brandMrp = 32f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Gout",
            janAushadhiName = "Allopurinol 100mg"
        ),
        Medicine(
            brandName = "Febuxostat 40",
            genericSalt = "Febuxostat",
            purposeHindi = "Yeh bhi uric acid kam karne ki nayi dawaai hai.",
            howToTakeHindi = "Roz ek goli, khane ke baad.",
            brandMrp = 125f, janAushadhiMrp = 25f,
            scheduleClass = "H", category = "Gout",
            janAushadhiName = "Febuxostat 40mg"
        ),

        // ── ANTICOAGULANTS ────────────────────────────────────────
        Medicine(
            brandName = "Acitrom 2",
            genericSalt = "Acenocoumarol",
            purposeHindi = "Yeh khoon patla rakhti hai. Khoon thakka nahi baanne deti. Bahut dhyan se leni hai.",
            howToTakeHindi = "Doctor ke bataye time par, bilkul sahi dose mein. INR test niyamit karwayein.",
            brandMrp = 45f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Anticoagulant",
            janAushadhiName = "Acenocoumarol 2mg"
        ),
        Medicine(
            brandName = "Warfarin 2",
            genericSalt = "Warfarin Sodium",
            purposeHindi = "Yeh khoon patla rakhti hai. Bahut zaroor hai INR check karte rehna.",
            howToTakeHindi = "Doctor ke bataye time par, bilkul sahi dose mein. Haari-bhoori cheezein kam khayein.",
            brandMrp = 28f, janAushadhiMrp = 6f,
            scheduleClass = "H", category = "Anticoagulant",
            janAushadhiName = "Warfarin 2mg"
        ),

        // More medicines to reach ~100 total...
        Medicine(
            brandName = "Metrogyl 400",
            genericSalt = "Metronidazole",
            purposeHindi = "Yeh amoeba aur bacteria ke infection ke liye di jaati hai.",
            howToTakeHindi = "Khane ke saath, din mein 3 baar.",
            brandMrp = 28f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Antibiotic",
            janAushadhiName = "Metronidazole 400mg"
        ),
        Medicine(
            brandName = "Ciprofloxacin 500",
            genericSalt = "Ciprofloxacin",
            purposeHindi = "Yeh bacterial infection ki takatwar goli hai. Pura course khatam karein.",
            howToTakeHindi = "Khane se aadha ghanta pehle ya 2 ghante baad, din mein 2 baar.",
            brandMrp = 48f, janAushadhiMrp = 8f,
            scheduleClass = "H", category = "Antibiotic",
            janAushadhiName = "Ciprofloxacin 500mg"
        ),
        Medicine(
            brandName = "Norflox TZ",
            genericSalt = "Norfloxacin + Tinidazole",
            purposeHindi = "Yeh pet ke infection aur dast ke liye di jaati hai.",
            howToTakeHindi = "Khane ke 1 ghante baad, din mein 2 baar.",
            brandMrp = 38f, janAushadhiMrp = 6f,
            scheduleClass = "H", category = "Antibiotic",
            janAushadhiName = "Norfloxacin + Tinidazole"
        ),
        Medicine(
            brandName = "Chloroquine 250",
            genericSalt = "Chloroquine Phosphate",
            purposeHindi = "Yeh malaria aur kuch autoimmune bimariyon mein di jaati hai.",
            howToTakeHindi = "Doctor ke bataye anusaar. Malaria mein ek hafte ka course hota hai.",
            brandMrp = 18f, janAushadhiMrp = 4f,
            scheduleClass = "H", category = "Antiparasitic",
            janAushadhiName = "Chloroquine 250mg"
        ),
        Medicine(
            brandName = "Fluconazole 150",
            genericSalt = "Fluconazole",
            purposeHindi = "Yeh fungal infection ke liye di jaati hai. Aksar ek hi dose kaafi hota hai.",
            howToTakeHindi = "Ek goli, ek hi baar, khane ke saath ya baad mein.",
            brandMrp = 38f, janAushadhiMrp = 8f,
            scheduleClass = "H", category = "Antifungal",
            janAushadhiName = "Fluconazole 150mg"
        ),
        Medicine(
            brandName = "Prednisolone 10",
            genericSalt = "Prednisolone",
            purposeHindi = "Yeh sujan kam karne ki takatwar dawaai hai. Doctor ke bina band mat karein.",
            howToTakeHindi = "Khane ke saath, subah. Doctor ke bataye anusaar dose kam hogi.",
            brandMrp = 28f, janAushadhiMrp = 5f,
            scheduleClass = "H", category = "Steroid",
            janAushadhiName = "Prednisolone 10mg"
        ),
        Medicine(
            brandName = "Deflazacort 6",
            genericSalt = "Deflazacort",
            purposeHindi = "Yeh sujan aur allergy ke liye di jaati hai.",
            howToTakeHindi = "Khane ke saath, doctor ke bataye anusaar.",
            brandMrp = 65f, janAushadhiMrp = 12f,
            scheduleClass = "H", category = "Steroid",
            janAushadhiName = "Deflazacort 6mg"
        ),
        Medicine(
            brandName = "Hydroxychloroquine 200",
            genericSalt = "Hydroxychloroquine Sulphate",
            purposeHindi = "Yeh joints ki sujan (rheumatoid arthritis) aur lupus ke liye di jaati hai.",
            howToTakeHindi = "Khane ke saath, din mein 1-2 baar. Aankhon ka check karwate rehna.",
            brandMrp = 95f, janAushadhiMrp = 18f,
            scheduleClass = "H", category = "Autoimmune",
            janAushadhiName = "Hydroxychloroquine 200mg"
        ),
        Medicine(
            brandName = "Methotrexate 2.5",
            genericSalt = "Methotrexate",
            purposeHindi = "Yeh joints ki bimari ke liye hafte mein ek baar di jaati hai. Bahut dhyan se leni hai.",
            howToTakeHindi = "Sirf hafte mein EK BAAR. Folic acid saath mein leni hai.",
            brandMrp = 45f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Autoimmune",
            janAushadhiName = "Methotrexate 2.5mg"
        ),
        Medicine(
            brandName = "Spironolactone 25",
            genericSalt = "Spironolactone",
            purposeHindi = "Yeh dil ki kami ya liver mein paani aane par di jaati hai. Potassium badhata hai.",
            howToTakeHindi = "Khane ke saath, din mein ek ya do baar.",
            brandMrp = 35f, janAushadhiMrp = 6f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Spironolactone 25mg"
        ),
        Medicine(
            brandName = "Ivermectin 12",
            genericSalt = "Ivermectin",
            purposeHindi = "Yeh keede (parasite) ke infection ke liye di jaati hai.",
            howToTakeHindi = "Khali pet, ek baar, paani ke saath.",
            brandMrp = 55f, janAushadhiMrp = 12f,
            scheduleClass = "H", category = "Antiparasitic",
            janAushadhiName = "Ivermectin 12mg"
        ),
        Medicine(
            brandName = "Rabeprazole 20",
            genericSalt = "Rabeprazole Sodium",
            purposeHindi = "Yeh pet ki acidity aur ulcer ke liye di jaati hai.",
            howToTakeHindi = "Khane se 30 minute pehle, roz subah.",
            brandMrp = 58f, janAushadhiMrp = 7f,
            scheduleClass = "H", category = "Gastro",
            janAushadhiName = "Rabeprazole 20mg"
        ),
        Medicine(
            brandName = "Esomeprazole 40",
            genericSalt = "Esomeprazole",
            purposeHindi = "Yeh pet ki tezaab ki problem ke liye di jaati hai.",
            howToTakeHindi = "Khane se 30 minute pehle, roz subah.",
            brandMrp = 88f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Gastro",
            janAushadhiName = "Esomeprazole 40mg"
        ),
        Medicine(
            brandName = "Ondansetron 4",
            genericSalt = "Ondansetron",
            purposeHindi = "Yeh ulti rokne ki goli hai. Chemotherapy ya surgery ke baad di jaati hai.",
            howToTakeHindi = "Ulti hone se pehle ya shuru hote hi, din mein 2-3 baar.",
            brandMrp = 35f, janAushadhiMrp = 7f,
            scheduleClass = "H", category = "Gastro",
            janAushadhiName = "Ondansetron 4mg"
        ),
        Medicine(
            brandName = "Lactulose Solution",
            genericSalt = "Lactulose",
            purposeHindi = "Yeh kabz ke liye di jaati hai. Aant ko naram karta hai.",
            howToTakeHindi = "15-30 ml, din mein 2-3 baar, paani mein milakar. Asar ek-do din mein aata hai.",
            brandMrp = 95f, janAushadhiMrp = 20f,
            scheduleClass = "OTC", category = "Gastro",
            janAushadhiName = "Lactulose 10gm/15ml Solution"
        ),
        Medicine(
            brandName = "Torsemide 10",
            genericSalt = "Torsemide",
            purposeHindi = "Yeh paani bahar nikalne ki dawaai hai. Dil ya kidney ki takleef mein di jaati hai.",
            howToTakeHindi = "Subah khane ke baad, ek goli.",
            brandMrp = 55f, janAushadhiMrp = 10f,
            scheduleClass = "H", category = "Cardiac",
            janAushadhiName = "Torsemide 10mg"
        ),
        Medicine(
            brandName = "Glipizide 5",
            genericSalt = "Glipizide",
            purposeHindi = "Yeh sugar ki goli hai. Pancreas se insulin badhata hai.",
            howToTakeHindi = "Khane se 30 minute pehle, din mein ek ya do baar.",
            brandMrp = 22f, janAushadhiMrp = 4f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Glipizide 5mg"
        ),
        Medicine(
            brandName = "Teneligliptin 20",
            genericSalt = "Teneligliptin",
            purposeHindi = "Yeh nayi sugar ki dawaai hai. Khana khane ke baad sugar nahi badhne deti.",
            howToTakeHindi = "Khane se pehle, din mein ek baar.",
            brandMrp = 125f, janAushadhiMrp = 28f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Teneligliptin 20mg"
        ),
        Medicine(
            brandName = "Dapagliflozin 10",
            genericSalt = "Dapagliflozin",
            purposeHindi = "Yeh nayi sugar ki dawaai hai. Sugar peshab ke raste bahar nikaalti hai. Dil bhi theek rakhti hai.",
            howToTakeHindi = "Subah naashte ke saath, ek goli roz.",
            brandMrp = 265f, janAushadhiMrp = 75f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Dapagliflozin 10mg"
        ),
        Medicine(
            brandName = "Vildagliptin 50",
            genericSalt = "Vildagliptin",
            purposeHindi = "Yeh sugar ki goli hai. Khane ke baad sugar nahi badhne deti.",
            howToTakeHindi = "Din mein do baar, subah aur raat, khane ke saath.",
            brandMrp = 185f, janAushadhiMrp = 40f,
            scheduleClass = "H", category = "Diabetes",
            janAushadhiName = "Vildagliptin 50mg"
        )
    )

    // ============================================================
    // SCAM PHRASE DATA — sourced from I4C / MHA advisories
    // ============================================================
    val scamPhrases = listOf(
        // English — High-weight phrases (weight 5 = very strong signal)
        ScamPhrase(phrase = "digital arrest", weight = 5, language = "en", reason = "I4C-documented scam type; CBI/police never conduct 'digital arrests' via phone"),
        ScamPhrase(phrase = "narcotics control bureau", weight = 4, language = "en", reason = "Frequently impersonated in I4C-documented elder fraud cases"),
        ScamPhrase(phrase = "ED summons", weight = 4, language = "en", reason = "Enforcement Directorate does not send arrest warrants via phone/message"),
        ScamPhrase(phrase = "money laundering", weight = 4, language = "en", reason = "Common phrase in Digital Arrest scripts per MHA advisory"),
        ScamPhrase(phrase = "your account will be frozen", weight = 5, language = "en", reason = "Manufactured urgency — banks do not freeze accounts with zero notice by phone"),
        ScamPhrase(phrase = "do not tell anyone", weight = 5, language = "en", reason = "Isolation tactic — primary red flag in all elder scam literature"),
        ScamPhrase(phrase = "send money immediately", weight = 5, language = "en", reason = "Time pressure tactic — strongest predictor of EFA (Elder Financial Abuse)"),
        ScamPhrase(phrase = "arrest warrant", weight = 4, language = "en", reason = "Real arrest warrants are physical documents, never digital"),
        ScamPhrase(phrase = "senior officer", weight = 2, language = "en", reason = "Authority impersonation — moderate signal, alone not enough"),
        ScamPhrase(phrase = "Supreme Court order", weight = 4, language = "en", reason = "Frequently cited in Digital Arrest scripts"),
        ScamPhrase(phrase = "FIR has been registered", weight = 4, language = "en", reason = "FIR never communicated via message/phone first"),
        ScamPhrase(phrase = "verification required urgently", weight = 3, language = "en", reason = "Urgency manufacture"),
        ScamPhrase(phrase = "international call", weight = 2, language = "en", reason = "Foreign number commonly used in scam operations"),
        ScamPhrase(phrase = "RBI clearance", weight = 4, language = "en", reason = "RBI does not call individuals to 'clear' money"),
        ScamPhrase(phrase = "TRAI action", weight = 4, language = "en", reason = "TRAI does not disconnect phones or issue arrest notices"),
        ScamPhrase(phrase = "your SIM will be blocked", weight = 4, language = "en", reason = "Telecom authority threat — I4C documented pattern"),
        ScamPhrase(phrase = "cyber crime branch", weight = 3, language = "en", reason = "Impersonation; real CC branch contacts via physical letter first"),
        ScamPhrase(phrase = "pay penalty", weight = 3, language = "en", reason = "Manufactured fine — common in extortion scripts"),
        ScamPhrase(phrase = "government refund", weight = 3, language = "en", reason = "False refund promise — phishing variant"),
        ScamPhrase(phrase = "OTP share", weight = 5, language = "en", reason = "No legitimate authority ever asks for OTP"),
        ScamPhrase(phrase = "share your bank details", weight = 5, language = "en", reason = "No legitimate authority asks for bank details by phone"),
        ScamPhrase(phrase = "video call verification", weight = 4, language = "en", reason = "Digital Arrest variant — fake officer on video call"),

        ScamPhrase(phrase = "parcel held", weight = 4, language = "en", reason = "Customs courier extortion script"),
        ScamPhrase(phrase = "illegal drugs found", weight = 5, language = "en", reason = "Narcotics courier false accusation"),
        ScamPhrase(phrase = "do not disconnect", weight = 4, language = "en", reason = "Digital arrest coercion to prevent victim calling family"),
        ScamPhrase(phrase = "electricity cut", weight = 4, language = "en", reason = "Power disconnection threat"),
        ScamPhrase(phrase = "bijli kaat", weight = 4, language = "hi", reason = "Power disconnection threat"),
        ScamPhrase(phrase = "sim band", weight = 4, language = "hi", reason = "Telecom block threat"),
        ScamPhrase(phrase = "lottery", weight = 4, language = "hi", reason = "Lottery advance fee fraud"),
        ScamPhrase(phrase = "tax jama", weight = 3, language = "hi", reason = "Advance fee tax demand"),
        ScamPhrase(phrase = "card block", weight = 4, language = "hi", reason = "Bank card block intimidation"),
        ScamPhrase(phrase = "customs officer", weight = 4, language = "en", reason = "Customs authority impersonation"),
        ScamPhrase(phrase = "KYC pending", weight = 4, language = "en", reason = "Phishing urgency for bank credentials"),
        // Hindi — High-weight phrases
        ScamPhrase(phrase = "digital giraftari", weight = 5, language = "hi", reason = "Hindi version of Digital Arrest — I4C documented term"),
        ScamPhrase(phrase = "girftaar kar lenge", weight = 5, language = "hi", reason = "Arrest threat — scam script phrase"),
        ScamPhrase(phrase = "abhi paise bhejiye", weight = 5, language = "hi", reason = "Immediate money demand"),
        ScamPhrase(phrase = "turant paise bhejiye", weight = 5, language = "hi", reason = "Urgent money demand"),
        ScamPhrase(phrase = "paise bhejiye", weight = 5, language = "hi", reason = "Immediate money demand"),
        ScamPhrase(phrase = "kisi ko mat batana", weight = 5, language = "hi", reason = "Isolation command — strongest red flag"),
        ScamPhrase(phrase = "CBI officer bol raha hoon", weight = 5, language = "hi", reason = "CBI impersonation — most common in Digital Arrest"),
        ScamPhrase(phrase = "aapka account band hoga", weight = 5, language = "hi", reason = "Account freeze threat"),
        ScamPhrase(phrase = "OTP batao", weight = 5, language = "hi", reason = "No legitimate person ever asks for OTP"),
        ScamPhrase(phrase = "darr mat", weight = 2, language = "hi", reason = "Psychological manipulation opener"),
        ScamPhrase(phrase = "court ka order hai", weight = 4, language = "hi", reason = "Court order impersonation"),
        ScamPhrase(phrase = "turant transfer karo", weight = 5, language = "hi", reason = "Urgent transfer demand"),
        ScamPhrase(phrase = "ED ka notice", weight = 4, language = "hi", reason = "Enforcement Directorate impersonation"),
        ScamPhrase(phrase = "NCB officer", weight = 4, language = "hi", reason = "Narcotics Control Bureau impersonation"),
        ScamPhrase(phrase = "jel ho sakti hai", weight = 4, language = "hi", reason = "Jail threat — scam pressure tactic"),
        ScamPhrase(phrase = "khaata freeze", weight = 5, language = "hi", reason = "Account freeze — manufactured crisis"),
        ScamPhrase(phrase = "bank account number do", weight = 5, language = "hi", reason = "Direct bank information request"),
        ScamPhrase(phrase = "paisa transfer karo warna", weight = 5, language = "hi", reason = "Ultimatum threat — strong scam signal"),
        ScamPhrase(phrase = "police aa jaayegi", weight = 4, language = "hi", reason = "Police threat tactic"),
        ScamPhrase(phrase = "ghar pe raid padegi", weight = 5, language = "hi", reason = "Home raid threat — I4C documented phrase"),
        ScamPhrase(phrase = "sarkaari notice", weight = 2, language = "hi", reason = "Government notice claim — alone not enough, but counts in total score"),
        ScamPhrase(phrase = "RBI se bol raha hoon", weight = 4, language = "hi", reason = "RBI impersonation"),
        ScamPhrase(phrase = "custom clearance", weight = 3, language = "hi", reason = "Customs/parcel scam variant"),
        ScamPhrase(phrase = "nasha baraamad", weight = 4, language = "hi", reason = "Drug seizure claim — Digital Arrest variant")
    )
}
