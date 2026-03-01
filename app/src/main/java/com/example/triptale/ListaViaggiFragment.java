package com.example.triptale;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class ListaViaggiFragment extends Fragment {

    // Colleghiamo il Java alla sua grafica (XML)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // "Gonfiamo" il layout vuoto che Android Studio ha creato per questo Fragment
        return inflater.inflate(R.layout.fragment_lista_viaggi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Troviamo il pulsante "+" nella grafica
        FloatingActionButton fab = view.findViewById(R.id.fabAggiungiViaggio);

        // Gli diciamo cosa fare quando viene cliccato
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // NAVCONTROLLER in azione
                // Gli diciamo di navigare (navigate) seguendo la freccia del grafo
                Navigation.findNavController(view).navigate(R.id.action_listaViaggiFragment_to_aggiungiViaggioFragment);
            }
        });

        // Recuperiamo i nuovi elementi della lista
        ScrollView scrollView = view.findViewById(R.id.scrollViewViaggi);
        LinearLayout contenitoreViaggi = view.findViewById(R.id.contenitoreViaggi);
        TextView textEmptyState = view.findViewById(R.id.textEmptyState);

        // Thread per recuperare i viaggi dal database
        new Thread(() -> {
            // Apriamo il Database e richiediamo la lista dei viaggi
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Viaggio> viaggiSalvati = db.viaggioDao().ottieniViaggi();

            requireActivity().runOnUiThread(() -> {
                // Svuotiamo la lista per evitare duplicati se torniamo indietro
                contenitoreViaggi.removeAllViews();

                if (viaggiSalvati.isEmpty()) {
                    // Se la lista è vuota: nascondiamo la lista e mostriamo il testo di "default"
                    scrollView.setVisibility(View.GONE);
                    textEmptyState.setVisibility(View.VISIBLE);
                } else {
                    textEmptyState.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);

                    for (Viaggio viaggio : viaggiSalvati) {
                        // Creiamo il singolo rettangolino per questo viaggio
                        View itemViaggio = getLayoutInflater().inflate(R.layout.item_viaggio, contenitoreViaggi, false);
                        TextView textTitolo = itemViaggio.findViewById(R.id.textTitoloViaggio);
                        TextView textDate = itemViaggio.findViewById(R.id.textDateViaggio);
                        textTitolo.setText(viaggio.titolo);
                        textDate.setText(viaggio.dataInizio + " - " + viaggio.dataFine);

                        // Gestiamo il click sul singolo rettangolino
                        itemViaggio.setOnClickListener(v -> {
                            // Creiamo il Bundle e serializziamo Viaggio
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("viaggio_selezionato", viaggio);
                            Navigation.findNavController(v).navigate(R.id.action_listaViaggiFragment_to_dettaglioViaggioFragment, bundle);
                        });
                        // Aggiungiamo il rettangolino dentro il LinearLayout
                        contenitoreViaggi.addView(itemViaggio);
                    }
                }
            });
        }).start();
    }
}