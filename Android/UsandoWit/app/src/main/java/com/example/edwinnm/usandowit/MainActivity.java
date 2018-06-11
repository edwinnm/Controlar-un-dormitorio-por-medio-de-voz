package com.example.edwinnm.usandowit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.AlarmClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class MainActivity extends AppCompatActivity implements AIListener, View.OnClickListener {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mSocket=null;
    BluetoothDevice mDevice;
    OutputStream mOutputStream;

    MediaPlayer mediaPlayer =null;
    ArrayList <Musica> todasCanciones;


    private EditText etInput;
    private TextView tvRespuesta;
    private ImageButton mSpeakBtn;
    private ImageButton enviarPeticion;


    private AIService aiService;
    private AIDataService aiDataService;

    String CLIENT_ACCESS_TOKEN = "Ingresa tu token proporcionado por Dialogflow";

    //Estados del sistema

    boolean conectado= true, prendido, abierto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etInput = findViewById(R.id.textInput);
        tvRespuesta = findViewById(R.id.textOutput);
        mSpeakBtn = findViewById(R.id.mSpeakBtn);
        enviarPeticion = findViewById(R.id.btnEnviarPeticion);

        todasCanciones = encontrarCanciones(Environment.getExternalStorageDirectory());

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = new MediaPlayer();

        //Creo el BluetoothAdapter
        //Compruebo que tenga bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "El dipositivo no tiene bluetooth ", Toast.LENGTH_SHORT).show();
        }

        //Compruebo que el bluetooth esté activado
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }



        //Configuro la interaccion con DialogFlow
        final AIConfiguration config = new AIConfiguration(CLIENT_ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.Spanish,
                AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        aiDataService = new AIDataService(config);


        mSpeakBtn.setOnClickListener(this);
        enviarPeticion.setOnClickListener(this);

    }

    void openBT(String nombreDispositivo) throws IOException {
        // Obtengo todos los dispositivos sincronizados

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // Si existen dispositivos sincronizados

        boolean deviceFound = false;

        if (pairedDevices.size() > 0) {
            // Recorro todos los dispositivos sincronizados
            for (BluetoothDevice device : pairedDevices) {

                if (device.getName().equals(nombreDispositivo)){
                    mDevice = device;
                    deviceFound = true;
                    break;
                }
            }
        }
        if (deviceFound){
            //ID estándar
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();
            mOutputStream = mSocket.getOutputStream();

            showMessage ("Bluetooth conectado", Toast.LENGTH_SHORT);
        }else{
            showMessage("No se ha encontrado el dispositivo indicado", Toast.LENGTH_LONG);
        }

    }

    void endBT() throws IOException{
        mOutputStream.close();
        mSocket.close();
        conectado=false;
    }

    //Envío del estado deseado
    private void sendBT(String msg){
        try{

            mOutputStream.write(msg.getBytes());
            showMessage("Datos enviados al modulo bluetooth", Toast.LENGTH_SHORT);
        }catch (Exception e){
            showMessage("Error en el envío", Toast.LENGTH_SHORT);
        }
    }

    //Metodos de AI Listerner (DialogFlow)
    @Override
    public void onResult(AIResponse response) {

        boolean reproducir = false;
        boolean alarma = false;

        String artista="N/A";
        String nombreCancion = "N/A";
        String hora="0", minuto="0", dia="0";

        Result result = response.getResult();

        //Obtener Parametros

        if(result.getParameters() != null && !result.getParameters().isEmpty()){

            for(final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()){

                //Control de Luces
                if(entry.getKey().equals("actionlights")){


                    if((entry.getValue()).toString().contains("prender")){

                        try {
                            if(mSocket==null){
                                openBT("HC-05");
                            }
                            if(!prendido){
                                sendBT("1");
                                prendido=true;
                                mensajeRespuesta("Se ha prendido la luz.");
                            }else{
                                mensajeRespuesta("La luz ya se encuentra prendida.");
                            }



                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else if((entry.getValue()).toString().contains("apagar")){
                        if(prendido){
                            sendBT("0");
                            prendido=false;
                            mensajeRespuesta("Se ha apagado la luz.");
                        }else {
                            mensajeRespuesta("La luz ya se encuentra apagada.");
                        }

                    }


                // Control de entretenimiento con música

                }else if(entry.getKey().equals("control-song")){


                    if((entry.getValue()).toString().contains("play")) {

                        reproducir = true;

                    }else if((entry.getValue()).toString().contains("pause")){

                        if(mediaPlayer.isPlaying()){
                            mediaPlayer.pause();
                        }else {
                            mensajeRespuesta("NO hay una canción reproduciendose.");
                        }
                    }
                    else if((entry.getValue()).toString().contains("continue")){
                        if(!mediaPlayer.isPlaying()){
                            mediaPlayer.start();
                        }else {
                            mensajeRespuesta("NO hay una canción en estado Pause.");
                        }
                    }else if((entry.getValue()).toString().contains("stop")){
                        if(mediaPlayer.isPlaying()){
                            mediaPlayer.stop();
                        }else {
                            mensajeRespuesta("NO hay una canción reproduciendose.");
                        }
                    }
                    else if((entry.getValue()).toString().contains("change")){
                        if(mediaPlayer.isPlaying()){
                            mediaPlayer.stop();

                            reproducirMusica(aleatorio());
                            mensajeRespuesta("Se ha cambiado de canción al azar.");
                        }else{
                            mensajeRespuesta("No se puede cambiar de canción, puesto que no está reproduciendo nada.");
                        }

                    }
                }else if(entry.getKey().equals("artist")){
                    artista = entry.getValue().toString();
                    artista = artista.substring(1, artista.length()-1);


                }else if(entry.getKey().equals("songname")){
                    nombreCancion = entry.getValue().toString();
                    nombreCancion = nombreCancion.substring(1, nombreCancion.length()-1);


                }
                //Control Alarma
                else if(entry.getKey().equals("control-alarma")){

                    if(entry.getValue().toString().contains("Programa") ){

                        alarma = true;
                    }

                }else if(entry.getKey().equals("time")){

                    hora = entry.getValue().toString().substring(1,3);
                    minuto = entry.getValue().toString().substring(4,6);

                }else if(entry.getKey().equals("date")){

                    dia = entry.getValue().toString().substring(9,11);

                }

                //Control de Cortinas
                else if(entry.getKey().equals("action-cortinas")){

                    if(entry.getValue().toString().contains("open")){
                        try {
                            if(mSocket==null){
                                openBT("HC-05");
                            }
                            if(!abierto){
                                sendBT("3");
                                abierto = true;
                                mensajeRespuesta("Se han abierto las persianas.");
                            }else{
                                mensajeRespuesta("No se pueden abrir las persianas, Ya están abiertas!.");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }



                    }else if(entry.getValue().toString().contains("close")){

                        try {
                            if(mSocket==null){
                                openBT("HC-05");
                            }
                        if(abierto) {
                            sendBT("4");
                            abierto =false;
                            mensajeRespuesta("Se han cerrado las persinas.");
                        }else{
                            mensajeRespuesta("No se pueden cerrar las persianas, Ya están cerradas!.");
                        }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }


                }else if(entry.getKey().equals("action-b")){
                    if(entry.getValue().toString().contains("Desconectar")){
                        try{
                            endBT();
                            mensajeRespuesta("Se ha desconectado la conexión bluetooth.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }else{
            tvRespuesta.setText(result.getFulfillment().getSpeech());
        }

        Musica aReproducir;
        boolean seReproducio = false;
        if(reproducir){
            //Solo me da la canción
            if(!nombreCancion.equalsIgnoreCase("N/A")){
                for(int i=0; i<todasCanciones.size(); i++){
                    if(todasCanciones.get(i).getNombre().equalsIgnoreCase(nombreCancion) && todasCanciones.get(i).getArtista().equalsIgnoreCase(artista)){
                        aReproducir = todasCanciones.get(i);

                        reproducirMusica(aReproducir);
                        seReproducio = true;
                        break;

                    }else if(todasCanciones.get(i).getNombre().equalsIgnoreCase(nombreCancion)){
                        aReproducir = todasCanciones.get(i);
                        mediaPlayer.stop();
                        reproducirMusica(aReproducir);
                        seReproducio = true;
                        break;
                    }
                }
            }
            //Solo me da el artista
            else if(nombreCancion.equalsIgnoreCase("N/A")){

                aReproducir = aleatorioArtista(artista);
                if(aReproducir != null ) {
                    mediaPlayer.stop();
                    reproducirMusica(aReproducir);
                    seReproducio = true;
                }

            }

            if(!seReproducio){
                mensajeRespuesta("No se encontró la cancion y/o artista.");
            }else{
                mensajeRespuesta("Reproduciendo la pista.");
            }

        }

        if(alarma){
            startAlarma(hora, minuto);
            mensajeRespuesta("Alarma programada.");
        }

        //Muestro los resultados

        etInput.setText(result.getResolvedQuery());




    }


    @Override
    public void onError(AIError error) {
       tvRespuesta.setText(error.toString());

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

    @Override
    public void onClick(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etInput.getWindowToken(), 0);
        switch (view.getId()) {
            case R.id.mSpeakBtn:
                aiService.startListening();
                break;
            case R.id.btnEnviarPeticion:

                final AIRequest aiRequest = new AIRequest();
                if(!etInput.getText().toString().equals("")){
                    aiRequest.setQuery(etInput.getText().toString());
                    new AsyncTask<AIRequest, Void, AIResponse>() {
                        private AIError aiError;

                        @Override
                        protected AIResponse doInBackground(AIRequest... requests) {

                            try {
                                final AIResponse response = aiDataService.request(aiRequest);
                                return response;
                            } catch (AIServiceException e) {
                                aiError = new AIError(e);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(AIResponse aiResponse) {
                            if (aiResponse != null) {
                                onResult(aiResponse);
                            } else {
                                onError(aiError);
                            }
                        }
                    }.execute(aiRequest);
                }else{
                    mensajeRespuesta("Escriba su petición.");
                }

                break;
        }


    }


    private Musica aleatorioArtista(String artista){
        Musica m = null;
        ArrayList<Musica> soloDeEsteArtista = new ArrayList<>();
        int i;

        for (i=0; i<todasCanciones.size(); i++){

            if(todasCanciones.get(i).getArtista().equalsIgnoreCase(artista)){
                soloDeEsteArtista.add(todasCanciones.get(i));
            }
        }

        if(soloDeEsteArtista.size()>0){

            m = soloDeEsteArtista.get((int) (Math.random() * soloDeEsteArtista.size()));
        }



        return m;
    }

    private Musica aleatorio(){
        Musica m;
        m = todasCanciones.get((int) (Math.random() * todasCanciones.size()));
        return m;
    }
    public ArrayList<Musica> encontrarCanciones(File root){
        ArrayList<Musica> arrayCanciones = new ArrayList<>();
        File [] archivos = root.listFiles();
        //Busca todos los archivos y directorios
        for(File archivo : archivos){
            //Si un archivo es directorio, realiza un proceso recursivo
            if(archivo.isDirectory() && !archivo.isHidden()){
                arrayCanciones.addAll(encontrarCanciones(archivo));
            }else{
                if(archivo.getName().endsWith(".mp3")){
                    String nombre;
                    String artista = "";
                    String genero = "";
                    String [] partes = archivo.getName().split("-");
                    if(partes.length == 3){
                        nombre = partes[0] ;
                        artista = partes [1] ;
                        genero = partes[2].replace(".mp3","");
                    }
                    else if(partes.length == 2){
                        nombre = partes[0] ;
                        artista = partes [1].replace(".mp3","") ;
                    }
                    else{
                        nombre = archivo.getName().toString().replace(".mp3", "");
                    }

                    String direccion = archivo.getPath().toString() ;

                    Musica m = new Musica(nombre, artista, genero, direccion, archivo);

                    arrayCanciones.add(m);
                }

            }
        }

        return arrayCanciones;
    }

    private void reproducirMusica(Musica musica){
    mediaPlayer=new MediaPlayer();
        if(!mediaPlayer.isPlaying() || mediaPlayer==null){

            Uri uri = Uri.parse(musica.getDireccion());
            try{
                mediaPlayer.setDataSource(getApplicationContext(), uri);
                mediaPlayer.prepare();
                mediaPlayer.start();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    //Muestra mensajes en pantalla
    private void showMessage(String msg, int time){
        Toast toast = Toast.makeText(MainActivity.this, msg, time);
        toast.show();
    }

    private void mensajeRespuesta(String mensaje){
        tvRespuesta.setText(mensaje);
    }

    public void startAlarma(String horaAlarma, String minutoAlarma) {

        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);

        intent.putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(horaAlarma));
        intent.putExtra(AlarmClock.EXTRA_MINUTES,Integer.parseInt(minutoAlarma) );
        startActivity(intent);

    }


}
