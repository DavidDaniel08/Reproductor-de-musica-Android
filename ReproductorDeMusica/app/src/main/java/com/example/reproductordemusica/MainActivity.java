package com.example.reproductordemusica;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.example.reproductordemusica.R;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class MainActivity extends AppCompatActivity {

    Button play_pause;
    TextView nombCancion;
    ImageView iv;
    CircularSeekBar circularSeekBar;
    int posicion = 0;
    boolean isUserSeeking = false;
    int contadorClics = 0;
    long ultimoClicTiempo = 0;
    static final int TIEMPO_ENTRE_CLICS = 300; // Tiempo en milisegundos para distinguir entre clic simple y doble

    MediaPlayer vectormp[] = new MediaPlayer[5];
    int[] portadas = {R.drawable.portada1, R.drawable.portada2, R.drawable.portada3, R.drawable.portada4, R.drawable.portadadefault2};
    int[] duraciones = {230000, 256000 , 284000, 198000, 136000}; // Duraciones en milisegundos
    String[] nombresCanciones = {"Daylight", "Am I Dreaming", "Skyfall", "Ojos Marrones", "Rara Vez"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play_pause = findViewById(R.id.btn_play);
        iv = findViewById(R.id.imageView);
        nombCancion = findViewById(R.id.nombre_cancion);
        circularSeekBar = findViewById(R.id.circularSeekBar);

        vectormp[0] = MediaPlayer.create(this, R.raw.daylight);
        vectormp[1] = MediaPlayer.create(this, R.raw.draming);
        vectormp[2] = MediaPlayer.create(this, R.raw.skyfall);
        vectormp[3] = MediaPlayer.create(this, R.raw.ojos);
        vectormp[4] = MediaPlayer.create(this, R.raw.rara);

        // Configurar la duración de la CircularSeekBar
        circularSeekBar.setMax(100);

        // Listener para la CircularSeekBar para controlar la reproducción
        circularSeekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                if (fromUser) {
                    if (vectormp[posicion].isPlaying()) {
                        vectormp[posicion].seekTo((int) (vectormp[posicion].getDuration() * (progress / 100)));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
                isUserSeeking = false;
            }
        });

        // Obtener los colores del primer álbum
        obtenerColoresAlbumArt(0);
        // Mostrar el nombre de la primera canción
        nombCancion.setText(nombresCanciones[0]);
    }

    // Método para cambiar de activity
    public void SiguienteActivity(View view){
        Intent sig = new Intent(this, MainActivity2.class);
        startActivity(sig);
    }

    // Método para el botón Play/Pause
    public void PlayPause(View view) {
        if (vectormp[posicion].isPlaying()) {
            vectormp[posicion].pause();
            play_pause.setBackgroundResource(R.drawable.reproducir);
            Toast.makeText(this, "Pausa", Toast.LENGTH_SHORT).show();
        } else {
            vectormp[posicion].start();
            play_pause.setBackgroundResource(R.drawable.pausa);
            Toast.makeText(this, "Play", Toast.LENGTH_SHORT).show();
            updateSeekBar();
        }
    }

    // Método para actualizar la CircularSeekBar mientras la canción se reproduce
    private void updateSeekBar() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (vectormp[posicion].isPlaying()) {
                    try {
                        Thread.sleep(1000); // Actualizar cada segundo
                        if (!isUserSeeking) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    float progress = ((float) vectormp[posicion].getCurrentPosition() / vectormp[posicion].getDuration()) * 100;
                                    circularSeekBar.setProgress(progress);
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // Método para la canción anterior
    public void Anterior(View view) {
        // Incrementar el contador de clics
        contadorClics++;

        if (contadorClics == 1) {
            // Programar un temporizador para restablecer el contador después de un cierto tiempo
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (contadorClics == 1) {
                        // Si el contador de clics sigue siendo 1, ejecutar el código para un solo clic
                        ejecutarRetrocesoCancion();
                    } else {
                        // Si se detecta un doble clic, retrocede 5 segundos y restablece el contador de clics
                        if (vectormp[posicion].isPlaying()) {
                            int newPosition = vectormp[posicion].getCurrentPosition() - 5000; //  5000 milisegundos (5 segundos)
                            if (newPosition < 0) {
                                newPosition = 0; // Asegura que la posición no sea negativa
                            }
                            vectormp[posicion].seekTo(newPosition);
                        }
                        contadorClics = 0;
                    }
                }
            }, TIEMPO_ENTRE_CLICS);
        }
    }

    // Método para ejecutar el código para retroceder la canción
    private void ejecutarRetrocesoCancion() {
        if (posicion > 0) {
            vectormp[posicion].pause();
            vectormp[posicion].seekTo(0); // Reiniciar la canción al principio
            posicion--;
            actualizarPortada(posicion);
            obtenerColoresAlbumArt(posicion);
            // Configurar la duración de la CircularSeekBar para la nueva canción
            circularSeekBar.setMax(100);
            circularSeekBar.setProgress(0); // Reiniciar la posición de la barra de progreso
            vectormp[posicion].start();
            // Actualizar el nombre de la canción
            nombCancion.setText(nombresCanciones[posicion]);
        } else {
            Toast.makeText(this, "No hay más canciones", Toast.LENGTH_SHORT).show();
        }
        contadorClics = 0; // Reiniciar el contador de clics después de ejecutar el retroceso de canción
    }

    // Método para la siguiente canción
    public void Siguiente(View view) {
        // Incrementar el contador de clics
        contadorClics++;

        if (contadorClics == 1) {
            // Programar un temporizador para restablecer el contador después de un cierto tiempo
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (contadorClics == 1) {
                        // Si el contador de clics sigue siendo 1, ejecutar el código para un solo clic
                        ejecutarCambioCancion();
                    } else {
                        // Si se detecta un doble clic, adelanta 5 segundos y restablece el contador de clics
                        if (vectormp[posicion].isPlaying()) {
                            int newPosition = vectormp[posicion].getCurrentPosition() + 5000; // 5000 milisegundos (5 segundos)
                            vectormp[posicion].seekTo(newPosition);
                        }
                        contadorClics = 0;
                    }
                }
            }, TIEMPO_ENTRE_CLICS);
        }
    }

    // Método para ejecutar el código para cambiar de canción
    private void ejecutarCambioCancion() {
        if (posicion < vectormp.length - 1) {
            vectormp[posicion].pause();
            vectormp[posicion].seekTo(0); // Reiniciar la canción al principio
            posicion++;
            actualizarPortada(posicion);
            obtenerColoresAlbumArt(posicion);
            // Configurar la duración de la CircularSeekBar para la nueva canción
            circularSeekBar.setMax(100);
            circularSeekBar.setProgress(0); // Reiniciar la posición de la barra de progreso
            vectormp[posicion].start();
            // Actualizar el nombre de la canción
            nombCancion.setText(nombresCanciones[posicion]);
            updateSeekBar(); // Iniciar la actualización de la barra de progreso
        } else {
            Toast.makeText(this, "No hay más canciones", Toast.LENGTH_SHORT).show();
        }
        contadorClics = 0; // Reiniciar el contador de clics después de ejecutar el cambio de canción
    }

    // Método para actualizar la portada de la canción
    private void actualizarPortada(int posicion) {
        iv.setImageResource(portadas[posicion]);
    }

    // Método para obtener los colores principales del arte del álbum
    private void obtenerColoresAlbumArt(int posicion) {
        Bitmap albumArtBitmap = BitmapFactory.decodeResource(getResources(), portadas[posicion]);
        Palette.from(albumArtBitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(Palette palette) {
                int defaultColor = getResources().getColor(android.R.color.black);
                int color1 = palette.getDominantColor(defaultColor);
                int color2 = palette.getLightVibrantColor(defaultColor);
                int color3 = palette.getDarkVibrantColor(defaultColor);

                // Crear el degradado de colores
                GradientDrawable gradientDrawable = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{color1, color2, color3}
                );

                // Establecer el fondo del reproductor de música
                findViewById(R.id.main).setBackground(gradientDrawable);
            }
        });
    }
}
