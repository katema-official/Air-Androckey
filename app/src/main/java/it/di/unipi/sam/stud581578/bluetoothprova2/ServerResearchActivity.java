package it.di.unipi.sam.stud581578.bluetoothprova2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ServerResearchActivity extends AppCompatActivity {

    private ServiceConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setto il layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_research);

        TextView tv = findViewById(R.id.textView_other_device_name);
        Button b0 = findViewById(R.id.button_yes);
        Button b1 = findViewById(R.id.button_no);
        tv.setVisibility(View.INVISIBLE);
        b0.setVisibility(View.INVISIBLE);
        b1.setVisibility(View.INVISIBLE);

        TextView tv_name = findViewById(R.id.textview_name_server);
        tv_name.setText(PublicConstants.bluetoothAdapter.getName());

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

        Log.d("SERVER RESEARC ACTIVITY", "lancio un nuovo server thread nella onCreate");
        MyConnectionThread.createMyConnectionThread(MyConnectionThread.SERVER_STRING, this, null);

    }

    @Override
    protected void onResume(){
        super.onResume();
        //lancio il thread lato server



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

    @Override
    public void onBackPressed(){
        MyConnectionThread.destroyMyConnectionThread();
    }



}