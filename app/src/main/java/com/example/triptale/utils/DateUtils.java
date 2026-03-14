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

public class DateUtils {

    // =========================================================================
    // METODO PER APRIRE IL CALENDARIO
    // =========================================================================
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

    // =========================================================================
    // METODO PER VALIDARE LE DATE (Ritorna true se le date sono valide)
    // =========================================================================
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