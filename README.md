# ✈️ TripTale

> **Pianifica, vivi e ricorda le tue avventure in giro per il mondo.** > TripTale è un'applicazione Android nativa progettata per i viaggiatori che desiderano avere il proprio itinerario e il proprio diario di viaggio sempre a portata di mano, sia online che offline.

---

## ✨ Funzionalità Principali

* 🗺️ **Gestione Viaggi:** Crea, modifica ed elimina i tuoi viaggi definendo destinazione, date e immagine di copertina.
* 📍 **Diario a Tappe:** Arricchisci ogni viaggio con tappe specifiche, aggiungendo titoli, note personali e fotografie scattate al momento.
* ☁️ **Sincronizzazione Cloud & Auth:** Sistema di Login/Registrazione sicuro tramite **Firebase Authentication**. I dati vengono sincronizzati sul cloud per non perdere mai un ricordo.
* 📴 **Offline-First:** Grazie al database locale, puoi consultare e aggiungere tappe al tuo diario anche senza connessione internet (in aereo o all'estero).
* ⛅ **Previsioni Meteo Intelligenti:** Servizio in background che recupera le previsioni meteorologiche per la tua destinazione nei giorni del viaggio.
* 📸 **Elaborazione Immagini Custom:** Modulo fotocamera integrato con ottimizzazione della memoria (compressione dinamica) e applicazione automatica di un **watermark** personalizzato sulle foto scattate.
* 🌗 **UI Moderna e Accessibile:** Interfaccia utente ottimizzata per l'uso "Edge-to-Edge", supporto nativo al Dark Theme e piena accessibilità (Screen Reader) su tutte le schermate.

---

## 🛠️ Stack Tecnologico e Architettura

L'applicazione è sviluppata interamente in **Java** seguendo le linee guida e le best practice di sviluppo Android moderno:

* **Architettura UI:** Single-Activity Architecture con **Navigation Component** e gestione del backstack.
* **Database Locale:** **Room Persistence Library** (SQLite astratto) combinato con operazioni asincrone multi-thread.
* **Backend as a Service (BaaS):** **Firebase** (Auth e Realtime Database/Firestore).
* **Background Tasks:** Schedulazione dei servizi meteo tramite **JobScheduler**.
* **Gestione Permessi e Media:** Implementazione della moderna **Activity Result API** per l'acquisizione sicura di immagini dalla fotocamera e dalla galleria (`ACTION_OPEN_DOCUMENT` con permessi persistenti URI).

---

## ⚙️ Installazione e Configurazione

Se desideri clonare e compilare questo progetto localmente, segui questi passaggi:

### Prerequisiti
* **Android Studio** (versione Iguana o superiore consigliata).
* **SDK Android** (Min API 26 - Target API 34+).

### Passaggi

1. **Clona la repository:**
   ```bash
   git clone [https://github.com/tuo-username/TripTale.git](https://github.com/tuo-username/TripTale.git)
Apri il progetto:
Apri Android Studio e seleziona File > Open, quindi naviga fino alla cartella clonata.

Configura Firebase:

Crea un progetto su Firebase Console.

Aggiungi un'app Android registrando il package name (com.example.triptale).

Scarica il file google-services.json e inseriscilo nella cartella app/ del progetto.

Abilita l'autenticazione via Email/Password in Firebase.

Configura l'API Meteo (Opzionale):

Nota: Assicurati di inserire la tua chiave API per il meteo all'interno di MeteoManager.java o local.properties.

Compila e avvia:
Sincronizza Gradle e premi Run per testare l'app su un emulatore o un dispositivo fisico.

📂 Struttura del Progetto
Una panoramica dei pacchetti principali:

ui/ : Contiene la MainActivity e tutti i Fragment (Login, Home, Dettaglio, Aggiunta/Modifica).

model/ : Entità dati (POJO) come Viaggio e Tappa utilizzate da Room e Firebase.

database/ : Configurazione del database locale AppDatabase e relative interfacce DAO.

network/ : Logica di rete, inclusi FirebaseManager, chiamate API (MeteoManager) e servizi in background (MeteoJobService).

utils/ : Classi di utilità stateless (DateUtils, ImageUtils) per validazioni, gestione calendari ed elaborazione immagini (watermark).

🚀 Sviluppi Futuri (To-Do)
[ ] Implementazione del Deep Learning (CNN) per l'auto-categorizzazione delle foto (es. mare, montagna, città).

[ ] Esportazione del diario di viaggio in formato PDF.

[ ] Condivisione dei viaggi con altri utenti dell'app.

👨‍💻 Autore
[Il Tuo Nome e Cognome]

GitHub: @tuo-username

LinkedIn: Il Tuo Profilo

Progetto sviluppato per finalità didattiche / Esame universitario.
