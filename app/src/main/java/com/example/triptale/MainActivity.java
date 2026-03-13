package com.example.triptale;
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
    // PROGRAMMAZIONE DEL CONTROLLO GIORNALIERO (Ogni 24 ore in background)
    // =========================================================================
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