package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import it.di.unipi.sam.stud581578.bluetoothprova2.MainActivity;
import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;

public class ClientConnectedActivity extends AppCompatActivity{

    BallSurfaceView bsf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //imposto il layout
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);



        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        PublicConstants.SCREEN_WIDTH = dm.widthPixels;
        PublicConstants.SCREEN_HEIGHT = dm.heightPixels;

        bsf = new BallSurfaceView(this);
        setContentView(bsf);

    }

    @Override
    protected void onRestart(){
        super.onRestart();
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        Log.d("MESSAGE NOT REPLAY", "Ehi baby, on destroy");
        super.onDestroy();
    }

    @Override
    public void onBackPressed(){
        GameThread gt = bsf.mThread.gt;

        //faccio terminare il thread GameThread se era bloccato
        synchronized (gt.lock_replay){
            gt.cond_replay = true;

            //faccio ripartire il thread confermando che si può rigiocare
            gt.rematch_message = PublicConstants.MESSAGE_NOT_REPLAY;
            gt.lock_replay.notify();

        }
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
    }


}