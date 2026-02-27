package com.example.triptale;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    }
}