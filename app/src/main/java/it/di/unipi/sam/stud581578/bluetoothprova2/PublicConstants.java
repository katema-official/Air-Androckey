package it.di.unipi.sam.stud581578.bluetoothprova2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PublicConstants {

    public static BluetoothAdapter bluetoothAdapter = null;
    public static int SERVER_REQUEST_CODE = 10;
    public static int CLIENT_REQUEST_CODE = 20;
    public static String UUID = "2797afc9-5c53-4086-8c6a-cf11eb44066a";
    public static int DISCOVERABILITY_DURATION = 15;
    public static String NAME = "Air Hockey Bluetooth";

    public static final int THREE_WAY_HANDSHAKE_1 = 0;    //usato per chiedere al server di giocare
    public static final String THREE_WAY_HANDSHAKE_2_YES = "YES";   //usato per dire al client che è stata accettata la sua richiesta di giocare
    public static final String THREE_WAY_HANDSHAKE_2_NO = "NO";     //usato per dire al client che è stata rifiutata la sua richiesta di giocare
    public static final String S_MESSAGE_CONNECTION_CLOSED = "";    //usato per dire all'altro dispositivo che la connessione è stata chiusa
    public static final int MESSAGE_RESPONSE_CONNECT = 1;   //usato per dire al client se si è accettato oppure no
    public static final int THREE_WAY_HANDSHAKE_3_OK = 4; //usato per dire al server che si può iniziare a dialogare

    public static final int MESSAGE_GAME_SYNCHRONIZE = 5;   //usato dal client e dal server per sincronizzarsi durante la partita



    public static final float MESSAGE_DISTANCE_INVALID = -1; //usato dal client e dal server per comunicare all'altro che c'è stato un errore
                                                            //nella comunicazione della posizione del disco
    public static final float MESSAGE_YOU_SCORED = -2;      //usato dal client e dal server per comunicare all'altro che ha segnato
    public static final float MESSAGE_I_SCORED = -3;       //usato per dire all'altro giocatore che ho fatto punto

    public static final String MESSAGE_REPLAY_QM = "REPLAY?";   //usato per esprimere all'altro giocatore, dopo la fine di una partita, la volontà
                                                                //di giocare ancora
    public static final String MESSAGE_REPLAY_OK = "OK!";       //usato per accettare la richiesta di giocare ancora
    public static final String MESSAGE_NOT_REPLAY = "BASTA";    //usato per dire all'altro giocatore che non ho intenzione di giocare ancora

    public static final int GAME_THREAD_STOP = -100;            //usato per terminare il thread del gioco e far partire subito una nuova partita
    public static final int GAME_THREAD_ERROR = -200;          //usato per terminare il thread del gioco e terminare completamente la partita in caso di errore



    public static final String GAME_THREAD_PUBLISH_PROGRESS_REPEAT = "REPEAT";      //per ripetere il gioco nella onPublishProgress
    public static final String GAME_THREAD_PUBLISH_PROGRESS_END = "END";            //per terminare il gioco nella onPublishProgress





    public static int SCREEN_WIDTH;
    public static int SCREEN_HEIGHT;



    public static String SERVER_ROLE = "SERVER";
    public static String CLIENT_ROLE = "CLIENT";
}
