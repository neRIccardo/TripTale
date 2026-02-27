package com.example.triptale;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Diciamo ad Android di usare il design moderno a tutto schermo
        EdgeToEdge.enable(this);

        // Carichiamo la cornice
        setContentView(R.layout.activity_main);

        // Diciamo alla cornice (R.id.main) di aggiungere un padding
        // grande esattamente quanto la barra superiore (batteria) e quella inferiore (tasti)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}