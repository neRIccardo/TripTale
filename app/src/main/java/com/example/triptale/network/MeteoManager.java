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

public class MeteoManager {

    // =========================================================================
    // CLASSE PER "IMPACCHETTARE" I DATI DI UN SINGOLO GIORNO
    // =========================================================================
    public static class Previsione {
        public String data;
        public double tempMax;
        public double tempMin;
        public String iconaEmoji;

        public Previsione(String data, double tempMax, double tempMin, String iconaEmoji) {
            this.data = data;
            this.tempMax = tempMax;
            this.tempMin = tempMin;
            this.iconaEmoji = iconaEmoji;
        }
    }

    // ===============================================================================
    // INTERFACCIA USATA DA DettaglioViaggioFragment PER GESTIRE LE RISPOSTE ALLE API
    // ===============================================================================
    public interface MeteoCallback {
        void onSuccess(List<Previsione> previsioni);
        void onError(String errorMessage);
    }

    // =========================================================================
    // METODO PER CALCOLARE LE DATE CORRETTE PER L'API
    // =========================================================================
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
            if (inizio.after(limiteApi)) {
                return null;
            }

            // Calcoliamo "Inizio + 6 giorni" (limite grafico per avere max 7 quadratini)
            Calendar calInizio = Calendar.getInstance();
            calInizio.setTime(inizio);
            calInizio.add(Calendar.DAY_OF_YEAR, 6);
            Date limiteGrafico = calInizio.getTime();

            // Scegliamo la data di fine corretta. Deve essere la PIÙ PICCOLA tra:
            // - La vera fine del viaggio
            // - Il limite grafico dei 7 giorni
            // - Il limite API dei 14 giorni da oggi
            Date fineEffettiva = fine;

            if (fineEffettiva.after(limiteGrafico)) {
                fineEffettiva = limiteGrafico;
            }
            if (fineEffettiva.after(limiteApi)) {
                fineEffettiva = limiteApi;
            }
            return new String[]{formattaOut.format(inizio), formattaOut.format(fineEffettiva)};
        } catch (Exception e) {
            return null; // In caso di altri errori
        }
    }

    // =========================================================================
    // METODO PER OTTENERE LE PREVISIONE DEL METEO CON VOLLEY
    // =========================================================================
    public static void ottieniPrevisioni(Context context, String citta, String dataInizio, String dataFine, MeteoCallback callback) {

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

    // ===============================================================================================================
    //  METODO PER TRADURRE IL CODICE DEL METEO IN ICONE (basato sui WMO - World Meteorological Organization - codes)
    // ===============================================================================================================
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