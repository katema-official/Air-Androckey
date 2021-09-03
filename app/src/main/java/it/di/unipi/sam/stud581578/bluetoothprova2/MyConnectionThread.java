package it.di.unipi.sam.stud581578.bluetoothprova2;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import it.di.unipi.sam.stud581578.bluetoothprova2.game.ClientConnectedActivity;
import it.di.unipi.sam.stud581578.bluetoothprova2.game.ServerConnectedActivity;

public class MyConnectionThread extends AsyncTask<Void, String, Void> {

    //--------------------VARIABILI PER IL THREAD--------------------

    //variabili usate per indicare se questo thread deve fungere da client o da server
    public static final String CLIENT_STRING = "CLIENT";
    public static final String SERVER_STRING = "SERVER";

    //dato che voglio un solo thread alla volta per la gestione della comunicazione, applico il pattern singleton
    private static MyConnectionThread instance = null;

    private static String mode;    //definisce se il thread funge da client o da server
    private static Activity activity; //il context

    private static int counter_DEBUG = 0;
    private static int THREADS_IN_EXECUTION = 0;

    //variabile che mi serve quando sono il server e devo rendere visibili i pulsanti per accettare di comunicare
    private static boolean publishForButtons = false;

    //per essere sicuro che al più un thread sia in esecuzione, mi dichiaro una lock e una variabile di condizione
    private static final Object lock_thread = new Object();
    private static boolean thread_active = false;

    //--------------------VARIABILI PER IL SERVICE--------------------

    //booleano per sapere se mi sono legato al service (e quindi posso cominciare a usare i suoi metodi)
    public static boolean service_bound = false;
    //lock che mi garantisce di modificare il valore di service_bound in mutua esclusione
    public static final Object lock_service = new Object();

    //riferimento al service
    public static ConnectionBluetoothService my_bluetooth_service;

    private static BluetoothDevice otherDevice;




    private MyConnectionThread(){
    }

    //metodo per creare una nuova istanza del thread
    public static synchronized void createMyConnectionThread(String mode, Activity activity, BluetoothDevice otherDevice) {

        //cosa voglio fare quando lancio un nuovo thread? Assicurarmi che non ce ne siano altri in esecuzione
        //per farlo posso usare un trucchetto legato alle eccezioni: chiudo socket e stream che erano aperti in precedenza,
        //così il service che se ne sta occupando lancia un'eccezione e termina. A quel punto il service dirà al thread in esecuzione
        //che qualcosa è andato storto e lo farà terminare

        synchronized (lock_thread) {
            //fintanto che c'è un altro thread in esecuzione
            while(thread_active){
                //io faccio il dispettoso e chiudo, sul service corrente, tutte le connessioni
                instance.die();
                try {
                    //e aspetto che il thread morente mi confermi di essere morto
                    lock_thread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int tmp = counter_DEBUG;
            counter_DEBUG++;
            Log.d("MCT CLIENT/SERVER", "counter_DEBUG = " + tmp + ", but now = " + counter_DEBUG);

            THREADS_IN_EXECUTION++;
            Log.d("THREADS IN EXEC", "New thread. Threads in execution = " + THREADS_IN_EXECUTION);

            //ora che il thread precedente è terminato, posso lanciarne uno nuovo, E MI SEGNO CHE IO SONO ATTIVO
            MyConnectionThread.thread_active = true;
            MyConnectionThread.activity = activity;
            MyConnectionThread.mode = mode;

            MyConnectionThread.otherDevice = otherDevice;

            instance = new MyConnectionThread();
            instance.execute();
            Log.d("MCT CLIENT/SERVER", "thread created");


        }
    }



    //metodo per distruggere il thread corrente in esecuzione (è pensato per essere chiamato solo dall'esterno: mai chiamarlo all'interno di questa classe. Chiamare piuttosto die() direttamente)
    //ma anche per liberare le risorse usate per la connessione (socket, stream...)
    public static synchronized void destroyMyConnectionThread(){
        //se c'è un thread, lo uccido e libero le risorse del service, facendo l'unbind con esso
        if(instance!=null){
            instance.die();
        }
    }



    @Override
    protected Void doInBackground(Void... voids) {
        //----------------------------------------------------------------------------------
        //aspetto che il service sia pronto
        synchronized (lock_service) {
            while (!service_bound) {
                try {
                    Log.d("LOCK SERVICE", "wait");
                    lock_service.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d("LOCK SERVICE", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            }
        }

        //ora che il service è pronto, posso cominciare ad usarlo. Inizio dicendogli qual è il dispositivo
        //con cui voglio comunicare (se sono in mode client). Poi lancio il thread

        my_bluetooth_service.setOtherDevice(otherDevice);

        //----------------------------------------------------------------------------------

        String ret = null;

        //il thread è in esecuzione, ma avrà comportamenti ben diversi a seconda del fatto che funga da client o da server
        if(MyConnectionThread.mode.equals(MyConnectionThread.CLIENT_STRING)){
            ret = actAsClient();
            if(ret==null){
                die();
                return null;
            }
        }

        if(MyConnectionThread.mode.equals(MyConnectionThread.SERVER_STRING)){
            ret = actAsServer();
            if(ret==null){
                die();
                return null;
            }
        }

        Log.d("MCT", "Muoio di morte naturale! CHE BELLO!");

        return null;
    }



    private String actAsClient(){

        String ret = null;

        //uso il BluetoothDevice per ottenere il BluetoothSocket
        ret = my_bluetooth_service.createClientSocket();
        if(ret!=null) {
            Log.d("MCT CLIENT", "create... riuscita");
        }else {
            Log.d("MCT CLIENT", "create... non riuscita");
            Log.d("MCT CLIENT", "muoio 1");
            publishProgress(new String[]{activity.getResources().getString(R.string.text_error_bluetooth)});
            return null;
        }

        //ora devo connettermi all'altro dispositivo
        ret = my_bluetooth_service.connectToOtherDevice();
        if(ret!=null){
            Log.d("MCT CLIENT", "connect riuscita");
            publishProgress(new String[]{activity.getResources().getString(R.string.text_wait_bluetooth)});
        }else{
            Log.d("MCT CLIENT", "è fallita la connection");
            Log.d("MCT CLIENT", "muoio 2");
            publishProgress(new String[]{activity.getResources().getString(R.string.text_error_bluetooth)});
            return null;
        }

        //e adesso effettuo il 3-way handshake
        ret = my_bluetooth_service.handShakeClient();
        if(ret!=null && ret.equals("OK")){
            //se ha accettato lancio la nuova activity, e mi segno che sono il client nel service
            my_bluetooth_service.setRole(PublicConstants.CLIENT_ROLE);
            Intent intent = new Intent(activity, ClientConnectedActivity.class);
            activity.startActivity(intent);
            Log.d("MCT CLIENT", "ho lanciato l'intent");
        }else if(ret!= null && ret.equals("NOT OK")){
            //se non ha accettato
            publishProgress(new String[]{activity.getResources().getString(R.string.text_deny_bluetooth)});
            Log.d("MCT CLIENT", "dopo aver detto chi sono ho ricevuto la risposta negativa, quindi uffa, muoio!");
            Log.d("MCT CLIENT", "muoio 3.1");
            return null;
        }else{
            Log.d("MCT CLIENT", "muoio 3.2");
            Log.d("MCT CLIENT", "errore nella lettura/scrittura sul socket");
            return null;
        }

        return "A";

    }







    private String actAsServer(){

        String ret = null;

        //prendo il bluetoothServerSocket
        ret = my_bluetooth_service.createServerServerSocket();
        if(ret!=null){
            Log.d("MCT SERVER", "create... riuscita");
        }else{
            Log.d("MCT SERVER", "create... non riuscita");
            Log.d("MCT SERVER", "muoio 1");
            publishProgress(new String[]{activity.getResources().getString(R.string.text_error_bluetooth)});
            activity.finish();
            return null;
        }

        //poi faccio l'accept
        ret = my_bluetooth_service.acceptOtherDevice();
        if(ret!=null){
            Log.d("MCT SERVER", "accept riuscita");
        }else {
            Log.d("MCT SERVER", "è fallita l'accept");
            Log.d("MCT SERVER", "muoio 2");
            activity.finish();
            return null;
        }

        //e poi c'è il 3-way handshake. Lo divido in due parti perché ho bisogno della prima lettura per poter aggiornare la UI
        ret = my_bluetooth_service.handShakeServer_part1();
        if(ret!=null){
            //mostro all'utente con chi sto parlando e gli chiedo di decidere cosa fare
            publishForButtons = true;
            publishProgress(new String[]{ret}); //ANDARE A LEGGERE LA ONPROGRESSUPDATE
        }else{
            Log.d("MCT SERVER", "errore nella lettura/scrittura sul socket");
            Log.d("MCT SERVER", "muoio 3");
            activity.finish();
            return null;
        }

        //qua dovrebbe esserci una write verso il client, ma in realtà viene effettuata nella onProgressUpdate

        //aspetto la conferma da parte dell'altro
        Log.d("MCT SERVER", "Read di conferma iniziata");

        ret = my_bluetooth_service.handShakeServer_part2();
        if(ret!=null) {
            Log.d("MCT SERVER", "si può iniziare a dialogare");
        }else{
            Log.d("MCT SERVER", "muoio 3");
            activity.finish();
            return null;
        }

        return "A";

    }








    private void die(){
        //con questo metodo, il thread MUORE, ovvero:
        //-sta per terminare
        //-quindi rilascia tutte le risorse usate (nel service)
        //-notifica un altro eventuale thread in attesa che può essere eseguito
        synchronized (lock_thread) {

            //libero le risorse eventualmente usate dal service
            my_bluetooth_service.freeConnectionResources();

            THREADS_IN_EXECUTION--;
            Log.d("THREADS IN EXEC", "Dead thread. Threads in execution = " + THREADS_IN_EXECUTION);

            //setto che non c'è più un thread attivo e che l'istanza è null
            thread_active = false;
            instance = null;

            //rimetto publishForButtons a false
            publishForButtons = false;

            //notifico un thread eventualmente in attesa che può essere eseguito
            lock_thread.notify();
            Log.d("MCT CLIENT/SERVER", "die()");
            Log.d("MCT CLIENT/SERVER", "counter_DEBUG = " + counter_DEBUG);

        }
    }







    @Override
    protected void onProgressUpdate(String... strings){

        if(publishForButtons) {
            //rimetto visibile la textview, ora che conosco il nome del device che vuole parlare con me
            String name = strings[0];
            TextView text_view = this.activity.findViewById(R.id.textView_other_device_name);
            text_view.setText(name + activity.getResources().getString(R.string.text_connection_incoming));
            text_view.setVisibility(View.VISIBLE);

            Button b0 = this.activity.findViewById(R.id.button_yes);
            Button b1 = this.activity.findViewById(R.id.button_no);

            Log.d("MCT SERVER", "testo a visible");

            View.OnClickListener ocl = new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (view.getId() == R.id.button_yes) {

                        //l'utente ha accettato la richiesta di connessione. Comunichiamolo all'altro device
                        my_bluetooth_service.myWrite(PublicConstants.MESSAGE_RESPONSE_CONNECT, PublicConstants.THREE_WAY_HANDSHAKE_2_YES);
                        my_bluetooth_service.setRole(PublicConstants.SERVER_ROLE);
                        Intent intent = new Intent(activity, ServerConnectedActivity.class);
                        activity.startActivity(intent);
                    }

                    if (view.getId() == R.id.button_no) {

                        //l'utente NON ha accettato la richiesta di connessione. Comunichiamolo all'altro device
                        my_bluetooth_service.myWrite(PublicConstants.MESSAGE_RESPONSE_CONNECT, PublicConstants.THREE_WAY_HANDSHAKE_2_NO);
                        activity.finish();
                    }
                }
            };

            //setto il listener su pulsanti
            b0.setOnClickListener(ocl);
            b1.setOnClickListener(ocl);

            //rendo visibili i pulsanti
            b0.setVisibility(View.VISIBLE);
            b1.setVisibility(View.VISIBLE);
            Log.d("ACCEPT THREAD TASK", "bottoni a visible");

            publishForButtons = false;
        }else{
            Toast.makeText(activity, strings[0], Toast.LENGTH_SHORT).show();
        }
    }

}
