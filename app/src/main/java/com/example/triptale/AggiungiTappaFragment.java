package com.example.triptale;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AggiungiTappaFragment extends Fragment {
    private int idViaggioCorrente = -1; // Variabile per ricordare l'ID del viaggio corrente a cui associare la tappa

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

        // Gestione bottone per scattare una foto
        btnScattaFoto.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Domani apriremo la Fotocamera qui!", Toast.LENGTH_SHORT).show();
        });

        // Gestione bottone di salvataggio della tappa
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
}