package it.di.unipi.sam.stud581578.bluetoothprova2;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ConnectionBluetoothService extends Service {

    // Binder fornito ai client
    private final IBinder binder = new LocalBinder();

    //variabili per la comunicazione bluetooth
    private static BluetoothServerSocket bluetoothServerSocket;
    private static BluetoothSocket bluetoothSocket;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static DataInputStream dataInputStream;
    private static DataOutputStream dataOutputStream;
    private static BluetoothDevice otherDevice;

    //per sapere se sono il client o il server
    private static String role;

    //per tenere i punteggi di entrambi i giocatori
    private static int myScore = 0;
    private static int otherScore = 0;


    //classe usata dai client per prendere un riferimento a questo servizio
    public class LocalBinder extends Binder {
        public ConnectionBluetoothService getService() {

            //restituisce questa istanza di ConnectionBluetoothService per permettere ai client
            //che si bindano di chiamare metodi pubblici di questo Service
            Log.d("BINDER", "Restituito il ConnectionBluetoothService");
            return ConnectionBluetoothService.this;
        }
    }

    @Override
    public void onCreate(){
        Log.d("ON CREATE SERVICE", "ciao");

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("ON BIND", "ciao, restituisco il binder");
        return binder;
    }


    //--------------------METODI PUBLIC USATI DAI THREAD--------------------

    //metodo usato per impostare il dispositivo bluetooth con cui si sta cercando di comunicare
    public void setOtherDevice(BluetoothDevice otherDevice){
        ConnectionBluetoothService.otherDevice = otherDevice;
    }

    //metodo usato per aprire il socket quando ci si comporta come client
    public String createClientSocket(){
        try {
            bluetoothSocket = otherDevice.createRfcommSocketToServiceRecord(UUID.fromString(PublicConstants.UUID));
        } catch (Exception e) {
            return null;
        }
        return "A";

    }

    //metodo usato per connettersi all'altro dispositivo
    public String connectToOtherDevice() {
        try {
            bluetoothSocket.connect();
        } catch (Exception connectException) {
            return null;
        }
        return "A";
    }

    //metodo usato per fare l'handshake con il server
    public String handShakeClient(){
        String ret = null;

        //se sono riuscito a connettermi (e sono il client), posso cominciare il mio handshaking
        if(bluetoothSocket != null && bluetoothSocket.isConnected()) {
            //se mi sono connesso, chiedo all'altro di giocare
            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                dataInputStream = new DataInputStream(inputStream);
                dataOutputStream = new DataOutputStream(outputStream);

                //gli dico chi sono
                myWrite(PublicConstants.THREE_WAY_HANDSHAKE_1, PublicConstants.bluetoothAdapter.getName());

                //aspetto la risposta
                String response = myRead();

                if (response != null && response.equals(PublicConstants.THREE_WAY_HANDSHAKE_2_YES)) {
                    //se ha accettato gli comunico che possiamo iniziare a parlare
                    myWrite(PublicConstants.THREE_WAY_HANDSHAKE_3_OK, "LET'S TALK");
                    ret = "OK";
                } else {
                    return "NOT OK";
                }
            } catch (Exception e) {
                return null;
            }
        }
        return ret;
    }



    //metodo usato dal server per aprire il serverSocket
    public String createServerServerSocket(){
        try {
            bluetoothServerSocket = PublicConstants.bluetoothAdapter.listenUsingRfcommWithServiceRecord(PublicConstants.NAME, UUID.fromString(PublicConstants.UUID));
        } catch (Exception e) {
            Log.d("CREATE SSSOCKET", "error");
            return null;
        }
        return "A";
    }

    //metodo usato per accettare la connessione del client
    public String acceptOtherDevice(){
        String ret = null;
        //prima di tutto faccio la connect
        try {
            bluetoothSocket = bluetoothServerSocket.accept();
        } catch (Exception connectException) {
            return null;
        }
        return "A";
    }

    //metodo usato per fare l'handshake con client (parte 1)
    public String handShakeServer_part1(){
        //se mi sono connesso (e sono il server), chiedo all'altro chi è
        if(bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                dataInputStream = new DataInputStream(inputStream);
                dataOutputStream = new DataOutputStream(outputStream);

                //voglio sapere con chi sto parlando
                String name_other_device = myRead();
                return name_other_device;
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    //metodo usato per fare l'handshake con client (parte 2)
    public String handShakeServer_part2() {
        String confirm = myRead();
        if(confirm != null){
            if(confirm.equals(PublicConstants.S_MESSAGE_CONNECTION_CLOSED)) {
                return null;
            }
        }else{
            return null;
        }
        return "A";
    }



    //metodi di lettura e scrittura per la fase di instaurazione della connessione
    public void myWrite(int msg_id, String msg){
        try {
            dataOutputStream.writeInt(msg_id);
            dataOutputStream.writeUTF(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String myRead(){
        int id_received;
        String msg_received;
        try {
            id_received = dataInputStream.readInt();
            msg_received = dataInputStream.readUTF();
        } catch (Exception e) {
            e.printStackTrace();
            id_received = -1;
            msg_received = null;
        }

        return msg_received;
    }

    //metodi di lettura e scrittura in fase di gioco
    public void myWriteDistance(float distance){
        try{
            dataOutputStream.writeFloat(distance);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public float myReadDistance(){
        float ret = PublicConstants.GAME_THREAD_ERROR;
        try{
            ret = dataInputStream.readFloat();
        }catch (Exception e){
            ret = PublicConstants.GAME_THREAD_ERROR;
        }
        return ret;
    }



    //metodo per liberare le risorse relative alla comunicaizone bluetooth. Così facendo sono sicuro di non avere
    //connessioni strane pendenti che potrebbero impedirmi di stabilire future connessioni
    public void freeConnectionResources(){
        if (dataInputStream != null) {
            try {
                dataInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (dataOutputStream != null) {
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bluetoothServerSocket != null) {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dataInputStream = null;
        dataOutputStream = null;
        inputStream = null;
        outputStream = null;
        bluetoothServerSocket = null;
        bluetoothSocket = null;

        Log.d("SERVICE", "Free resources");
        myScore = 0;
        otherScore = 0;

        otherDevice = null;

    }





    public void setRole(String role){
        this.role = role;
    }

    public String getRole(){
        return role;
    }

    public void addMyScore(){
        myScore++;
    }

    public void addOtherScore(){
        otherScore++;
    }

    public int getMyScore(){
        return myScore;
    }

    public int getOtherScore(){
        return otherScore;
    }

    public void setToZeroAllScores(){
        myScore = 0;
        otherScore = 0;
    }


}
