package com.prototipo2.androidbasicoapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.location.LocationManagerCompat;
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
    private static final int PERMISO_UBICACION = 100;  // código para reconocer la respuesta del permiso de ubicación
    private static final String SEDE = "Santo Tomás Santiago Centro";  // lo que busca Maps si no hay ubicación
    private static final String URL_SITIO = "https://www.santotomas.cl";
    private static final String TELEFONO = "+56200000000";          // número de ejemplo
    private static final String CORREO = "contacto@ejemplo.cl";     // correo de ejemplo

    // =====================================================================
    // 2. VISTAS (encapsuladas como private)
    // =====================================================================
    private Button btnUbicacion, btnMapa, btnWeb, btnLlamar, btnCorreo, btnCamara;
    private TextView tvUbicacion;
    private ImageView imgFoto;
    private ProgressBar pbFoto;

    // =====================================================================
    // 3. ESTADO (valores que cambian mientras se usa la app)
    // =====================================================================
    private LocationManager locationManager;  // servicio del sistema que entrega la ubicación
    private double latitud, longitud;          // double primitivo: guarda el valor directo
    private boolean ubicacionObtenida = false; // evita abrir el mapa con coordenadas vacías

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
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Eventos de los intents implícitos
        btnUbicacion.setOnClickListener(v -> pedirUbicacion());
        btnMapa.setOnClickListener(v -> abrirMapa());          // I1
        btnWeb.setOnClickListener(v -> abrirSitioWeb());       // I2
        btnLlamar.setOnClickListener(v -> abrirMarcador());    // I3
        btnCorreo.setOnClickListener(v -> enviarCorreo());     // I4

        // Eventos de los intents explícitos (sección 7)
        configurarIntentsExplicitos();
    }

    // =====================================================================
    // 5. INTENTS IMPLÍCITOS (Android elige qué app externa responde)
    // =====================================================================

    // Sensor de ubicación: primero revisa el permiso; si no está, lo pide con el código 100
    private void pedirUbicacion() {
        if (tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION)
                || tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            obtenerUbicacion();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISO_UBICACION);
        }
    }

    // Pide UNA lectura al sensor. Llega en segundo plano y el resultado vuelve al hilo principal.
    private void obtenerUbicacion() {
        if (locationManager == null || !LocationManagerCompat.isLocationEnabled(locationManager)) {
            mostrarMensaje(R.string.msg_activa_gps);  // validación: GPS apagado
            return;
        }
        tvUbicacion.setText(R.string.txt_buscando_ubicacion);
        // Android 12+: el proveedor "fused" combina GPS, Wi-Fi y red. Antes: red si está activa, si no GPS.
        String proveedor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && locationManager.hasProvider(LocationManager.FUSED_PROVIDER)) {
            proveedor = LocationManager.FUSED_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            proveedor = LocationManager.NETWORK_PROVIDER;
        } else {
            proveedor = LocationManager.GPS_PROVIDER;
        }
        try {
            LocationManagerCompat.getCurrentLocation(locationManager, proveedor, new CancellationSignal(),
                    ContextCompat.getMainExecutor(this), ubicacion -> {
                        if (ubicacion == null) {  // validación: el sensor no encontró señal
                            tvUbicacion.setText(R.string.txt_ubicacion_no_encontrada);
                            return;
                        }
                        latitud = ubicacion.getLatitude();
                        longitud = ubicacion.getLongitude();
                        ubicacionObtenida = true;
                        tvUbicacion.setText(getString(R.string.txt_ubicacion, latitud, longitud));
                    });
        } catch (SecurityException e) {  // el permiso se quitó desde Ajustes mientras la app estaba abierta
            tvUbicacion.setText(R.string.txt_sin_permiso_ubicacion);
        }
    }

    // I1 — Google Maps con geo: (si no hay ubicación, busca la sede por nombre)
    private void abrirMapa() {
        String geo;
        if (ubicacionObtenida) {
            // Unir texto + double siempre usa punto decimal (geo: no acepta coma)
            geo = "geo:" + latitud + "," + longitud + "?q=" + latitud + "," + longitud;
        } else {
            geo = "geo:0,0?q=" + Uri.encode(SEDE);
            mostrarMensaje(R.string.msg_mapa_sede);
        }
        abrirIntentSeguro(new Intent(Intent.ACTION_VIEW, Uri.parse(geo)), R.string.error_sin_mapas);
    }

    // I2 — Sitio web: ACTION_VIEW + https:// abre el navegador
    private void abrirSitioWeb() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SITIO));
        abrirIntentSeguro(intent, R.string.error_sin_navegador);
    }

    // I3 — Marcador: ACTION_DIAL solo muestra el número, por eso NO necesita el permiso CALL_PHONE
    private void abrirMarcador() {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + TELEFONO));
        abrirIntentSeguro(intent, R.string.error_sin_telefono);
    }

    // I4 — Correo: "mailto:" hace que solo respondan apps de correo; los extras prellenan los campos
    private void enviarCorreo() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{CORREO});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.correo_asunto));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.correo_cuerpo));
        abrirIntentSeguro(intent, R.string.error_sin_correo);
    }

    // =====================================================================
    // 6. RESPUESTA DE LOS PERMISOS (el usuario tocó "Permitir" o "No permitir")
    // =====================================================================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISO_UBICACION) {
            if (tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION)
                    || tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                obtenerUbicacion();
            } else {
                tvUbicacion.setText(R.string.txt_sin_permiso_ubicacion);
            }
        }
    }

    // =====================================================================
    // 7. INTENTS EXPLÍCITOS (navegan a pantallas de nuestra propia app)
    // =====================================================================
    private void configurarIntentsExplicitos() {
        // Aquí se conectarán los botones de Detalle, Ajustes e Inscripción (E1 a E3)
    }

    // =====================================================================
    // 8. UTILIDADES
    // =====================================================================
    // Abre cualquier intent implícito sin que la app se cierre si no hay una app que responda
    private void abrirIntentSeguro(Intent intent, int mensajeError) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            mostrarMensaje(mensajeError);
        }
    }

    private boolean tienePermiso(String permiso) {
        return ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED;
    }

    private void mostrarMensaje(int idTexto) {
        Toast.makeText(this, idTexto, Toast.LENGTH_SHORT).show();
    }
}
