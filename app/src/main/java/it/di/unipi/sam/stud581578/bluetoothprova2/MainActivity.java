package it.di.unipi.sam.stud581578.bluetoothprova2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    BluetoothAdapter bluetoothAdapter;
    MediaPlayer mp;
    int[] audios = new int[]{R.raw.gervasi_audio_1, R.raw.gervasi_audio_2, R.raw.gervasi_audio_3, R.raw.gervasi_audio_4, R.raw.gervasi_audio_5, R.raw.gervasi_audio_6, R.raw.gervasi_audio_7,
            R.raw.gervasi_audio_8, R.raw.gervasi_audio_9};
    Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //imposto il layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mp = new MediaPlayer();


        //prendo i pulsanti per settare il clickListener
        Button b0 = findViewById(R.id.button_server);
        b0.setOnClickListener(this);
        Button b1 = findViewById(R.id.button_client);
        b1.setOnClickListener(this);

        Button b2 = findViewById(R.id.button_extra);
        b2.setOnClickListener(this);

        //prendo il defaultAdapter del bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            PublicConstants.bluetoothAdapter = bluetoothAdapter;

            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }

        

    }

    @Override
    protected void onResume() {
        super.onResume();
        //se per qualche motivo ho attivo un thread, lo chiudo
        MyConnectionThread.destroyMyConnectionThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //se per qualche motivo ho attivo un thread, lo chiudo
        MyConnectionThread.destroyMyConnectionThread();
        mp.release();
    }


    @Override
    public void onClick(View v) {

        //se l'utente vuole fare da client
        if(v.getId() == R.id.button_client) {


            //se il bluetooth non è supportato, amen (TODO: implementare le altre funzionalità dell'app (che saranno strepitose))
            if(bluetoothAdapter == null){
                Toast.makeText(getApplicationContext(), "Il dispositivo non supporta il bluetooth. Prova altre funzionalità della app però!", Toast.LENGTH_SHORT).show();
            }else {

                //se il bluetooth non è abilitato, chiedo di abilitarlo
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, PublicConstants.CLIENT_REQUEST_CODE);
                } else {
                    //lancio l'activity SOLO se mi viene dato il permesso di usare il bluetooth
                    Intent intent = new Intent(this, ClientResearchActivity.class);
                    this.startActivity(intent);
                    Log.d("MAIN", "GIA' ATTIVO!");
                }
            }
        }

        //se l'utente vuole fare da server, lancio un intent discoverable per rendermi visibile agli altri dispositivi nelle vicinanze
        if(v.getId() == R.id.button_server) {

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, PublicConstants.DISCOVERABILITY_DURATION);
            startActivityForResult(discoverableIntent, PublicConstants.SERVER_REQUEST_CODE);

        }

        if(v.getId() == R.id.button_extra){

            mp.reset();
            int choice = random.nextInt(9);
            AssetFileDescriptor audio = getResources().openRawResourceFd(audios[choice]);
            try {
                mp.setDataSource(audio.getFileDescriptor(), audio.getStartOffset(), audio.getLength());
                audio.close();
                mp.prepare();
            }catch(IOException e){
                e.printStackTrace();
            }
            mp.start();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        //se il request code è relativo all'attivazione del bluetooth come client, vedo se l'utente mi ha dato l'ok per attivarlo
        if(requestCode == PublicConstants.CLIENT_REQUEST_CODE){

            //se all'utente sta bene attivare il bluetooth, lancio l'activity successiva
            if(resultCode == RESULT_OK){
                Intent intent = new Intent(this, ClientResearchActivity.class);
                this.startActivity(intent);
            }

            //se invece l'utente non vuole attivare il bluetooth, non succede niente
            if(resultCode == RESULT_CANCELED){
                Log.d("MAIN ACTIVITY", "Bluetooth NON attivo (client)! :(");
            }
        }

        //se il request code è relativo all'attivazione del bluetooth come server (il device vuole rendersi discoverable)...
        if(requestCode == PublicConstants.SERVER_REQUEST_CODE){

            Log.d("DISCOVERABLE", "ENTRATO");

            //se l'utente ha accettato di rendersi discoverable, lancio l'activity successiva
            if(resultCode == PublicConstants.DISCOVERABILITY_DURATION){
                Log.d("DISCOVERABLE", "SI");
                Intent intent = new Intent(this, ServerResearchActivity.class);
                this.startActivity(intent);
            }

            //sennò pace, non succede nulla
            if(resultCode == 0){
                Log.d("DISCOVERABLE", "NO");
                Log.d("MAIN ACTIVITY", "Bluetooth NON attivo (server)! :(");
            }

        }


    }

}