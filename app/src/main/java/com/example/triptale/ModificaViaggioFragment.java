package com.example.triptale;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ModificaViaggioFragment extends Fragment {

    private Viaggio viaggioCorrente;
    private ImageView imageCopertina;
    private String nuovoPercorsoImmagine = null; // Qui salveremo la foto scelta

    // Questo oggetto si mette in ascolto: quando la galleria si chiude, cattura la foto scelta
    private final ActivityResultLauncher<Intent> apriGalleriaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // L'utente ha scelto una foto, estraiamo il suo URI
                    Uri uriImmagineSelezionata = result.getData().getData();
                    if (uriImmagineSelezionata != null) {
                        // Diciamo ad Android di non far scadere il permesso di lettura per questa foto
                        requireActivity().getContentResolver().takePersistableUriPermission(
                                uriImmagineSelezionata,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        // Salviamo l'indirizzo come stringa per metterlo nel Database
                        nuovoPercorsoImmagine = uriImmagineSelezionata.toString();
                        // Mostriamo la foto nel nostro quadratino
                        imageCopertina.setImageURI(uriImmagineSelezionata);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_modifica_viaggio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editTitolo = view.findViewById(R.id.editTitoloModifica);
        EditText editDataInizio = view.findViewById(R.id.editDataInizioModifica);
        EditText editDataFine = view.findViewById(R.id.editDataFineModifica);
        imageCopertina = view.findViewById(R.id.imageCopertinaViaggio);
        Button btnScattaFoto = view.findViewById(R.id.btnSelezionaFotoCopertina);
        Button btnSalva = view.findViewById(R.id.btnSalvaModifiche);

        editDataInizio.setFocusable(false);
        editDataFine.setFocusable(false);

        // Recupero viaggio dal Bundle e precompilazione campi
        if (getArguments() != null) {
            viaggioCorrente = (Viaggio) getArguments().getSerializable("viaggio_selezionato");
            if (viaggioCorrente != null) {
                editTitolo.setText(viaggioCorrente.titolo);
                editDataInizio.setText(viaggioCorrente.dataInizio);
                editDataFine.setText(viaggioCorrente.dataFine);

                if (viaggioCorrente.imagePath != null) {
                    try {
                        imageCopertina.setImageURI(Uri.parse(viaggioCorrente.imagePath));
                        nuovoPercorsoImmagine = viaggioCorrente.imagePath;
                    } catch (Exception e) {
                        // L'utente ha cancellato la foto dal telefono o ha revocato i permessi
                        imageCopertina.setImageResource(android.R.drawable.ic_menu_camera);
                        nuovoPercorsoImmagine = null; // Resettiamo la variabile, così se l'utente salva, puliamo il DB
                    }
                }
            }
        }

        editDataInizio.setOnClickListener(v -> mostraCalendario(editDataInizio));
        editDataFine.setOnClickListener(v -> mostraCalendario(editDataFine));

        // Gestione scelta copertina
        btnScattaFoto.setOnClickListener(v -> {
            // ACTION_OPEN_DOCUMENT assegna automaticamente un permesso temporaneo in lettura
            // Non serve richiedere l'autorizzazione all'utente, a contrario di ACTION_PICK
            Intent intentGalleria = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentGalleria.addCategory(Intent.CATEGORY_OPENABLE);
            intentGalleria.setType("image/*"); // Mostra solo le immagini

            // Lanciamo l'intent
            apriGalleriaLauncher.launch(intentGalleria);
        });

        // Update modifiche nel DB
        btnSalva.setOnClickListener(v -> {
            String titolo = editTitolo.getText().toString().trim();
            String dataInizio = editDataInizio.getText().toString().trim();
            String dataFine = editDataFine.getText().toString().trim();

            editTitolo.setError(null);
            editDataInizio.setError(null);
            editDataFine.setError(null);

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
            // CONTROLLO LOGICO DELLE DATE (L'inizio non può essere dopo la fine)
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

            // Aggiorniamo l'oggetto viaggioCorrente con i nuovi dati
            viaggioCorrente.titolo = titolo;
            viaggioCorrente.dataInizio = dataInizio;
            viaggioCorrente.dataFine = dataFine;
            viaggioCorrente.imagePath = nuovoPercorsoImmagine; // La nuova foto (o quella vecchia se non l'ha cambiata)

            // Salviamo nel DB usando un Thread
            new Thread(() -> {
                AppDatabase.getInstance(requireContext()).viaggioDao().aggiornaViaggio(viaggioCorrente);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Viaggio modificato!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                });
            }).start();
        });
    }

    // Metodo per il calendario (identico a quello in AggiungiViaggio)
    private void mostraCalendario(EditText casellaDaRiempire) {
        Calendar calendario = Calendar.getInstance();
        int anno = calendario.get(Calendar.YEAR);
        int mese = calendario.get(Calendar.MONTH);
        int giorno = calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                String dataScelta = String.format(Locale.ITALY, "%02d/%02d/%04d", dayOfMonth, (month + 1), year);
                casellaDaRiempire.setText(dataScelta);
                casellaDaRiempire.setError(null);
            }
        }, anno, mese, giorno);
        dialog.show();
    }
}