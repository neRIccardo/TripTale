package com.example.triptale;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DettaglioViaggioFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dettaglio_viaggio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView textTitolo = view.findViewById(R.id.textTitoloDettaglio);
        TextView textDate = view.findViewById(R.id.textDateDettaglio);

        // Controlliamo se ci è stato passato un Bundle
        if (getArguments() != null) {
            // Estraiamo l'oggetto serializzato usando la stessa chiave definita prima
            Viaggio viaggioRicevuto = (Viaggio) getArguments().getSerializable("viaggio_selezionato");
            if (viaggioRicevuto != null) {
                // Popoliamo l'interfaccia
                textTitolo.setText(viaggioRicevuto.titolo);
                textDate.setText(viaggioRicevuto.dataInizio + " - " + viaggioRicevuto.dataFine);
            }
        }
    }
}