package com.prototipo2.androidbasicoapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/*
 * =====================================================================
 *  ARCHIVO : DetalleActivity.java
 *  QUÉ HACE: Destino del INTENT EXPLÍCITO 1. Recibe los datos que envía
 *            MainActivity (putExtra) y los muestra después de una carga
 *            simulada en un Thread, sin congelar la pantalla.
 *  ESTRUCTURA: 1. Claves de los extras  2. Vistas  3. onCreate  4. Thread
 * =====================================================================
 */
public class DetalleActivity extends AppCompatActivity {

    // 1. CLAVES DE LOS EXTRAS (públicas: MainActivity las usa para no escribirlas mal)
    public static final String EXTRA_TITULO = "extra_titulo";
    public static final String EXTRA_DESCRIPCION = "extra_descripcion";
    public static final String EXTRA_CAPACIDAD = "extra_capacidad";
    private static final int TIEMPO_CARGA_MS = 1500;  // simula una consulta lenta a un servidor

    // 2. VISTAS
    private TextView tvTitulo, tvDescripcion, tvCapacidad;
    private ProgressBar pbCarga;

    // 3. CICLO DE VIDA
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        tvTitulo = findViewById(R.id.tvTitulo);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvCapacidad = findViewById(R.id.tvCapacidad);
        pbCarga = findViewById(R.id.pbCarga);
        Button btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());  // cierra esta pantalla y vuelve a MainActivity

        // Leemos los extras. Validación de null: si no vienen, mostramos un texto por defecto
        String titulo = getIntent().getStringExtra(EXTRA_TITULO);
        String descripcion = getIntent().getStringExtra(EXTRA_DESCRIPCION);
        int capacidad = getIntent().getIntExtra(EXTRA_CAPACIDAD, -1);  // -1 = no vino (un int no puede ser null)
        if (titulo == null) titulo = getString(R.string.sin_informacion);
        if (descripcion == null) descripcion = getString(R.string.sin_informacion);
        String textoCapacidad = capacidad >= 0
                ? getString(R.string.txt_capacidad, capacidad)
                : getString(R.string.sin_informacion);

        cargarEnSegundoPlano(titulo, descripcion, textoCapacidad);
    }

    // 4. THREAD: espera en un hilo aparte y luego vuelve al hilo principal para mostrar los datos
    private void cargarEnSegundoPlano(String titulo, String descripcion, String capacidad) {
        pbCarga.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                Thread.sleep(TIEMPO_CARGA_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            runOnUiThread(() -> {        // solo el hilo principal puede modificar las vistas
                if (isFinishing()) return; // validación: el usuario volvió antes de que terminara
                pbCarga.setVisibility(View.GONE);
                tvTitulo.setText(titulo);
                tvDescripcion.setText(descripcion);
                tvCapacidad.setText(capacidad);
            });
        }).start();
    }
}
