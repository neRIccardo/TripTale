package com.example.triptale;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class ListaViaggiFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_viaggi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton fab = view.findViewById(R.id.fabAggiungiViaggio);

        // --- GESTIONE BOTTONE AGGIUNGI VIAGGIO ---
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    // Se la lista è vuota nascondiamo la lista e mostriamo il testo di "default"
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
                        ImageView imageCopertina = itemViaggio.findViewById(R.id.imageCopertina);

                        if (viaggio.imagePath != null) {
                            try {
                                // Proviamo a caricare la foto salvata
                                imageCopertina.setImageURI(android.net.Uri.parse(viaggio.imagePath));
                            } catch (SecurityException e) {
                                // Se i permessi sono saltati o la foto non esiste più,
                                // non facciamo crashare l'app mettendo l'icona di default
                                imageCopertina.setImageResource(android.R.drawable.ic_menu_camera);
                            }
                        } else {
                            // Se non c'è proprio il percorso, mettiamo l'icona di default
                            imageCopertina.setImageResource(android.R.drawable.ic_menu_camera);
                        }

                        // --- GESTIONE CLICK SUL SINGOLO VIAGGIO ---
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