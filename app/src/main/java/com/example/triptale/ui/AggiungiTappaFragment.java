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

import android.util.Log;
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

/**
 * Fragment dedicato alla creazione e all'aggiunta di una nuova Tappa a un Viaggio preesistente.
 * Gestisce l'interfaccia utente per l'inserimento testuale, l'acquisizione di una fotografia
 * tramite la fotocamera del dispositivo e il salvataggio concorrente sia sul database locale (Room)
 * sia in cloud (Firebase).
 */
public class AggiungiTappaFragment extends Fragment {
    private int idViaggioCorrente = -1; // Variabile per ricordare l'ID del viaggio corrente a cui associare la tappa
    private ImageView imageAnteprima;
    private String percorsoFotoAttuale = null;
    private Uri uriFotoTemporanea = null;
    private String percorsoNuovaFotoTemp = null; // Il file da 0 byte in attesa
    private boolean salvataggioCompletato = false;
    private String cloudIdViaggioCorrente = null;


    /**
     * Metodo del ciclo di vita chiamato alla creazione iniziale del Fragment.
     * Si occupa di estrarre in background gli ID (locale e cloud) del viaggio padre passati tramite Bundle,
     * in modo da avere i dati pronti prima ancora che venga "gonfiata" (inflated) l'interfaccia grafica.
     *
     * @param savedInstanceState L'eventuale stato precedentemente salvato del Fragment.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recuperiamo l'id del viaggio dal bundle
        if (getArguments() != null) {
            idViaggioCorrente = getArguments().getInt("id_del_viaggio", -1);
            cloudIdViaggioCorrente = getArguments().getString("cloud_id_viaggio", null);
        }
    }

    /**
     * Inizializza e restituisce la gerarchia delle view associata al Fragment.
     *
     * @param inflater Il LayoutInflater utilizzato per "gonfiare" il layout XML.
     * @param container Il ViewGroup padre a cui la UI del Fragment dovrebbe essere attaccata.
     * @param savedInstanceState Lo stato salvato in precedenza.
     * @return La View radice del layout del Fragment.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_aggiungi_tappa, container, false);
    }

    /**
     * Metodo chiamato immediatamente dopo la creazione della gerarchia delle view.
     * Qui vengono collegati i componenti grafici (findViewById), impostati i listener
     * per i bottoni di scatto foto e salvataggio, e gestita la logica di inserimento dati.
     *
     * @param view La View radice restituita da onCreateView().
     * @param savedInstanceState L'eventuale stato salvato in precedenza.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

    /**
     * Metodo del ciclo di vita chiamato quando la vista (UI) del Fragment sta per essere distrutta.
     * Svolge due compiti fondamentali di ottimizzazione:
     * 1. Elimina fisicamente dal dispositivo le foto "orfane" (scattate ma non salvate).
     * 2. Previene i Memory Leak sganciando i riferimenti alle View globali.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Se sta uscendo SENZA salvare e aveva scattato una foto, cancelliamola
        if (!salvataggioCompletato && percorsoFotoAttuale != null) {
            if (!new File(percorsoFotoAttuale).delete()) {
                Log.w("TripTale", "Impossibile eliminare il file o file già assente");
            }
        }
        imageAnteprima = null;
    }

    /**
     * Gestore asincrono dell'intent per la fotocamera, basato sulla Activity Result API.
     * Rimane in attesa dell'esito dello scatto: in caso di successo (esitoPositivo = true),
     * applica il watermark e il ridimensionamento all'immagine, elimina eventuali foto scattate
     * precedentemente per lo stesso inserimento e aggiorna l'anteprima a schermo.
     */
    private final ActivityResultLauncher<Uri> scattaFotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            esitoPositivo -> {
                if (esitoPositivo) {
                    if (percorsoNuovaFotoTemp != null) {
                        ImageUtils.ridimensionaEApplicaWatermark(requireContext(), percorsoNuovaFotoTemp);
                        // Se l'utente aveva GIA' scattato una foto in questa sessione
                        // e ha deciso di rifarla, cancelliamo quella precedente
                        if (percorsoFotoAttuale != null) {
                            if (!new File(percorsoFotoAttuale).delete()) {
                                Log.w("TripTale", "Impossibile eliminare il file o file già assente");
                            }
                        }
                        // Promuoviamo la foto temporanea a foto ufficiale
                        percorsoFotoAttuale = percorsoNuovaFotoTemp;
                    }
                    imageAnteprima.setImageURI(uriFotoTemporanea);
                } else {
                    // L'utente ha premuto INDIETRO sulla fotocamera
                    // Cancelliamo il file da 0 byte
                    if (percorsoNuovaFotoTemp != null) {
                        if (!new File(percorsoNuovaFotoTemp).delete()) {
                            Log.w("TripTale", "Impossibile eliminare il file o file già assente");
                        }
                        percorsoNuovaFotoTemp = null;
                    }
                }
            }
    );
}