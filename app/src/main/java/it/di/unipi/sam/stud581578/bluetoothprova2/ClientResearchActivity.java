package it.di.unipi.sam.stud581578.bluetoothprova2;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ClientResearchActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener{

    private ListView lv;
    private ArrayList<String> al;
    private ViewGroup layout;
    private ArrayAdapter adapter;
    private HashSet<BluetoothDevice> pairedDevices;
    private ServiceConnection connection;

    private String lastContactedDevice = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //imposto il layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_research);

        //prendo il layout (mi può servire) e il bluetooth adapter
        layout = findViewById(R.id.activity_client_research);

        //prendo il pulsante che sta su questa activity
        Button b = findViewById(R.id.button_discover);
        b.setOnClickListener(this);


        //se c'è il bluetooth voglio intanto mostrare i dispositivi accoppiati.
        //dato che il set restituito da getBondedDevices è unmodifiable, e invece io ho bisogno di uno modifiable, uso due set:
        //-uno per prendere i bondedDevices
        //-l'altro per metterci i bondedDevices e i NUOVI dispositivi trovati
        Set<BluetoothDevice> s1 = PublicConstants.bluetoothAdapter.getBondedDevices();
        pairedDevices = new HashSet<BluetoothDevice>();
        pairedDevices.addAll(s1);
        s1 = null;
        al = new ArrayList<String>();


        //se ci sono già dispositivi accoppiati, prendo i loro nomi per mostrarli a schermo
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                al.add(deviceName);
            }
        }else{
            Log.d("CLIENT RESEARCH ACTIV", " Non conosco dispositivi con cui sono già accoppiato");
        }

        //prendo le due textview superiori per dire all'utente qual è il nome del suo dispositivo
        TextView view_name = findViewById(R.id.textview_name);
        view_name.setText(PublicConstants.bluetoothAdapter.getName());

        //associo la listview alla lista di dispositivi disponibili con l'adapter (TODO: forse si può evitare l'arrayAdapter)
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, al);
        lv = findViewById(R.id.listview);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);



        //se non imposto qui il collegamento al service, non funziona (amen)
        //quindi qui lego l'activity corrente al service (anche se poi le sue funzioni le userò nel thread ma vabbè)
        connection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                synchronized (MyConnectionThread.lock_service) {
                    //ci siamo legati al Service, prendiamo un riferimento a lui e indichiamo che il service è bound, e quindi possiamo
                    //cominciare ad usarlo
                    ConnectionBluetoothService.LocalBinder binder = (ConnectionBluetoothService.LocalBinder) service;
                    MyConnectionThread.my_bluetooth_service = binder.getService();
                    MyConnectionThread.service_bound = true;
                    MyConnectionThread.lock_service.notify();
                    Log.d("ON SERVICE CONNECTED", "ok!");
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                synchronized (MyConnectionThread.lock_service) {
                    //se per un qualche motivo il service è stato disconnesso, lo segno su service_bound
                    MyConnectionThread.service_bound = false;
                    Log.d("ON SERVICE DISCON", "What?");
                }
            }
        };

        //connetto l'activity al service
        Intent intent = new Intent(this, ConnectionBluetoothService.class);
        boolean what = bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    //quando l'activity non è più visibile faccio l'unbind
    @Override
    protected void onDestroy(){
        super.onDestroy();
        //devo slacciarmi dal service
        synchronized (MyConnectionThread.lock_service) {
            //faccio l'unbind
            unbindService(connection);

            //segno che il service non è più bound
            MyConnectionThread.service_bound = false;
        }
    }



    //broadcastReceiver per scoprire nuovi dispositivi
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //se ho trovato un dispositivo con cui accoppiarmi
            Log.d("CLIENT RESEARCH ACTI", "Debug 2");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //prendo il "descrittore" del device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("CLIENT RESEARCH ACTI", "Debug 3");
                //DEBUG
                if(al == null){
                    Log.d("ON RECEIVE", "ArrayList null");
                }
                if(adapter == null){
                    Log.d("ON RECEIVE", "Adapter null");
                }
                if(pairedDevices == null){
                    Log.d("ON RECEIVE", "pairedDevices null");
                }

                String deviceName = device.getName();

                //se il dispositivo che ho trovato non è null, ha un nome e non è già presente nella lista...
                if(device != null && deviceName != null && !al.contains(deviceName)) {

                    Log.d("CLIENT RESEARCH ACTI", "Nuovo dispositivo trovato In particolare, ho trovato " + deviceName);

                    //aggiungo il dispostivo trovato al mio insieme di dispositivi disponibili
                    pairedDevices.add(device);

                    //aggiungo il dispositivo alla sorgente dati della listview, notificando l'adapter che deve aggiornarsi
                    al.add(deviceName);
                    adapter.notifyDataSetChanged();

                    //invalido il layout perché l'UI deve aggiornarsi
                    layout.invalidate();

                    Log.d("CLIENT RESEARCH ACTI", "La lista attuale di dispositivi è " + al);

                }else{
                    Log.d("ON RECEIVE", "Il dispositivo è null o è già presente nella lista");
                }
            }

            //DEBUG
            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d("ON RECEIVE", "INIZIA LA SCOPERTA DI NUOVI DISPOSITIVI");
            }
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d("ON RECEIVE", "TERMINA LA SCOPERTA DI NUOVI DISPOSITIVI. ADDIO, ADDIO, AMICI ADDIO.");
            }

        }
    };


    @Override
    public void onClick(View v) {

        //se l'utente vuole scoprire nuovi dispositivi vicini
        if(v.getId()==R.id.button_discover){
            try {
                //Registro un broadcastReceiver per essere notificato quando viene trovato un nuovo dispositivo
                IntentFilter filter0 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(receiver, filter0);

                //DEBUG
                IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                registerReceiver(receiver, filter1);
                IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(receiver, filter2);

                Log.d("CLIENT RESEARCH ACTI", "Debug 1");

                //faccio partire la discovery di nuovi dispositivi
                PublicConstants.bluetoothAdapter.startDiscovery();
            }catch(Exception e){
                //
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //devo deregistrare il broadcastReceiver
        try {
            //Toast.makeText(getApplicationContext(), "Register deregistrato",Toast.LENGTH_SHORT).show();
            Log.d("RECEIVER", "HO DEREGISTRATO CORRETTAMENTE IL RECEIVER");
            unregisterReceiver(receiver);
        }catch(Exception e){
            //Toast.makeText(getApplicationContext(), "Register NON deregistrato",Toast.LENGTH_SHORT).show();
            Log.d("RECEIVER", "NON HO POTUTO DEREGISTRARE IL RECEIVER");
        }

        //smetto di fare la discovery di nuovi dispositivi, se la stavo facendo
        if(PublicConstants.bluetoothAdapter.isDiscovering()){
            PublicConstants.bluetoothAdapter.cancelDiscovery();
        }

        lastContactedDevice = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //quando l'utente seleziona il nome del dispositivo con cui vuole comunicare, vado a cercare in pairedDevices il BluetoothDevice relativo, e lancio il ClientThread
        //che POTREBBE riuscire a collegarsi con quel dispositivo. Non è detto infatti che la connessione avvenga: il dispositivo con cui vuole comunicare magari non
        //è disponibile, oppure l'altro utente ha ricevuto la richiesta di gioco ma l'ha rifiutata
        String deviceName=lv.getAdapter().getItem(position).toString();
        Log.d("CLIENT RESEARCH ACTI", "click sul device = " + deviceName);
        Iterator<BluetoothDevice> it = pairedDevices.iterator();
        while(it.hasNext()){
            BluetoothDevice currentDevice = it.next();

            //se ho cliccato su un dispositivo, voglio ammazzare il thread precedente e lanciarne uno nuovo
            if(currentDevice.getName().equals(deviceName)){

                //interrompo il thread già esistente, se ne esiste già uno
                MyConnectionThread.destroyMyConnectionThread();
                Log.d("CLIENT RESEARCH ACTI", "Pulito il thread");

                //e ne lancio un altro
                Log.d("CLIENT RESEARCH ACTI", "currentDevice = " + currentDevice + ", whose name is " + currentDevice.getName());
                Log.d("CLIENT RESEARC ACTI", "lancio un nuovo client thread nella onItemClick (IF), currentDevice = " + currentDevice);
                MyConnectionThread.createMyConnectionThread(MyConnectionThread.CLIENT_STRING, this, currentDevice);
                Log.d("CLIENT RESEARCH ACTI", "Lanciato il nuovo thread?");
                break;
            }
        }
    }


    @Override
    public void onBackPressed(){
        super.onBackPressed();
        MyConnectionThread.destroyMyConnectionThread();
    }

}