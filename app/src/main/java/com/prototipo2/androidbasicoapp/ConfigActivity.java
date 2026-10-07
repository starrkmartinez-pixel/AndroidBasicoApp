package com.prototipo2.androidbasicoapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

/*
 * =====================================================================
 *  ARCHIVO : ConfigActivity.java
 *  QUÉ HACE: Destino del INTENT EXPLÍCITO 2. Pantalla de ajustes internos
 *            con Toolbar propia y flecha "Atrás". Guarda un ajuste en
 *            SharedPreferences para que se mantenga al cerrar la app.
 *  ESTRUCTURA: 1. Constantes  2. onCreate  3. Flecha "Atrás"
 * =====================================================================
 */
public class ConfigActivity extends AppCompatActivity {

    // 1. CONSTANTES: nombre del archivo de ajustes y clave del valor guardado
    private static final String PREFERENCIAS = "ajustes_androidbasicoapp";
    private static final String CLAVE_NOTIFICACIONES = "notificaciones";

    // 2. CICLO DE VIDA
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_config);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        // El tema es NoActionBar: la Toolbar del XML pasa a ser la barra de esta pantalla
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {  // validación de null
            getSupportActionBar().setTitle(R.string.titulo_config);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // muestra la flecha ←
        }

        // Lee el valor guardado (true si es la primera vez) y guarda cada cambio
        SharedPreferences preferencias = getSharedPreferences(PREFERENCIAS, MODE_PRIVATE);
        SwitchMaterial swNotificaciones = findViewById(R.id.swNotificaciones);
        swNotificaciones.setChecked(preferencias.getBoolean(CLAVE_NOTIFICACIONES, true));
        swNotificaciones.setOnCheckedChangeListener((boton, activo) -> {
            preferencias.edit().putBoolean(CLAVE_NOTIFICACIONES, activo).apply();  // apply guarda sin bloquear
            Toast.makeText(this, activo ? R.string.msg_notificaciones_on : R.string.msg_notificaciones_off,
                    Toast.LENGTH_SHORT).show();
        });
    }

    // 3. Se ejecuta al tocar la flecha ← de la Toolbar: cierra y vuelve a MainActivity
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
