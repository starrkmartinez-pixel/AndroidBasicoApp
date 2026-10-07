package com.prototipo2.androidbasicoapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Patterns;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.location.LocationManagerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

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
    private static final int PERMISO_CAMARA = 200;     // código para reconocer la respuesta del permiso de cámara
    private static final String SEDE = "Santo Tomás Santiago Centro";  // lo que busca Maps si no hay ubicación
    private static final String URL_SITIO = "https://www.santotomas.cl";
    private static final String TELEFONO = "+56200000000";          // número de ejemplo
    private static final String CORREO = "contacto@ejemplo.cl";     // correo de ejemplo
    private static final String CARPETA_FOTOS = "AndroidBasicoApp";        // subcarpeta dentro de Imágenes

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
    private Uri uriFoto;                       // dónde la cámara guarda la foto (null = no hay foto en curso)

    // Espera el resultado de la cámara. Se registra como atributo, nunca dentro de un clic.
    private final ActivityResultLauncher<Intent> lanzadorCamara = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            resultado -> {
                if (resultado.getResultCode() == RESULT_OK && uriFoto != null) {
                    mostrarVistaPrevia(uriFoto);
                    mostrarMensaje(R.string.msg_foto_guardada);
                } else {
                    // Validación: si se canceló, borramos el archivo vacío que reservamos en la galería
                    if (uriFoto != null) getContentResolver().delete(uriFoto, null, null);
                    mostrarMensaje(R.string.msg_foto_cancelada);
                }
                uriFoto = null;
            });

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
        btnCamara.setOnClickListener(v -> pedirCamara());      // I5

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

    // I5 — Cámara: revisa el permiso; si no está, lo pide con el código 200
    private void pedirCamara() {
        if (tienePermiso(Manifest.permission.CAMERA)) {
            abrirCamara();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISO_CAMARA);
        }
    }

    // Reserva un archivo en la galería (Imágenes/AndroidBasicoApp) y le pide a la cámara que guarde ahí la foto
    private void abrirCamara() {
        ContentValues datos = new ContentValues();
        datos.put(MediaStore.Images.Media.DISPLAY_NAME, "AndroidBasicoApp_" + System.currentTimeMillis() + ".jpg");
        datos.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        datos.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + CARPETA_FOTOS);
        uriFoto = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, datos);
        if (uriFoto == null) {  // validación: no se pudo crear el archivo
            mostrarMensaje(R.string.error_preparar_foto);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto);
        try {
            lanzadorCamara.launch(intent);  // launch: esperamos el resultado (OK o cancelado)
        } catch (ActivityNotFoundException e) {
            getContentResolver().delete(uriFoto, null, null);
            uriFoto = null;
            mostrarMensaje(R.string.error_sin_camara);
        }
    }

    // THREAD: hacer la miniatura de una foto de varios MB tarda; en el hilo principal congelaría la pantalla
    private void mostrarVistaPrevia(Uri uri) {
        pbFoto.setVisibility(View.VISIBLE);
        new Thread(() -> {
            Bitmap miniatura = null;
            try {
                miniatura = getContentResolver().loadThumbnail(uri, new Size(800, 800), null);
            } catch (IOException e) {
                // se queda en null y se avisa abajo
            }
            Bitmap resultado = miniatura;  // las lambdas solo pueden usar variables que no cambian
            runOnUiThread(() -> {          // solo el hilo principal puede modificar las vistas
                pbFoto.setVisibility(View.GONE);
                if (resultado == null) {
                    mostrarMensaje(R.string.error_vista_previa);
                    return;
                }
                imgFoto.setImageBitmap(resultado);
                imgFoto.setVisibility(View.VISIBLE);
            });
        }).start();  // start() crea el hilo nuevo; run() lo ejecutaría en el hilo principal
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
        } else if (requestCode == PERMISO_CAMARA) {
            if (tienePermiso(Manifest.permission.CAMERA)) {
                abrirCamara();
            } else {
                mostrarMensaje(R.string.msg_sin_permiso_camara);
            }
        }
    }

    // =====================================================================
    // 7. INTENTS EXPLÍCITOS (navegan a pantallas de nuestra propia app)
    // =====================================================================
    private Button btnDetalle;
    private Button btnConfig;
    private Button btnEnviar;
    private TextInputLayout tilNombre, tilCorreo;
    private TextInputEditText etNombre, etCorreo;
    private TextView tvEstadoInscripcion;

    // E3: espera la respuesta de ConfirmActivity (OK = confirmó; CANCELED = canceló o volvió atrás)
    private final ActivityResultLauncher<Intent> lanzadorConfirmacion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    Intent datos = resultado.getData();  // validación: la respuesta puede venir sin datos
                    String hora = (datos != null) ? datos.getStringExtra(ConfirmActivity.EXTRA_HORA) : null;
                    tvEstadoInscripcion.setText(getString(R.string.txt_inscripcion_ok,
                            hora != null ? hora : "--:--"));
                    etNombre.setText("");
                    etCorreo.setText("");
                } else {
                    tvEstadoInscripcion.setText(R.string.txt_inscripcion_cancelada);
                }
            });

    private void configurarIntentsExplicitos() {
        btnDetalle = findViewById(R.id.btnDetalle);
        btnConfig = findViewById(R.id.btnConfig);
        btnEnviar = findViewById(R.id.btnEnviar);
        tilNombre = findViewById(R.id.tilNombre);
        tilCorreo = findViewById(R.id.tilCorreo);
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        tvEstadoInscripcion = findViewById(R.id.tvEstadoInscripcion);

        btnDetalle.setOnClickListener(v -> abrirDetalle());  // E1
        // E2 — MainActivity → ConfigActivity (la clase destino se indica con .class)
        btnConfig.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ConfigActivity.class)));
        btnEnviar.setOnClickListener(v -> enviarInscripcion());  // E3
    }

    // E1 — MainActivity → DetalleActivity enviando datos con putExtra
    private void abrirDetalle() {
        Intent intent = new Intent(MainActivity.this, DetalleActivity.class);
        intent.putExtra(DetalleActivity.EXTRA_TITULO, getString(R.string.detalle_biblioteca_titulo));
        intent.putExtra(DetalleActivity.EXTRA_DESCRIPCION, getString(R.string.detalle_biblioteca_descripcion));
        intent.putExtra(DetalleActivity.EXTRA_CAPACIDAD, 120);
        startActivity(intent);
    }

    // E3 — Valida el formulario y abre ConfirmActivity esperando su respuesta
    private void enviarInscripcion() {
        String nombre = leerTexto(etNombre);
        String correo = leerTexto(etCorreo);
        // Validaciones: setError muestra el mensaje en rojo bajo el campo (null lo borra)
        tilNombre.setError(nombre.length() < 3 ? getString(R.string.error_nombre) : null);
        tilCorreo.setError(Patterns.EMAIL_ADDRESS.matcher(correo).matches() ? null : getString(R.string.error_correo));
        if (tilNombre.getError() != null || tilCorreo.getError() != null) return;  // no navegamos con datos malos

        Intent intent = new Intent(MainActivity.this, ConfirmActivity.class);
        intent.putExtra(ConfirmActivity.EXTRA_NOMBRE, nombre);
        intent.putExtra(ConfirmActivity.EXTRA_CORREO, correo);
        lanzadorConfirmacion.launch(intent);  // launch y no startActivity: esperamos una respuesta
    }

    // Validación de null: getText() puede devolver null
    private String leerTexto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
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
