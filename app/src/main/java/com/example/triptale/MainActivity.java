package com.example.triptale;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

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

    // =========================================================================
    // CREAZIONE DEL CANALE DI NOTIFICA
    // =========================================================================
    private void creaCanaleNotifiche() {
        // I canali servono solo da Android Oreo (API 26) in poi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String idCanale = "CANALE_VIAGGI";
            String nomeCanale = "Promemoria e meteo viaggi";
            String descrizione = "Avvisi sulle partenze imminenti e il meteo giornaliero";

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

    // =========================================================================
    // RICHIESTA PERMESSO A SCHERMO
    // =========================================================================
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

    // =========================================================================
    // PROGRAMMAZIONE DEL WORKMANAGER (Ogni 24 ore in background)
    // =========================================================================
    private void programmaControlloGiornaliero() {
        // Creiamo un lavoro periodico che scatta ogni 24 ore
        PeriodicWorkRequest lavoroMeteo = new PeriodicWorkRequest.Builder(
                MeteoWorker.class,
                15, TimeUnit.MINUTES
        ).build();

        // Diciamo ad Android di metterlo in coda.
        // Usiamo "enqueueUniquePeriodicWork" per evitare che aprendo l'app più volte
        // si creino tanti lavoratori diversi che mandano notifiche doppie
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ControlloMeteoViaggi", // "nome" di questo specifico lavoratore
                ExistingPeriodicWorkPolicy.KEEP, // KEEP = Se c'è già un lavoratore con questo nome, lascialo lavorare e non riavviarlo
                lavoroMeteo
        );
    }
}