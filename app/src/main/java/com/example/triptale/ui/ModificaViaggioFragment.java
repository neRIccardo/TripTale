package com.example.triptale.ui;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.triptale.database.AppDatabase;
import com.example.triptale.network.FirebaseManager;
import com.example.triptale.R;
import com.example.triptale.model.Viaggio;
import com.example.triptale.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ModificaViaggioFragment extends Fragment {

    private Viaggio viaggioCorrente;
    private ImageView imageCopertina;
    private String nuovoPercorsoImmagine = null; // Qui salveremo la foto scelta

    // =========================================================================
    // OGGETTO PER GESTIRE L'INTENT DELLA GALLERIA
    // =========================================================================
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            viaggioCorrente = getArguments().getParcelable("viaggio_selezionato");
        }
    }

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
        EditText editCitta = view.findViewById(R.id.editCittaModifica);

        editDataInizio.setFocusable(false);
        editDataFine.setFocusable(false);

        // Precompilazione campi
        if (viaggioCorrente != null) {
            editTitolo.setText(viaggioCorrente.titolo);
            editDataInizio.setText(viaggioCorrente.dataInizio);
            editDataFine.setText(viaggioCorrente.dataFine);
            editCitta.setText(viaggioCorrente.cittaDestinazione);

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

        editDataInizio.setOnClickListener(v -> DateUtils.mostraCalendario(getContext(), editDataInizio));
        editDataFine.setOnClickListener(v -> DateUtils.mostraCalendario(getContext(), editDataFine));

        // --- GESTIONE BOTTONE SCATTO FOTO ---
        btnScattaFoto.setOnClickListener(v -> {
            // ACTION_OPEN_DOCUMENT assegna automaticamente un permesso temporaneo in lettura
            // Non serve richiedere l'autorizzazione all'utente, a contrario di ACTION_PICK
            Intent intentGalleria = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intentGalleria.addCategory(Intent.CATEGORY_OPENABLE);
            intentGalleria.setType("image/*"); // Mostra solo le immagini

            // Lanciamo l'intent
            apriGalleriaLauncher.launch(intentGalleria);
        });

        // --- GESTIONE BOTTONE SALVATAGGIO VIAGGIO ---
        btnSalva.setOnClickListener(v -> {
            String titolo = editTitolo.getText().toString().trim();
            String dataInizio = editDataInizio.getText().toString().trim();
            String dataFine = editDataFine.getText().toString().trim();
            String citta = editCitta.getText().toString().trim();

            editTitolo.setError(null);
            editDataInizio.setError(null);
            editDataFine.setError(null);

            if(!DateUtils.validaCampiObbligatori(requireContext(), editTitolo, editDataInizio, editDataFine, titolo, dataInizio, dataFine))
                return;
            if (!DateUtils.sonoDateValide(dataInizio, dataFine)) {
                editDataFine.setError(getString(R.string.errore_generico));
                Toast.makeText(requireContext(), R.string.errore_date_incongruenti, Toast.LENGTH_LONG).show();
                return; // Blocchiamo il salvataggio
            }

            // Aggiorniamo l'oggetto viaggioCorrente con i nuovi dati
            viaggioCorrente.titolo = titolo;
            viaggioCorrente.cittaDestinazione = citta;
            viaggioCorrente.dataInizio = dataInizio;
            viaggioCorrente.dataFine = dataFine;
            viaggioCorrente.imagePath = nuovoPercorsoImmagine; // La nuova foto (o quella vecchia se non l'ha cambiata)

            // Salviamo nel DB usando un Thread
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                Viaggio viaggioAggiornato = db.viaggioDao().ottieniViaggioPerId(viaggioCorrente.id);
                if (viaggioAggiornato != null) {
                    viaggioCorrente.cloudId = viaggioAggiornato.cloudId;
                }
                db.viaggioDao().aggiornaViaggio(viaggioCorrente);

                // Aggiorniamo Firebase
                if (viaggioCorrente.cloudId != null && !viaggioCorrente.cloudId.isEmpty()) {
                    FirebaseManager.aggiornaViaggio(viaggioCorrente);
                } else {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        FirebaseManager.sincronizzaTutto(requireContext(), user.getUid(), null);
                    }
                }

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.viaggio_modificato, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                });
            }).start();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageCopertina = null;
    }
}