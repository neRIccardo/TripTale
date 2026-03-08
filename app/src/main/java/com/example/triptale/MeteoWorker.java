package com.example.triptale;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class MeteoWorker extends Worker {

    public MeteoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<Viaggio> viaggi = AppDatabase.getInstance(getApplicationContext()).viaggioDao().ottieniViaggi();

        // Prepariamo i calendari per capire che giorno è oggi e che giorno è domani
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar calOggi = Calendar.getInstance();
        // Resettiamo ore, minuti e secondi per fare un confronto pulito sulle date
        calOggi.set(Calendar.HOUR_OF_DAY, 0); calOggi.set(Calendar.MINUTE, 0); calOggi.set(Calendar.SECOND, 0); calOggi.set(Calendar.MILLISECOND, 0);

        Calendar calDomani = (Calendar) calOggi.clone();
        calDomani.add(Calendar.DAY_OF_YEAR, 1);

        for (Viaggio v : viaggi) {
            try {
                Date dataInizio = sdf.parse(v.dataInizio);
                Date dataFine = sdf.parse(v.dataFine);

                // LOGICA DELLE NOTIFICHE: Il viaggio parte domani? Oppure è in corso oggi?
                boolean parteDomani = dataInizio.equals(calDomani.getTime());
                boolean inCorso = (calOggi.getTime().equals(dataInizio) || calOggi.getTime().after(dataInizio)) &&
                        (calOggi.getTime().equals(dataFine) || calOggi.getTime().before(dataFine));

                if (parteDomani || inCorso) {
                    String tipoAvviso = parteDomani ? "Domani avrà inizio il viaggio " : "Goditi il viaggio ";

                    // Semaforo per dire al Worker di aspettare 1 risposta prima di spegnersi
                    CountDownLatch latch = new CountDownLatch(1);

                    MeteoManager.ottieniPrevisioni(v.cittaDestinazione, v.dataInizio, v.dataFine, new MeteoManager.MeteoCallback() {
                        @Override
                        public void onSuccess(List<MeteoManager.Previsione> previsioni) {
                            String meteoTesto = "";
                            if (!previsioni.isEmpty()) {
                                if(parteDomani) {
                                    // Prendiamo la previsione per domani
                                    meteoTesto = "\nMeteo di domani:  " + previsioni.get(0).iconaEmoji + " " + Math.round(previsioni.get(0).tempMin) + "/" + Math.round(previsioni.get(0).tempMax) + "°C";
                                }
                                else {
                                    // Prendiamo la previsione di oggi
                                    meteoTesto = "\nMeteo di oggi:  " + previsioni.get(0).iconaEmoji + " " + Math.round(previsioni.get(0).tempMin) + "/" + Math.round(previsioni.get(0).tempMax) + "°C";
                                }
                            }
                            inviaNotifica(v, tipoAvviso + "\"" + v.titolo + "\"" + "!" + meteoTesto);
                            latch.countDown();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Se manca internet, mandiamo la notifica senza meteo
                            inviaNotifica(v, tipoAvviso + "\"" + v.titolo + "\"" + "!\n(Meteo non disponibile)");
                            latch.countDown();
                        }
                    });

                    // Il Worker si "congela" in questa riga finché il semaforo non viene rilasciato
                    latch.await();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Result.success();
    }

    // =========================================================================
    // METODO PER COSTRUIRE E LANCIARE LA NOTIFICA A SCHERMO
    // =========================================================================
    private void inviaNotifica(Viaggio viaggio, String testoDellaNotifica) {
        // Controlliamo per sicurezza di avere i permessi
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Se l'utente clicca la notifica, apriamo l'app
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putSerializable("viaggio_selezionato", viaggio);

        PendingIntent pendingIntent = new androidx.navigation.NavDeepLinkBuilder(getApplicationContext())
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.dettaglioViaggioFragment)
                .setArguments(bundle)
                .createPendingIntent();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "CANALE_VIAGGI")
                .setSmallIcon(android.R.drawable.ic_dialog_map) //TODO Sostituire con l'icona della tua app
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentTitle("TripTale ✈️")
                .setContentText(testoDellaNotifica)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // La notifica sparisce se ci clicchi
                .setContentIntent(pendingIntent); // Colleghiamo il click all'apertura del viaggio specifico

        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
        manager.notify(viaggio.id, builder.build());
    }
}