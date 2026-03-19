package com.example.triptale.ui;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.triptale.R;
import com.example.triptale.network.MeteoJobService;

/**
 * Activity principale dell'applicazione.
 * Si occupa del setup globale iniziale al lancio dell'app: gestione dell'interfaccia a tutto schermo,
 * configurazione dei canali di notifica, richiesta dei permessi a runtime e schedulazione dei servizi in background.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Metodo del ciclo di vita chiamato alla creazione dell'Activity.
     * Inizializza l'interfaccia grafica e lancia in sequenza tutti i controlli di sicurezza
     * e configurazione necessari per il corretto funzionamento delle funzionalità avanzate (notifiche e meteo).
     *
     * @param savedInstanceState L'eventuale stato precedentemente salvato dell'Activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- SETUP NOTIFICHE ---
        creaCanaleNotifiche();
        chiediPermessoNotifiche();

        programmaControlloGiornaliero();
    }

    /**
     * Inizializza il Notification Channel denominato "CANALE_VIAGGI".
     * A partire da Android 8.0 (API 26), tutte le notifiche devono obbligatoriamente
     * appartenere a un canale specifico per permettere all'utente di gestirne le priorità.
     * Il canale viene impostato con priorità alta e vibrazione per catturare l'attenzione dell'utente.
     */
    private void creaCanaleNotifiche() {
        // I canali servono solo da Android Oreo (API 26) in poi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String idCanale = "CANALE_VIAGGI";
            String nomeCanale = getString(R.string.canale_meteo_nome);
            String descrizione = getString(R.string.canale_meteo_descrizione);

            // IMPORTANCE_HIGH serve per far apparire il "fumetto" a comparsa in cima allo schermo
            int importanza = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel canale = new NotificationChannel(idCanale, nomeCanale, importanza);
            canale.setDescription(descrizione);

            // Aggiungiamo anche la vibrazione per essere sicuri che l'utente lo noti
            canale.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(canale);
            }
        }
    }

    /**
     * Gestisce la richiesta a runtime per il permesso di invio notifiche push.
     * Necessario a partire da Android 13 (API 33 - Tiramisu). Se il permesso non è ancora
     * stato garantito, mostra il popup di sistema per la richiesta di autorizzazione.
     */
    private void chiediPermessoNotifiche() {
        // Il permesso esplicito serve solo da Android Tiramisu (API 33) in poi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Controlliamo se l'utente ci ha già dato il permesso
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Se non ce l'ha dato, facciamo apparire il popup di sistema
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    /**
     * Registra il MeteoJobService all'interno del JobScheduler di sistema.
     * Effettua un controllo preventivo per evitare ri-programmazioni multiple dello stesso task.
     * Il servizio viene schedulato per un'esecuzione periodica ed è contrassegnato come
     * persistente, garantendo che sopravviva anche in caso di riavvio completo del dispositivo.
     */
    private void programmaControlloGiornaliero() {
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) return;

        // Controlliamo se il lavoro con ID 1001 è già stato programmato
        boolean giaProgrammato = false;
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == 1001) {
                giaProgrammato = true;
                break;
            }
        }

        // Se è già in coda, ci fermiamo qui e non lo riprogrammiamo
        if (giaProgrammato) {
            return;
        }

        // Se invece non esiste (es. la prima volta che apriamo l'app), lo creiamo
        ComponentName serviceComponent = new ComponentName(this, MeteoJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(1001, serviceComponent);

        // Esegui ogni 15 minuti (il minimo consentito da Android)
        builder.setPeriodic(15 * 60 * 1000L);

        // Sopravvivi al riavvio del telefono
        builder.setPersisted(true);

        // Mettiamo in coda il lavoro
        jobScheduler.schedule(builder.build());
    }
}