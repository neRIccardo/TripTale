package com.example.triptale.network;
import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.triptale.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Classe di utilità per la gestione delle chiamate di rete alle API di Open-Meteo.
 * Utilizza la libreria Volley per eseguire richieste asincrone (prima per il Geocoding
 * e poi per le previsioni meteo) e restituisce i risultati tramite un'interfaccia di callback.
 */
public class MeteoManager {

    /**
     * Struttura dati che rappresenta la previsione meteorologica per un singolo giorno.
     * Incapsula la data, le temperature minime e massime, e l'icona tradotta in formato Emoji.
     */
    public static class Previsione {
        public String data;
        public double tempMax;
        public double tempMin;
        public String iconaEmoji;

        /**
         * Costruttore della classe Previsione.
         *
         * @param data Data della previsione.
         * @param tempMax Temperatura massima prevista.
         * @param tempMin Temperatura minima prevista.
         * @param iconaEmoji Emoji corrispondente all'icona meteo.
         */
        public Previsione(String data, double tempMax, double tempMin, String iconaEmoji) {
            this.data = data;
            this.tempMax = tempMax;
            this.tempMin = tempMin;
            this.iconaEmoji = iconaEmoji;
        }
    }

    /**
     * Interfaccia di comunicazione asincrona.
     * Permette al chiamante (es. un Fragment) di rimanere in ascolto e ricevere
     * i dati meteo solo quando la chiamata di rete è terminata con successo o in errore.
     */
    public interface MeteoCallback {

        /**
         * Metodo chiamato quando la chiamata di rete è terminata con successo.
         *
         * @param previsioni Lista di previsioni meteo trovate.
         */
        void onSuccess(List<Previsione> previsioni);

        /**
         * Metodo chiamato quando la chiamata di rete è terminata in errore.
         *
         * @param errorMessage Messaggio di errore generato dalla chiamata di rete.
         */
        void onError(String errorMessage);
    }

    /**
     * Calcola la finestra temporale valida per la richiesta API.
     * Gestisce i limiti fisici del provider (max 14 giorni da oggi) e i limiti
     * dell'interfaccia grafica (max 7 giorni visualizzabili), garantendo che la chiamata
     * non vada mai in errore per date troppo lontane.
     *
     * @param dataInizio La data di partenza del viaggio.
     * @param dataFine La data di ritorno del viaggio.
     * @return Un array di due stringhe [Data Inizio Formattata, Data Fine Formattata],
     * oppure null se il viaggio è interamente oltre il limite dei 14 giorni.
     */
    private static String[] calcolaDateApi(String dataInizio, String dataFine) {
        try {
            SimpleDateFormat formattaIn = new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY);
            SimpleDateFormat formattaOut = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

            Date inizio = formattaIn.parse(dataInizio);
            Date fine = formattaIn.parse(dataFine);

            // Calcoliamo "Oggi + 14 giorni" (limite fisico di Open-Meteo)
            Calendar calOggi = Calendar.getInstance();
            calOggi.add(Calendar.DAY_OF_YEAR, 14);
            Date limiteApi = calOggi.getTime();

            // Se il viaggio INIZIA oltre i 14 giorni da oggi, fermiamo tutto subito
            if (inizio != null && inizio.after(limiteApi)) {
                return null;
            }

            // Calcoliamo "Inizio + 6 giorni" (limite grafico per avere max 7 quadratini)
            Calendar calInizio = Calendar.getInstance();
            if (inizio != null) {
                calInizio.setTime(inizio);
            }
            calInizio.add(Calendar.DAY_OF_YEAR, 6);
            Date limiteGrafico = calInizio.getTime();

            // Scegliamo la data di fine corretta. Deve essere la PIÙ PICCOLA tra:
            // - La vera fine del viaggio
            // - Il limite grafico dei 7 giorni
            // - Il limite API dei 14 giorni da oggi
            Date fineEffettiva = fine;

            if (fineEffettiva != null && fineEffettiva.after(limiteGrafico)) {
                fineEffettiva = limiteGrafico;
            }
            if (fineEffettiva != null && fineEffettiva.after(limiteApi)) {
                fineEffettiva = limiteApi;
            }
            if (inizio != null) {
                if (fineEffettiva != null) {
                    return new String[]{formattaOut.format(inizio), formattaOut.format(fineEffettiva)};
                }
            }
        } catch (Exception e) {
            return null; // In caso di altri errori
        }
        return new String[0]; 
    }

    /**
     * Esegue due chiamate di rete in sequenza: prima converte il nome della città in coordinate
     * spaziali (Geocoding), poi utilizza latitudine e longitudine per scaricare le previsioni.
     *
     * @param context Il contesto, utile per accedere alle risorse e creare la coda Volley.
     * @param citta Il nome della città di destinazione.
     * @param dataInizio La data di partenza.
     * @param dataFine La data di ritorno.
     * @param callback L'interfaccia su cui ritornare i risultati (onSuccess) o gli errori (onError).
     */
    public static void ottieniPrevisioni(Context context, String citta, String dataInizio, String dataFine, MeteoCallback callback) {
        if (citta == null || citta.trim().isEmpty()) {
            callback.onError("Nome città non valido");
            return;
        }

        // Creiamo la coda di richieste Volley usando l'Application Context per evitare Memory Leaks
        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());

        // --- FASE 1: GEOCODING ---
        String urlGeocoding = "https://geocoding-api.open-meteo.com/v1/search?name=" + citta.replace(" ", "+") + "&count=1&language=it";

        StringRequest geoRequest = new StringRequest(Request.Method.GET, urlGeocoding,
                geoResponse -> {
                    try {
                        JSONObject jsonGeo = new JSONObject(geoResponse);

                        if (!jsonGeo.has("results")) {
                            callback.onError(context.getString(R.string.errore_citta_non_trovata));
                            return;
                        }

                        JSONObject cittaResult = jsonGeo.getJSONArray("results").getJSONObject(0);
                        double lat = cittaResult.getDouble("latitude");
                        double lon = cittaResult.getDouble("longitude");

                        // Calcoliamo le date
                        String[] dateApi = calcolaDateApi(dataInizio, dataFine);

                        // Se dateApi è nullo, significa che il viaggio è tutto oltre i 14 giorni
                        if (dateApi == null) {
                            callback.onError(context.getString(R.string.errore_meteo_14_giorni));
                            return;
                        }

                        // --- FASE 2: FORECAST METEO ---
                        String urlMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto&start_date=" + dateApi[0] + "&end_date=" + dateApi[1];

                        StringRequest meteoRequest = new StringRequest(Request.Method.GET, urlMeteo,
                                meteoResponse -> {
                                    try {
                                        JSONObject jsonMeteo = new JSONObject(meteoResponse);
                                        JSONObject daily = jsonMeteo.getJSONObject("daily");

                                        JSONArray timeArray = daily.getJSONArray("time");
                                        JSONArray maxTempArray = daily.getJSONArray("temperature_2m_max");
                                        JSONArray minTempArray = daily.getJSONArray("temperature_2m_min");
                                        JSONArray codeArray = daily.getJSONArray("weather_code");

                                        List<Previsione> listaPrevisioni = new ArrayList<>();
                                        for (int i = 0; i < timeArray.length(); i++) {
                                            String dataGrezza = timeArray.getString(i);
                                            String dataCorta = dataGrezza.substring(8, 10) + "/" + dataGrezza.substring(5, 7);

                                            double max = maxTempArray.getDouble(i);
                                            double min = minTempArray.getDouble(i);
                                            int code = codeArray.getInt(i);

                                            listaPrevisioni.add(new Previsione(dataCorta, max, min, traduciCodiceInEmoji(code)));
                                        }
                                        callback.onSuccess(listaPrevisioni);
                                    } catch (Exception e) {
                                        callback.onError(context.getString(R.string.errore_lettura_meteo));
                                    }
                                },
                                error -> callback.onError(context.getString(R.string.errore_connessione_meteo))
                        );

                        // Aggiungiamo la richiesta del meteo alla coda di Volley
                        queue.add(meteoRequest);

                    } catch (Exception e) {
                        callback.onError(context.getString(R.string.errore_geolocalizzazione));
                    }
                },
                error -> callback.onError(context.getString(R.string.errore_connessione_meteo))
        );

        // Aggiungiamo la prima richiesta alla coda di Volley
        queue.add(geoRequest);
    }

    /**
     * Traduce un codice meteo in un emoji corrispondente.
     * Basato sui WMO (World Meteorological Organization) codes.
     *
     * @param code Il codice meteo da tradurre.
     * @return L'emoji corrispondente al codice, oppure "?" se sconosciuto.
     */
    private static String traduciCodiceInEmoji(int code) {
        if (code == 0) return "☀️"; // Sereno
        if (code == 1 || code == 2 || code == 3) return "⛅"; // Nuvoloso
        if (code == 45 || code == 48) return "🌫️"; // Nebbia
        if (code >= 51 && code <= 67) return "🌧️"; // Pioggia
        if (code >= 71 && code <= 77) return "❄️"; // Neve
        if (code >= 80 && code <= 82) return "🌦️"; // Acquazzoni
        if (code >= 85 && code <= 86) return "🌨️"; // Nevicate
        if (code >= 95 && code <= 99) return "⛈️"; // Temporale
        return "❓"; // Sconosciuto
    }
}