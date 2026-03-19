package com.example.triptale.utils;
import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;

import com.example.triptale.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Classe di utilità per la formattazione e la validazione delle date.
 * Isola la logica di controllo dal codice dell'interfaccia grafica, fornendo
 * metodi riutilizzabili per l'apertura dei calendari e la verifica dei dati inseriti.
 */
public class DateUtils {

    /**
     * Apre un DatePickerDialog nativo per permettere all'utente di selezionare una data.
     * Una volta scelta, la data viene automaticamente formattata e inserita nella casella di testo.
     *
     * @param context Il contesto dell'interfaccia corrente, necessario per generare il popup.
     * @param casellaDaRiempire L'EditText in cui verrà scritta la data selezionata.
     */
    public static void mostraCalendario(Context context, EditText casellaDaRiempire) {
        Calendar calendario = Calendar.getInstance();
        int anno = calendario.get(Calendar.YEAR);
        int mese = calendario.get(Calendar.MONTH);
        int giorno = calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            String dataScelta = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, (month + 1), year);
            casellaDaRiempire.setText(dataScelta);
            casellaDaRiempire.setError(null);
        }, anno, mese, giorno);
        dialog.show();
    }

    /**
     * Verifica la coerenza cronologica tra la data di partenza e quella di ritorno.
     * Previene l'inserimento o la modifica di viaggi in cui il ritorno è antecedente alla partenza.
     *
     * @param dataInizio La data di partenza in formato stringa "dd/MM/yyyy".
     * @param dataFine La data di ritorno in formato stringa "dd/MM/yyyy".
     * @return true se le date sono logicamente coerenti e valide, false in caso di incongruenza o errore di parsing.
     */
    public static boolean sonoDateValide(String dataInizio, String dataFine) {
        SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date dataPartenza = formatoData.parse(dataInizio);
            Date dataRitorno = formatoData.parse(dataFine);

            // Se la data di partenza è successiva a quella di ritorno, è un errore (false)
            if (dataPartenza != null && dataRitorno != null && dataPartenza.after(dataRitorno)) {
                return false;
            }
        } catch (ParseException e) {
            return false;
        }
        return true; // Se passa i controlli, le date sono ok
    }

    /**
     * Valida la completezza dei campi obbligatori all'interno dei form di creazione/modifica.
     * In caso di campo mancante, evidenzia in automatico l'errore sull'interfaccia grafica
     * e restituisce l'esito del controllo per bloccare i salvataggi parziali.
     *
     * @param context Il contesto per accedere alle risorse testuali dei messaggi di errore.
     * @param editTitolo Il campo UI relativo al titolo.
     * @param editDataInizio Il campo UI relativo alla data di partenza.
     * @param editDataFine Il campo UI relativo alla data di ritorno.
     * @param titolo Il valore testuale inserito nel titolo.
     * @param dataInizio Il valore testuale inserito nella data di partenza.
     * @param dataFine Il valore testuale inserito nella data di ritorno.
     * @return true se tutti i parametri sono compilati correttamente, false altrimenti.
     */
    public static boolean validaCampiObbligatori(Context context, EditText editTitolo, EditText editDataInizio, EditText editDataFine, String titolo, String dataInizio, String dataFine) {
        if (titolo.isEmpty()) {
            editTitolo.setError(context.getString(R.string.errore_titolo_viaggio));
            editTitolo.requestFocus();
            return false;
        }
        if (dataInizio.isEmpty()) {
            editDataInizio.setError(context.getString(R.string.errore_generico));
            Toast.makeText(context, R.string.errore_data_partenza, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (dataFine.isEmpty()) {
            editDataFine.setError(context.getString(R.string.errore_generico));
            Toast.makeText(context, R.string.errore_data_ritorno, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}