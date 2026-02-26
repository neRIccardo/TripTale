package com.example.triptale;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

// Questa classe è un Adapter per la RecyclerView
public class ViaggioAdapter extends RecyclerView.Adapter<ViaggioAdapter.ViaggioViewHolder> {

    // Lista vuota che poi riempiremo con i dati del database
    private List<Viaggio> listaViaggi = new ArrayList<>();

    // Metodo che useremo per "passare" i viaggi all'Adapter
    public void setViaggi(List<Viaggio> viaggi) {
        this.listaViaggi = viaggi;
        notifyDataSetChanged(); // Avvisa la grafica che i dati sono cambiati
    }

    @NonNull
    @Override
    public ViaggioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate del file XML item_viaggio trasformandolo in un oggetto Java
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_viaggio, parent, false);
        return new ViaggioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViaggioViewHolder holder, int position) {
        // Prendiamo il singolo viaggio dalla lista e settiamo i vari parametri
        Viaggio viaggioCorrente = listaViaggi.get(position);
        holder.textTitolo.setText(viaggioCorrente.titolo);
        holder.textDate.setText(viaggioCorrente.dataInizio + " - " + viaggioCorrente.dataFine);
    }

    @Override
    public int getItemCount() {
        // Ritorna quanti elementi ci sono in totale
        return listaViaggi.size();
    }

    // ViewHolder: questa classe memorizza i componenti grafici del singolo item
    class ViaggioViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitolo;
        private TextView textDate;
        private ImageView imageCopertina;

        public ViaggioViewHolder(@NonNull View itemView) {
            super(itemView);
            // Prendiamo gli ID degli elementi nel file item_viaggio.xml
            textTitolo = itemView.findViewById(R.id.textTitoloViaggio);
            textDate = itemView.findViewById(R.id.textDateViaggio);
            imageCopertina = itemView.findViewById(R.id.imageCopertina);
        }
    }
}
