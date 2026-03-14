package com.example.triptale.ui;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.triptale.database.AppDatabase;
import com.example.triptale.network.FirebaseManager;
import com.example.triptale.utils.ImageUtils;
import com.example.triptale.R;
import com.example.triptale.model.Tappa;
import java.io.File;
import java.io.IOException;

public class AggiungiTappaFragment extends Fragment {
    private int idViaggioCorrente = -1; // Variabile per ricordare l'ID del viaggio corrente a cui associare la tappa
    private ImageView imageAnteprima;
    private String percorsoFotoAttuale = null;
    private Uri uriFotoTemporanea = null;
    private String percorsoNuovaFotoTemp = null; // Il file da 0 byte in attesa
    private boolean salvataggioCompletato = false;
    private String cloudIdViaggioCorrente = null;

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
            cloudIdViaggioCorrente = getArguments().getString("cloud_id_viaggio", null);
        }
        // Controllo di sicurezza
        if (idViaggioCorrente == -1) {
            Toast.makeText(requireContext(), getString(R.string.viaggio_non_trovato), Toast.LENGTH_SHORT).show();
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
                File fileImmagine = ImageUtils.creaFileImmagine(requireContext());
                percorsoNuovaFotoTemp = fileImmagine.getAbsolutePath();

                // Chiediamo al FileProvider di creare un Uri sicuro per questo file
                uriFotoTemporanea = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        fileImmagine
                );
                // Lanciamo la fotocamera passandole l'Uri sicuro
                scattaFotoLauncher.launch(uriFotoTemporanea);

            } catch (IOException e) {
                Toast.makeText(requireContext(), R.string.errore_creazione_file, Toast.LENGTH_SHORT).show();
            }
        });

        // --- GESTIONE BOTTONE SALVATGGIO TAPPA ---
        btnSalva.setOnClickListener(v -> {
            salvataggioCompletato = true;

            String titoloInserito = editTitolo.getText().toString().trim();
            String noteInserite = editNote.getText().toString().trim();
            editTitolo.setError(null);

            if (titoloInserito.isEmpty()) {
                editTitolo.requestFocus();
                editTitolo.setError(getString(R.string.errore_titolo_obbligatorio));
                return; // Blocchiamo il salvataggio
            }

            Tappa nuovaTappa = new Tappa(idViaggioCorrente, titoloInserito, noteInserite);
            nuovaTappa.imagePath = percorsoFotoAttuale;
            new Thread(() -> {
                long idGenerato = AppDatabase.getInstance(requireContext()).tappaDao().inserisciTappa(nuovaTappa);
                nuovaTappa.id = (int) idGenerato;
                FirebaseManager.aggiungiTappa(requireContext(), nuovaTappa, cloudIdViaggioCorrente);

                if (!isAdded()) return;

                // Torniamo sul Main Thread per la grafica
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.tappa_salvata, Toast.LENGTH_SHORT).show();
                    // Torniamo al cruscotto del viaggio
                    Navigation.findNavController(view).popBackStack();
                });
            }).start();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Se sta uscendo SENZA salvare e aveva scattato una foto, cancelliamola
        if (!salvataggioCompletato && percorsoFotoAttuale != null) {
            new File(percorsoFotoAttuale).delete();
        }
    }

    // =========================================================================
    // OGGETTO PER GESTIRE L'INTENT DELLA FOTOCAMERA
    // =========================================================================
    private final ActivityResultLauncher<Uri> scattaFotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            esitoPositivo -> {
                if (esitoPositivo) {
                    if (percorsoNuovaFotoTemp != null) {
                        ImageUtils.ridimensionaEApplicaWatermark(requireContext(), percorsoNuovaFotoTemp);
                        // Se l'utente aveva GIA' scattato una foto in questa sessione
                        // e ha deciso di rifarla, cancelliamo quella precedente
                        if (percorsoFotoAttuale != null) {
                            new File(percorsoFotoAttuale).delete();
                        }
                        // Promuoviamo la foto temporanea a foto ufficiale
                        percorsoFotoAttuale = percorsoNuovaFotoTemp;
                    }
                    imageAnteprima.setImageURI(uriFotoTemporanea);
                } else {
                    // L'utente ha premuto INDIETRO sulla fotocamera
                    // Cancelliamo il file da 0 byte
                    if (percorsoNuovaFotoTemp != null) {
                        new File(percorsoNuovaFotoTemp).delete();
                        percorsoNuovaFotoTemp = null;
                    }
                }
            }
    );
}