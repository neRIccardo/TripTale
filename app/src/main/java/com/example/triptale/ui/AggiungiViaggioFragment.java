package com.example.triptale.ui;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.triptale.database.AppDatabase;
import com.example.triptale.network.FirebaseManager;
import com.example.triptale.R;
import com.example.triptale.model.Viaggio;
import com.example.triptale.utils.DateUtils;

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
        editDataInizio.setOnClickListener(v -> DateUtils.mostraCalendario(getContext(), editDataInizio));
        editDataFine.setOnClickListener(v -> DateUtils.mostraCalendario(getContext(), editDataFine));

        // --- GESTIONE BOTTONE SALVATAGGIO VIAGGIO ---
        btnSalva.setOnClickListener(v -> {
            // Azzeriamo errori
            editTitolo.setError(null);
            editDataInizio.setError(null);
            editDataFine.setError(null);

            String titolo = editTitolo.getText().toString().trim();
            String dataInizio = editDataInizio.getText().toString().trim();
            String dataFine = editDataFine.getText().toString().trim();
            String citta = editCitta.getText().toString().trim();

            if(!DateUtils.validaCampiObbligatori(requireContext(), editTitolo, editDataInizio, editDataFine, titolo, dataInizio, dataFine))
                return;
            if (!DateUtils.sonoDateValide(dataInizio, dataFine)) {
                editDataFine.setError(getString(R.string.errore_generico));
                Toast.makeText(requireContext(), R.string.errore_date_incongruenti, Toast.LENGTH_LONG).show();
                return; // Blocchiamo il salvataggio
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
                    Toast.makeText(requireContext(), R.string.viaggio_creato_successo, Toast.LENGTH_SHORT).show();
                    // Il NavController fa "Indietro" (come premere il tasto back del telefono)
                    Navigation.findNavController(v).popBackStack();
                });
            }).start();
        });
    }
}