package com.example.triptale;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AggiungiTappaFragment extends Fragment {
    private int idViaggioCorrente = -1; // Variabile per ricordare l'ID del viaggio corrente a cui associare la tappa
    private ImageView imageAnteprima;
    private String percorsoFotoAttuale = null;
    private Uri uriFotoTemporanea = null;

    // =========================================================================
    // OGGETTO PER GESTIRE L'INTENT DELLA FOTOCAMERA
    // =========================================================================
    private final ActivityResultLauncher<Uri> scattaFotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), esitoPositivo -> {
                if (esitoPositivo) {
                    // L'utente ha scattato la foto e ha premuto la spunta
                    // La fotocamera ha riempito il nostro file vuoto
                    imageAnteprima.setImageURI(uriFotoTemporanea);
                } else {
                    // L'utente ha premuto "Indietro" o ha annullato lo scatto
                    percorsoFotoAttuale = null;
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_aggiungi_tappa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Recuperiamo l'id del viaggio dal bundle
        if (getArguments() != null) {
            idViaggioCorrente = getArguments().getInt("id_del_viaggio", -1);
        }
        // Controllo di sicurezza
        if (idViaggioCorrente == -1) {
            Toast.makeText(requireContext(), "Errore: Viaggio non trovato!", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        EditText editTitolo = view.findViewById(R.id.editTitoloTappa);
        EditText editNote = view.findViewById(R.id.editNoteTappa);
        Button btnScattaFoto = view.findViewById(R.id.btnScattaFoto);
        Button btnSalva = view.findViewById(R.id.btnSalvaTappa);
        imageAnteprima = view.findViewById(R.id.imageAnteprimaFoto);

        // --- GESTIONE BOTTONE SCATTO FOTO ---
        btnScattaFoto.setOnClickListener(v -> {
            try {
                // Creiamo il file vuoto
                File fileImmagine = creaFileImmagine();

                // Chiediamo al FileProvider di creare un Uri sicuro per questo file
                uriFotoTemporanea = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        fileImmagine
                );

                // Lanciamo la fotocamera passandole l'Uri sicuro
                scattaFotoLauncher.launch(uriFotoTemporanea);

            } catch (IOException e) {
                Toast.makeText(requireContext(), "Errore nella creazione del file", Toast.LENGTH_SHORT).show();
            }
        });

        // --- GESTIONE BOTTONE SALVATGGIO TAPPA ---
        btnSalva.setOnClickListener(v -> {
            String titoloInserito = editTitolo.getText().toString().trim();
            String noteInserite = editNote.getText().toString().trim();
            editTitolo.setError(null);

            if (titoloInserito.isEmpty()) {
                editTitolo.requestFocus();
                editTitolo.setError("Il titolo è obbligatorio!");
                return; // Blocchiamo il salvataggio
            }

            Tappa nuovaTappa = new Tappa(idViaggioCorrente, titoloInserito, noteInserite);
            nuovaTappa.imagePath = percorsoFotoAttuale;
            new Thread(() -> {
                AppDatabase.getInstance(requireContext()).tappaDao().inserisciTappa(nuovaTappa);
                // Torniamo sul Main Thread per la grafica
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Tappa salvata!", Toast.LENGTH_SHORT).show();
                    // Torniamo al cruscotto del viaggio
                    Navigation.findNavController(view).popBackStack();
                });
            }).start();
        });
    }

    // =========================================================================
    // METODO PER CREARE UN FILE VUOTO PER LA FOTOCAMERA
    // =========================================================================
    private File creaFileImmagine() throws IOException {
        // Creiamo un nome basato sulla data e ora esatta
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nomeFile = "JPEG_" + timeStamp + "_";

        // Prendiamo la cartella "Pictures" segreta della nostra app
        File cartellaStorage = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Creiamo fisicamente il file vuoto
        File fileCreato = File.createTempFile(
                nomeFile,  // prefisso
                ".jpg",    // suffisso
                cartellaStorage // cartella
        );

        // Salviamo il percorso assoluto (Android/data/com.example.triptale/files/Pictures/JPEG_...)
        // Questo è il percorso che finirà nel nostro Database Room
        percorsoFotoAttuale = fileCreato.getAbsolutePath();
        return fileCreato;
    }
}