package com.example.triptale;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AggiungiViaggioFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_aggiungi_viaggio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editTitolo = view.findViewById(R.id.editTitoloViaggio);
        EditText editDataInizio = view.findViewById(R.id.editDataInizio);
        EditText editDataFine = view.findViewById(R.id.editDataFine);
        Button btnSalva = view.findViewById(R.id.btnSalvaViaggio);
        EditText editCitta = view.findViewById(R.id.editCittaViaggio);


        // Blocchiamo la tastiera su queste due caselle
        editDataInizio.setFocusable(false);
        editDataFine.setFocusable(false);

        // Quando tocchiamo le caselle, apriamo il calendario
        editDataInizio.setOnClickListener(v -> mostraCalendario(editDataInizio));
        editDataFine.setOnClickListener(v -> mostraCalendario(editDataFine));

        // --- GESTIONE BOTTONE SALVATAGGIO VIAGGIO ---
        btnSalva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Azzeriamo errori
                editTitolo.setError(null);
                editDataInizio.setError(null);
                editDataFine.setError(null);

                String titolo = editTitolo.getText().toString().trim();
                String dataInizio = editDataInizio.getText().toString().trim();
                String dataFine = editDataFine.getText().toString().trim();
                String citta = editCitta.getText().toString().trim();


                if (titolo.isEmpty()) {
                    editTitolo.setError("Inserisci il titolo del viaggio!");
                    editTitolo.requestFocus();
                    return;
                }
                if (dataInizio.isEmpty()) {
                    editDataInizio.setError("Errore");
                    Toast.makeText(requireContext(), "Seleziona la data di partenza!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (dataFine.isEmpty()) {
                    editDataFine.setError("Errore");
                    Toast.makeText(requireContext(), "Seleziona la data di ritorno!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Controllo logico delle date (L'inizio non può essere dopo la fine)
                SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY);
                try {
                    Date dataPartenza = formatoData.parse(dataInizio);
                    Date dataRitorno = formatoData.parse(dataFine);

                    // Se la data di partenza è successiva a quella di ritorno...
                    if (dataPartenza != null && dataRitorno != null && dataPartenza.after(dataRitorno)) {
                        editDataFine.setError("Errore");
                        Toast.makeText(requireContext(), "La data di fine non può precedere l'inizio!", Toast.LENGTH_LONG).show();
                        return; // Blocchiamo il salvataggio
                    }
                } catch (ParseException e) {
                    Log.e("ErroreData", "Impossibile leggere la data inserita", e);
                }

                Viaggio nuovoViaggio = new Viaggio(titolo, citta, dataInizio, dataFine);
                new Thread(() -> {
                    // Recuperiamo il database
                    AppDatabase db = AppDatabase.getInstance(requireContext());

                    // Inseriamo il viaggio
                    long idGenerato = db.viaggioDao().inserisciViaggio(nuovoViaggio);
                    nuovoViaggio.id = (int) idGenerato;
                    FirebaseManager.aggiungiViaggio(requireContext(), nuovoViaggio);

                    if (!isAdded()) return;

                    // Torniamo sul thread principale (UI Thread) per aggiornare lo schermo
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Viaggio creato con successo!", Toast.LENGTH_SHORT).show();
                        // Il NavController fa "Indietro" (come premere il tasto back del telefono)
                        Navigation.findNavController(v).popBackStack();
                    });
                }).start();
            }
        });
    }

    // =========================================================================
    // METODO PER APRERE IL CALENDARIO
    // =========================================================================
    private void mostraCalendario(EditText casellaDaRiempire) {
        // Prendiamo la data di oggi per aprire il calendario sul giorno giusto
        Calendar calendario = Calendar.getInstance();
        int anno = calendario.get(Calendar.YEAR);
        int mese = calendario.get(Calendar.MONTH);
        int giorno = calendario.get(Calendar.DAY_OF_MONTH);

        // Creiamo la finestrella del calendario
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // Attenzione: i mesi in Java partono da 0 (Gennaio = 0), quindi aggiungiamo 1
                String dataScelta = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, (month + 1), year);
                casellaDaRiempire.setText(dataScelta); // Scriviamo la data nella casella

                // Togliamo subito l'errore rosso non appena l'utente sceglie una data
                casellaDaRiempire.setError(null);
            }
        }, anno, mese, giorno);
        dialog.show(); // Mostriamo il calendario a schermo
    }
}