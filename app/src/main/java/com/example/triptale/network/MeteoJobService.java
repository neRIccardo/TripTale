package com.example.triptale.network;
import android.Manifest;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavDeepLinkBuilder;
import com.example.triptale.database.AppDatabase;
import com.example.triptale.R;
import com.example.triptale.model.Viaggio;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * Servizio in background schedulato dal sistema operativo tramite JobScheduler.
 * Si risveglia periodicamente per controllare lo stato dei viaggi locali e inviare
 * una notifica push se un viaggio è in partenza il giorno successivo o è attualmente in corso,
 * allegando le previsioni meteo scaricate in tempo reale.
 */
public class MeteoJobService extends JobService {

    /**
     * Punto di ingresso del Job. Poiché i JobService girano di default sul Main Thread,
     * questo metodo avvia immediatamente un Thread separato per non bloccare l'interfaccia utente.
     * Utilizza un CountDownLatch per sincronizzare le chiamate di rete asincrone (Volley)
     * prima di dichiarare il lavoro concluso.
     *
     * @param params I parametri forniti dal JobScheduler.
     * @return true perché il lavoro principale è delegato a un Thread asincrono e non è finito subito.
     */
    @Override
    public boolean onStartJob(JobParameters params) {
        // Il lavoro del JobService parte qui
        // Creiamo un thread perché il JobService di default gira sul Main Thread
        new Thread(() -> {
            List<Viaggio> viaggi = AppDatabase.getInstance(getApplicationContext()).viaggioDao().ottieniViaggi();

            // Prepariamo i calendari per capire che giorno è oggi e che giorno è domani
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar calOggi = Calendar.getInstance();
            // Resettiamo ore, minuti e secondi per fare un confronto pulito sulle date
            calOggi.set(Calendar.HOUR_OF_DAY, 0);
            calOggi.set(Calendar.MINUTE, 0);
            calOggi.set(Calendar.SECOND, 0);
            calOggi.set(Calendar.MILLISECOND, 0);

            Calendar calDomani = (Calendar) calOggi.clone();
            calDomani.add(Calendar.DAY_OF_YEAR, 1);

            for (Viaggio v : viaggi) {
                try {
                    Date dataInizio = sdf.parse(v.dataInizio);
                    Date dataFine = sdf.parse(v.dataFine);

                    // LOGICA DELLE NOTIFICHE: Il viaggio parte domani? Oppure è in corso oggi?
                    boolean parteDomani = calDomani.getTime().equals(dataInizio);
                    boolean inCorso = (calOggi.getTime().equals(dataInizio) || calOggi.getTime().after(dataInizio)) &&
                            (calOggi.getTime().equals(dataFine) || calOggi.getTime().before(dataFine));

                    if (parteDomani || inCorso) {
                        String tipoAvviso = parteDomani ?
                                getString(R.string.avviso_partenza_domani, v.titolo) :
                                getString(R.string.avviso_viaggio_in_corso, v.titolo);

                        // Semaforo per dire al Worker di aspettare la risposta di Volley prima di spegnersi
                        CountDownLatch latch = new CountDownLatch(1);

                        MeteoManager.ottieniPrevisioni(getApplicationContext(), v.cittaDestinazione, v.dataInizio, v.dataFine, new MeteoManager.MeteoCallback() {
                            @Override
                            public void onSuccess(List<MeteoManager.Previsione> previsioni) {
                                String meteoTesto = "";
                                if (!previsioni.isEmpty()) {
                                    if(parteDomani) {
                                        // Prendiamo la previsione per domani
                                        meteoTesto = getString(R.string.meteo_domani, previsioni.get(0).iconaEmoji, Math.round(previsioni.get(0).tempMin), Math.round(previsioni.get(0).tempMax));
                                    }
                                    else {
                                        // Prendiamo la previsione di oggi
                                        meteoTesto = getString(R.string.meteo_oggi, previsioni.get(0).iconaEmoji, Math.round(previsioni.get(0).tempMin), Math.round(previsioni.get(0).tempMax));
                                    }
                                }
                                inviaNotifica(v, tipoAvviso + meteoTesto);
                                latch.countDown(); // Diamo il semaforo verde per proseguire
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // Se manca internet o c'è un errore, mandiamo la notifica senza meteo
                                inviaNotifica(v, tipoAvviso + getString(R.string.meteo_non_disponibile));
                                latch.countDown(); // Diamo il semaforo verde per proseguire
                            }
                        });

                        // Il Thread si "congela" in questa riga finché il semaforo non viene rilasciato da Volley
                        latch.await();
                    }
                } catch (Exception e) {
                    Log.e("TripTale", "Si è verificato un errore", e);
                }
            }
            // Finito il ciclo sui viaggi, diciamo al sistema che abbiamo concluso il lavoro
            jobFinished(params, false);
        }).start();

        // Restituiamo true perché il lavoro sta continuando in modo asincrono nel Thread
        return true;
    }

    /**
     * Metodo chiamato quando il sistema cancella il job.
     *
     * @param params I parametri forniti dal JobScheduler.
     * @return true se il sistema deve riprovare a rilanciare il lavoro, false altrimenti.
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        // Se il sistema cancella il job prima del previsto, restituiamo true per riprovare dopo
        return true;
    }

    /**
     * Costruisce e pubblica una notifica di sistema locale.
     * Imposta un PendingIntent e un DeepLink per far sì che, al tocco della notifica,
     * l'applicazione si apra direttamente all'interno del Dettaglio del Viaggio specifico.
     *
     * @param viaggio Il viaggio a cui si riferisce la notifica.
     * @param testoDellaNotifica Il messaggio testuale da mostrare all'utente (es. meteo o avviso).
     */
    private void inviaNotifica(Viaggio viaggio, String testoDellaNotifica) {
        // Controlliamo per sicurezza di avere i permessi
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Se l'utente clicca la notifica, apriamo l'app
        Bundle bundle = new Bundle();
        bundle.putParcelable("viaggio_selezionato", viaggio);

        PendingIntent pendingIntent = new NavDeepLinkBuilder(getApplicationContext())
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.dettaglioViaggioFragment)
                .setArguments(bundle)
                .createPendingIntent();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "CANALE_VIAGGI")
                .setSmallIcon(R.mipmap.triptale)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentTitle(getString(R.string.app_name_notifica))
                .setContentText(testoDellaNotifica)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // La notifica sparisce se ci clicchi
                .setContentIntent(pendingIntent); // Colleghiamo il click all'apertura del viaggio specifico

        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
        manager.notify(viaggio.id, builder.build());
    }
}