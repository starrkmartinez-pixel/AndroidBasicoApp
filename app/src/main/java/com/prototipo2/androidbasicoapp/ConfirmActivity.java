package com.prototipo2.androidbasicoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * =====================================================================
 *  ARCHIVO : ConfirmActivity.java
 *  QUÉ HACE: Destino del INTENT EXPLÍCITO 3. Muestra los datos de la
 *            inscripción y DEVUELVE un resultado a MainActivity:
 *            RESULT_OK (confirmó) o RESULT_CANCELED (canceló).
 *  ESTRUCTURA: 1. Claves de los extras  2. onCreate  3. Botones
 * =====================================================================
 */
public class ConfirmActivity extends AppCompatActivity {

    // 1. CLAVES DE LOS EXTRAS (ida: nombre y correo · vuelta: hora de confirmación)
    public static final String EXTRA_NOMBRE = "extra_nombre";
    public static final String EXTRA_CORREO = "extra_correo";
    public static final String EXTRA_HORA = "extra_hora";

    // 2. CICLO DE VIDA
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE);
        String correo = getIntent().getStringExtra(EXTRA_CORREO);
        // Validación: sin datos no hay nada que confirmar → devolvemos "cancelado"
        if (nombre == null || correo == null) {
            Toast.makeText(this, R.string.msg_faltan_datos, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        TextView tvResumen = findViewById(R.id.tvResumen);
        tvResumen.setText(getString(R.string.txt_resumen, nombre, correo));

        // 3. BOTONES: setResult va ANTES de finish() para que MainActivity reciba la respuesta
        findViewById(R.id.btnConfirmar).setOnClickListener(v -> {
            Intent respuesta = new Intent();  // Intent vacío: solo transporta datos de vuelta
            respuesta.putExtra(EXTRA_HORA, new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            setResult(RESULT_OK, respuesta);
            finish();
        });
        findViewById(R.id.btnCancelar).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        // Si el usuario usa el botón Atrás del sistema, Android devuelve RESULT_CANCELED solo
    }
}
