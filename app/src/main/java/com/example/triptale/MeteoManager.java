package com.example.triptale;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
            java.text.SimpleDateFormat formattaIn = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ITALY);
            java.text.SimpleDateFormat formattaOut = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH);

            java.util.Date inizio = formattaIn.parse(dataInizio);
            java.util.Date fine = formattaIn.parse(dataFine);

            // Calcoliamo "Oggi + 14 giorni" (limite fisico di Open-Meteo)
            java.util.Calendar calOggi = java.util.Calendar.getInstance();
            calOggi.add(java.util.Calendar.DAY_OF_YEAR, 14);
            java.util.Date limiteApi = calOggi.getTime();

            // Se il viaggio INIZIA oltre i 14 giorni da oggi, fermiamo tutto subito
            if (inizio.after(limiteApi)) {
                return null;
            }

            // Calcoliamo "Inizio + 6 giorni" (limite grafico per avere max 7 quadratini)
            java.util.Calendar calInizio = java.util.Calendar.getInstance();
            calInizio.setTime(inizio);
            calInizio.add(java.util.Calendar.DAY_OF_YEAR, 6);
            java.util.Date limiteGrafico = calInizio.getTime();

            // Scegliamo la data di fine corretta. Deve essere la PIÙ PICCOLA tra:
            // - La vera fine del viaggio
            // - Il limite grafico dei 7 giorni
            // - Il limite API dei 14 giorni da oggi
            java.util.Date fineEffettiva = fine;

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
    // METODO PER OTTENERE LE PREVISIONE DEL METEO
    // =========================================================================
    public static void ottieniPrevisioni(String citta, String dataInizio, String dataFine, MeteoCallback callback) {
        new Thread(() -> {
            try {
                // --- FASE 1: GEOCODING ---
                String urlGeocoding = "https://geocoding-api.open-meteo.com/v1/search?name=" + citta + "&count=1&language=it";
                URL urlGeo = new URL(urlGeocoding);
                HttpURLConnection connGeo = (HttpURLConnection) urlGeo.openConnection();
                connGeo.setRequestMethod("GET");

                BufferedReader readerGeo = new BufferedReader(new InputStreamReader(connGeo.getInputStream()));
                StringBuilder responseGeo = new StringBuilder();
                String line;
                while ((line = readerGeo.readLine()) != null) responseGeo.append(line);
                readerGeo.close();

                JSONObject jsonGeo = new JSONObject(responseGeo.toString());

                if (!jsonGeo.has("results")) {
                    callback.onError("Città non trovata: controlla il nome nel pannello di modifica.");
                    return;
                }

                JSONObject cittaResult = jsonGeo.getJSONArray("results").getJSONObject(0);
                double lat = cittaResult.getDouble("latitude");
                double lon = cittaResult.getDouble("longitude");

                // --- FASE 2: FORECAST METEO ---
                String urlMeteo = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto";
                String[] dateApi = calcolaDateApi(dataInizio, dataFine);

                // Se dateApi è nullo, significa che il viaggio è tutto oltre i 14 giorni
                if (dateApi == null) {
                    callback.onError("Le previsioni meteo sono disponibili solo per i prossimi 14 giorni.");
                    return;
                }

                urlMeteo += "&start_date=" + dateApi[0] + "&end_date=" + dateApi[1];
                URL urlFor = new URL(urlMeteo);
                HttpURLConnection connFor = (HttpURLConnection) urlFor.openConnection();
                connFor.setRequestMethod("GET");

                // Controllo di sicurezza aggiuntivo
                if (connFor.getResponseCode() != 200) {
                    callback.onError("Servizio meteo momentaneamente non disponibile.");
                    return;
                }

                BufferedReader readerFor = new BufferedReader(new InputStreamReader(connFor.getInputStream()));
                StringBuilder responseFor = new StringBuilder();
                while ((line = readerFor.readLine()) != null) responseFor.append(line);
                readerFor.close();

                JSONObject jsonMeteo = new JSONObject(responseFor.toString());
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
                callback.onError("Assenza di connessione a internet.");
            }
        }).start();
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