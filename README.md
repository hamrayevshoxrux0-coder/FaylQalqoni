# Fayl Qalqoni (FaylQalqoni)

"To'ydan arxiv" uslubidagi (ikki karra kengaytmali, `.jpeg.apk` kabi) fayl-firibgarligi va josuslik APK'laridan himoya qiluvchi Android ilovasi.

## Ilova nima qiladi

1. **Fayl nomi firibgarligini aniqlaydi** — `rasm.jpeg.apk`, `hujjat.pdf.apk` kabi ikki karra kengaytmali fayl nomlarini avtomatik belgilaydi.
2. **Fayl ichini tekshiradi ("magic bytes")** — fayl kengaytmasi ".jpg" bo'lsa-da, uning ichki tarkibi aslida APK/EXE bo'lsa, buni aniqlaydi (kengaytmaga emas, haqiqiy tarkibga ishonadi).
3. **Ruxsatlarni tahlil qiladi** — APK so'ragan ruxsatlar kombinatsiyasini baholaydi (masalan SMS o'qish+yuborish, Accessibility+overlay, bildirishnomalarni o'qish va h.k. — bularning har biri OTP/bank ma'lumotlarini o'g'irlashning tipik usullari).
4. **Signature (imzo) bazasi** — `assets/malware_signatures.json` faylida ma'lum zararli fayllarning SHA-256 xeshlari saqlanadi. Sizga yuborilgan namunaviy "to'ydan arxivi" faylining xeshi allaqachon shu bazaga qo'shilgan.
5. **Real vaqtda monitoring** — fon xizmati (`FileMonitorService`) Yuklab olishlar, WhatsApp va Telegram papkalarini kuzatib turadi va yangi fayl tushishi bilanoq tekshiradi.
6. **Karantin** — xavfli deb topilgan fayllar darhol o'chirilmaydi, balki ilovaning ichki xavfsiz papkasiga ko'chiriladi (asl joyidan olib tashlanadi), shu bilan bexosdan ishlab ketishining oldi olinadi.
7. **Havola (link) tekshiruvi** — Telegram/WhatsApp/brauzerdagi shubhali linkni ochishdan oldin tekshirish mumkin:
   - **"Ulashish" (Share) orqali** — linkni to'g'ridan-to'g'ri ochmasdan, "Ulashish" tugmasi orqali "Fayl Qalqoni"ga yuborsangiz, natija darhol ko'rsatiladi.
   - **Qo'lda kiritib** — asosiy ekrandagi "Havolani tekshirish" maydoniga linkni joylashtirib, "Tekshirish"ni bosish orqali.
   - Tekshiruv ikki bosqichda ishlaydi: (a) tarmoqsiz, tezkor **lokal qoidalar** (IP-manzil domenlar, brendga taqlid qiluvchi domenlar — masalan "whatsapp-update.xyz", qisqartirilgan havolalar, punycode/IDN domenlar va h.k.), va (b) ixtiyoriy ravishda **Google Safe Browsing** bazasi (Sozlamalar ekranida bepul API kalit kiritilsa faollashadi).

## APK olish — Android Studio'siz (tavsiya etiladi)

Agar kompyuteringizga Android Studio o'rnatib bo'lmasa (joy yetmasa, internet sekin bo'lsa yoki kompyuter umuman bo'lmasa), **GitHub'ning bepul xizmati orqali** tayyor `.apk` faylini olishingiz mumkin — sizning tomoningizda HECH NARSA o'rnatilmaydi, hammasi GitHub'ning o'z serverida quriladi. Kerak bo'ladigani — istalgan brauzer (hatto telefonda ham).

1. [github.com](https://github.com) saytida bepul hisob oching (agar yo'q bo'lsa).
2. "New repository" tugmasi orqali yangi (masalan `FaylQalqoni` nomli) repozitoriy yarating — "Public" yoki "Private" farqi yo'q, README qo'shmasdan yarating.
3. Yuborilgan `FaylQalqoni.zip` faylini kompyuteringizda (yoki telefon fayl menejerida) yeching.
4. Repozitoriy sahifasida "Add file" → "Upload files" tugmasini bosing, so'ng yechilgan `FaylQalqoni` papkasi ICHIDAGI barcha fayl va papkalarni (jumladan yashirin `.github` papkasini ham) brauzerga sudrab tashlang (kompyuterda Chrome/Edge papkani ham qabul qiladi). "Commit changes" tugmasini bosing.
5. Repozitoriyning "Actions" bo'limiga o'ting — "APK qurish" ishi avtomatik boshlanadi (agar boshlanmasa, chapdagi workflow nomini bosib, "Run workflow"ni tanlang).
6. 3–6 daqiqa kutgach (yashil ✅ belgi chiqqach), o'sha run ichiga kiring va pastdagi "Artifacts" bo'limidan **FaylQalqoni-debug-apk** faylini yuklab oling — ichida tayyor `.apk` bor.
7. `.apk` faylini telefoningizga ko'chiring, fayl menejerida oching va o'rnating (birinchi marta "noma'lum manbalardan o'rnatishga ruxsat berish" so'raladi — shuni yoqing).

Eslatma: telefon brauzerida ko'p faylni papka tuzilishi bilan birga yuklash noqulay bo'lishi mumkin — iloji bo'lsa, shu bir martalik yuklash uchun istalgan (hatto eski/qarz) kompyuter brauzeridan foydalaning, chunki bunda hech qanday dastur o'rnatish shart emas.

## Loyihani Android Studio'da ochish (agar imkon bo'lsa)

1. [Android Studio](https://developer.android.com/studio) (Koala yoki yangiroq versiya) o'rnating.
2. `FaylQalqoni` papkasini "Open" orqali Android Studio'da oching (Gradle avtomatik sinxronlanadi).
3. Kamida bitta Android qurilma yoki emulyator ulang (minSdk 26 — Android 8.0+).
4. "Run" tugmasini bosing.

## Birinchi ishga tushirishda kerakli ruxsatlar

- **Bildirishnomalar** — xavf aniqlanganda ogohlantirish ko'rsatish uchun (Android 13+).
- **Barcha fayllarga kirish** (`MANAGE_EXTERNAL_STORAGE`) — ilovaning asosiy ekranidagi "Ruxsat berish" tugmasi orqali Sozlamalar oynasi ochiladi. Bu ruxsatsiz ilova fayllarni to'liq skanerlay olmaydi.

## Muhim cheklovlar (halol aytilishi kerak)

- Bu **shaxsiy/tashkiliy foydalanish uchun mo'ljallangan yordamchi vosita**, professional antivirus kompaniyalarining (millionlab yozuvli, doimiy yangilanadigan bulutli bazalarga ega) mahsulotlarini to'liq almashtirmaydi.
- Signature-baza faqat siz qo'shgan/ma'lum bo'lgan xeshlarni taniydi. Yangi, hali ko'rilmagan zararli fayllarni faqat xulq-atvor (kengaytma firibgarligi + ruxsatlar tahlili) orqali aniqlaydi — bu 100% kafolat bermaydi.
- Android 11+ da `MANAGE_EXTERNAL_STORAGE` ruxsatisiz ilova faqat o'zi yaratgan yoki media-skaner ko'radigan fayllarni ko'ra oladi.
- Play Store'ga joylashtirish uchun `QUERY_ALL_PACKAGES` va `MANAGE_EXTERNAL_STORAGE` ruxsatlari alohida asoslash (Play Console Policy) talab qiladi — bu ilova hozircha shaxsiy/ichki foydalanish (APK faylini to'g'ridan-to'g'ri o'rnatish) uchun mo'ljallangan.

## Signature bazasini yangilash

`app/src/main/assets/malware_signatures.json` faylini oching va quyidagi formatda yozuv qo'shing:

```json
{
  "sha256": "faylning-64-belgili-sha256-xeshi",
  "name": "Qisqa tavsif"
}
```

Fayl xeshini kompyuterda tekshirish uchun (Linux/macOS terminalda):

```
sha256sum fayl_nomi.apk
```

## Loyiha tuzilishi

```
app/src/main/java/uz/faylqalqoni/shield/
  core/          — aniqlash mantig'i (ScanEngine, FileTypeSniffer, SignatureDatabase, PermissionRiskAnalyzer)
  service/       — fon monitoringi (FileMonitorService, RecursiveFileObserver, receiver'lar)
  quarantine/    — karantin boshqaruvi
  notification/  — bildirishnomalar
  ui/            — RecyclerView adapterlari
  MainActivity.kt, QuarantineActivity.kt — ekranlar
```
