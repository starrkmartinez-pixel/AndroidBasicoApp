package com.prototipo2.androidbasicoapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;



/*
 * =====================================================================
 *  ARCHIVO : MainActivity.java
 *  QUÉ HACE: Pantalla principal de AndroidBasicoApp (guía de la sede).
 *            Desde aquí se lanzan los 5 intents implícitos y los 3 explícitos.
 *            Sin MVC/MVVM: la lógica vive en las Activities.
 *  ESTRUCTURA:
 *     1. Constantes            2. Vistas              3. Estado
 *     4. onCreate              5. Intents implícitos (I1 a I5)
 *     6. Respuesta de permisos 7. Intents explícitos (E1 a E3)
 *     8. Utilidades
 * =====================================================================
 */
public class MainActivity extends AppCompatActivity {

    // =====================================================================
    // 1. CONSTANTES (static final: una sola copia para la clase y no cambian)
    // =====================================================================

    // =====================================================================
    // 2. VISTAS (encapsuladas como private)
    // =====================================================================
    private Button btnUbicacion, btnMapa, btnWeb, btnLlamar, btnCorreo, btnCamara;
    private TextView tvUbicacion;
    private ImageView imgFoto;
    private ProgressBar pbFoto;


    // =====================================================================
    // 4. CICLO DE VIDA
    // =====================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Desde Android 15 la app se dibuja bajo las barras del sistema: este bloque deja el espacio justo
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // Conexión del XML con Java
        btnUbicacion = findViewById(R.id.btnUbicacion);
        btnMapa = findViewById(R.id.btnMapa);
        btnWeb = findViewById(R.id.btnWeb);
        btnLlamar = findViewById(R.id.btnLlamar);
        btnCorreo = findViewById(R.id.btnCorreo);
        btnCamara = findViewById(R.id.btnCamara);
        tvUbicacion = findViewById(R.id.tvUbicacion);
        imgFoto = findViewById(R.id.imgFoto);
        pbFoto = findViewById(R.id.pbFoto);

        // Eventos de los intents implícitos

        // Eventos de los intents explícitos (sección 7)
        configurarIntentsExplicitos();
    }

    // =====================================================================
    // 5. INTENTS IMPLÍCITOS (Android elige qué app externa responde)
    // =====================================================================

    // =====================================================================
    // 7. INTENTS EXPLÍCITOS (navegan a pantallas de nuestra propia app)
    // =====================================================================
    private void configurarIntentsExplicitos() {
        // Aquí se conectarán los botones de Detalle, Ajustes e Inscripción (E1 a E3)
    }
}
