package it.di.unipi.sam.stud581578.bluetoothprova2.game;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

import it.di.unipi.sam.stud581578.bluetoothprova2.ConnectionBluetoothService;
import it.di.unipi.sam.stud581578.bluetoothprova2.MainActivity;
import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;
import it.di.unipi.sam.stud581578.bluetoothprova2.R;

public class GameThread extends AsyncTask<Void, String, Void> implements View.OnTouchListener{

    private Activity activity;
    private ServiceConnection connection;
    private ConnectionBluetoothService my_bluetooth_service;
    private boolean service_bound = false;
    private final Object lock_service = new Object();
    private CopyOnWriteArrayList<Renderable> renderables;
    private RenderThread renderThread;
    private boolean wait_new_game = false;
    private BallSurfaceView ballSurfaceView;

    private float data;
    private boolean end_other_message_received = false;
    public final Object lock_replay = new Object();
    public boolean cond_replay = false;
    public String rematch_message = null;

    MediaPlayer mp_sfx_1, mp_sfx_disc, mp_sfx_disc_player, mp_background_music, mp_buongiorno_professore;
    boolean second_background_music = false;


    public GameThread(Activity activity, CopyOnWriteArrayList<Renderable> renderables, RenderThread renderThread, BallSurfaceView ballSurfaceView){
        this.activity = activity;
        this.renderables = renderables;
        this.renderThread = renderThread;
        this.ballSurfaceView = ballSurfaceView;

        ballSurfaceView.setOnTouchListener(this);

        connection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                synchronized (lock_service) {
                    //ci siamo legati al Service, prendiamo un riferimento a lui e indichiamo che il service è bound, e quindi possiamo
                    //cominciare ad usarlo
                    ConnectionBluetoothService.LocalBinder binder = (ConnectionBluetoothService.LocalBinder) service;
                    my_bluetooth_service = binder.getService();
                    service_bound = true;
                    lock_service.notify();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                synchronized (lock_service) {
                    //se per un qualche motivo il service è stato disconnesso, lo segno su service_bound
                    service_bound = false;
                }
            }
        };

        Intent intent = new Intent(activity, ConnectionBluetoothService.class);
        activity.bindService(intent, connection, activity.BIND_AUTO_CREATE);


    }


    @Override
    protected Void doInBackground(Void... voids) {

        synchronized (lock_service) {
            //finché il service non è pronto
            while (!service_bound) {
                try {
                    lock_service.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.d("GT DEBUG", "1");

        //sato 0: inizializzo la UI e company
        initialize();

        //preparo i mediaplayer
        mp_sfx_1 = new MediaPlayer();

        mp_sfx_disc = new MediaPlayer();
        AssetFileDescriptor audio = activity.getResources().openRawResourceFd(R.raw.key_hit);
        try {
            mp_sfx_disc.setDataSource(audio.getFileDescriptor(), audio.getStartOffset(), audio.getLength());
            audio.close();
            mp_sfx_disc.prepare();
        }catch(IOException e){
            e.printStackTrace();
        }

        mp_sfx_disc_player = new MediaPlayer();
        AssetFileDescriptor audioo = activity.getResources().openRawResourceFd(R.raw.spears_i_think);
        try {
            mp_sfx_disc_player.setDataSource(audioo.getFileDescriptor(), audioo.getStartOffset(), audioo.getLength());
            audioo.close();
            mp_sfx_disc_player.prepare();
        }catch(IOException e){
            e.printStackTrace();
        }

        mp_background_music = new MediaPlayer();
        AssetFileDescriptor ost1 = activity.getResources().openRawResourceFd(R.raw.mudeth_invictus);
        try {
            mp_background_music.setDataSource(ost1.getFileDescriptor(), ost1.getStartOffset(), ost1.getLength());
            ost1.close();
            mp_background_music.prepare();
        }catch(IOException e){
            e.printStackTrace();
        }
        mp_background_music.setVolume(0.4f, 0.4f);
        mp_background_music.setLooping(true);

        mp_buongiorno_professore = new MediaPlayer();
        AssetFileDescriptor bp;
        if(my_bluetooth_service.getRole().equals(PublicConstants.SERVER_ROLE)){
            bp = activity.getResources().openRawResourceFd(R.raw.alessio_buongiorno);  //QUI CI VADO IO
        }else {
            bp = activity.getResources().openRawResourceFd(R.raw.giovanni_buongiorno);
        }
        try {
            mp_buongiorno_professore.setDataSource(bp.getFileDescriptor(), bp.getStartOffset(), bp.getLength());
            bp.close();
            mp_buongiorno_professore.prepare();
        }catch(IOException e){
            e.printStackTrace();
        }

        //stato 1: 3... 2... 1... via!
        //voglio che i due dispositivi siano sincronizzati, quindi ciascuno di loro deve avvertire l'altro che è pronto
        //se sono il client, dico al server che sono pronto, e poi lascio che il server mi risponda
        String res_1 = null;
        if(my_bluetooth_service.getRole().equals(PublicConstants.CLIENT_ROLE)) {
            my_bluetooth_service.myWrite(PublicConstants.MESSAGE_GAME_SYNCHRONIZE, "OK");
            res_1 = my_bluetooth_service.myRead();
            Log.d("GT", "2 C");
        }else{
            //se invece sono il server, aspetto che il client mi dica che è pronto, e poi gli dico che anche io sono pronto
            res_1 = my_bluetooth_service.myRead();
            my_bluetooth_service.myWrite(PublicConstants.MESSAGE_GAME_SYNCHRONIZE, "OK");
            Log.d("GT", "2 S");
        }

        if(res_1!=null){
            Log.d("GAME THREAD", "Sincronizzazione ok!");
        }else{
            //TODO
            Log.d("GAME THREAD", "Sincronizzazione NON ok!");
            return null;
        }


        readySteadyGo();

        PlayerDisc pd = (PlayerDisc) renderables.get(1);
        pd.active = true;

        //stato 2: si gioca

        //innanzitutto mostriamo il punteggio
        renderables.add(new ScoreText("" + my_bluetooth_service.getMyScore()));

        //questo thread in questa fase non deve fare tantissimo, deve giusto aspettare che l'altro giocatore gli comunichi la
        //posizione del disco
        data = 0;
        //faccio partire la colonna sonora
        if(my_bluetooth_service.getRole().equals(PublicConstants.SERVER_ROLE)) {
            mp_background_music.start();
        }

        while(data!=PublicConstants.GAME_THREAD_ERROR && data!=PublicConstants.GAME_THREAD_STOP){        //!= PublicConstants.MESSAGE_DISTANCE_INVALID && data != PublicConstants.MESSAGE_YOU_SCORED
            //aspetto il messaggio del mio avversario
            data = myReadGame();
            if(data == PublicConstants.MESSAGE_DISTANCE_INVALID){
                //
            }else if(data == PublicConstants.MESSAGE_YOU_SCORED){
                //stato 3-win: ho fatto punto!

                //prima di tutto, informo anche l'avversario del mio punto (ma perché farlo se tanto lui già lo sa, visto che me
                //lo ha comunicato? Per svegliare il suo GameThread in attesa di una read, così che agisca di conseguenza
                myWriteGame(PublicConstants.MESSAGE_I_SCORED);
                scoreOrNot(true, pd);
            }else if(data == PublicConstants.MESSAGE_I_SCORED) {
                //stato 3-lose: l'altro giocatore ha fatto punto, peccato!

                scoreOrNot(false, pd);
            }else{
                //l'avversario mi sta comunicando dove far spawnare il disco
                float distance = data;
                //ora devo farmi dire le velocità. Prima quella orizzontale, poi quella verticale.
                float xvel = myReadGame() * PublicConstants.SCREEN_WIDTH;
                float yvel = myReadGame() * PublicConstants.SCREEN_WIDTH;

                //ora posso creare il disco correttamente
                renderables.add(new Disc(renderThread, distance, xvel, yvel));

                Log.d("GT", "I've read where i have to put the disc");
            }
        }

        if(data == PublicConstants.GAME_THREAD_ERROR){
            //la partita termina a causa di un errore (l'altro giocatore ha interrotto la partita ad esempio)
            Log.d("MESSAGE THREAD ERROR", "entrato");
            publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_END});
        }else if(data == PublicConstants.GAME_THREAD_STOP){
            //se la partita è terminata correttamente, aspetto di sapere se l'altro giocatore vuole continuare a giocare oppure no
            rematch_message = my_bluetooth_service.myRead();

            //se si è verificato un errore (l'altro giocatore ha chiuso bruscamente)
            if(rematch_message == null){
                publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_END});
            }else if(rematch_message.equals(PublicConstants.MESSAGE_REPLAY_QM)){
                //se l'altro giocatore mi sta chiedendo di rigiocare...
                end_other_message_received = true;

                //aspetto di decidere se rigiocare oppure no
                synchronized (lock_replay){
                    while(!cond_replay){
                        try {
                            Log.d("EHI", "Non facciamo scherzi...");
                            lock_replay.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //se è andato tutto bene, rigiochiamo
                if(rematch_message.equals(PublicConstants.MESSAGE_REPLAY_OK)){
                    my_bluetooth_service.setToZeroAllScores();
                    publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_REPEAT});
                }else {
                    //se invece non ho intenzione di rigiocare, fine
                    publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_END});
                }

            }else if(rematch_message.equals((PublicConstants.MESSAGE_REPLAY_OK))){
                my_bluetooth_service.setToZeroAllScores();
                publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_REPEAT});
            }else if(rematch_message.equals(PublicConstants.MESSAGE_NOT_REPLAY)){
                //se l'altro giocatore ha deciso di non ripetere la partita (volontariamente o perché si è verificato un errore)
                Log.d("MESSAGE NOT REPLAY", "Ehi baby, addio");
                publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_END});
            }else{
                Log.d("MESSAGE NOT REPLAY", "Ehi baby, addio per errore");
                publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_END});
            }
        }else{
            Log.d("MESSAGE NOT REPLAY", "Ehi baby, what?");
            publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_END});

        }

        return null;
    }

    @Override
    protected void onProgressUpdate(String... strings){
        if(strings[0].equals(PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_REPEAT)) {
            Log.d("GAME THRED", "Ehi baby, activity recreate");
            activity.recreate();
        }else{
            Log.d("MESSAGE NOT REPLAY", "Ehi baby, activity finish");
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
        }
        renderThread.mQuit = true;

    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        activity.unbindService(connection);
        service_bound = false;
        mp_sfx_1.release();
        mp_sfx_disc.release();
        mp_sfx_disc_player.release();
        mp_background_music.release();
        Log.d("MESSAGE NOT REPLAY", "Ehi baby, on post execute");
        if(renderThread != null) {
            renderThread.mQuit = true;
        }
    }




    //metodi usati per leggere e scrivere la distanza del disco dalla parete sinistra
    public void myWriteGame(float distance){
        my_bluetooth_service.myWriteDistance(distance);
    }

    public float myReadGame(){
        float ret = my_bluetooth_service.myReadDistance();
        return ret;
    }










    //funzioni mie per la gestione dell'automa del gioco

    public void initialize(){
        //qui imposto la UI

        if(my_bluetooth_service.getRole().equals(PublicConstants.SERVER_ROLE)) {
            //l'impostazione della UI cambia a seconda del fatto che io sia il client o il server
            //se sono il server, ho il disco blue e servo io
            renderables.add(new PlayerDisc(renderThread, Color.BLUE));
            renderables.add(new Disc(renderThread, -1, 0, 0));
        }else {
            //se invece sono il client, ho il disco rosso, e NON servo
            renderables.add(new PlayerDisc(renderThread, Color.RED));
        }

    }



    private void readySteadyGo(){
        //counter 3... 2... 1... via! per l'inizio della partita
        boolean play = (my_bluetooth_service.getRole().equals(PublicConstants.SERVER_ROLE) ? true : false);
        renderables.add(new Text("3"));
        int pos = renderables.size() - 1;
        if(play) playMp_sfx_1(R.raw.three);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        renderables.set(pos, new Text("2"));
        if(play) playMp_sfx_1(R.raw.two);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        renderables.set(pos, new Text("1"));
        if(play) playMp_sfx_1(R.raw.one);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        renderables.set(pos, new Text(activity.getResources().getString(R.string.text_go)));
        if(play) playMp_sfx_1(R.raw.go);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        renderables.remove(pos);
    }

    private void scoreOrNot(boolean scored, PlayerDisc pd){

        //interrompo la musica che stava venendo riprodotta (la metto in pausa)
        if(mp_background_music.isPlaying()) {
            mp_background_music.pause();
        }

        //disabilito per il momento i comandi touch
        pd.active = false;

        //se nessuno dei due giocatori ha ancora vinto, mi comporto in un modo. Se invece uno dei due ha vinto, mi comporto in un altro.
        int my_current_score = my_bluetooth_service.getMyScore();
        int other_current_score = my_bluetooth_service.getOtherScore();
        if(scored){
            my_current_score++;
        }else{
            other_current_score++;
        }

        if(my_current_score >= 5 && (my_current_score - other_current_score >= 2)){     //5 2
            //se ho vinto io
            endgame(true);
        }else if(other_current_score >= 5 && (other_current_score - my_current_score >= 2)){
            //se ha vinto l'avversario
            endgame(false);
        }else {

            if (scored) {
                //se ho fatto punto

                ///riproduco un suono
                mp_buongiorno_professore.start();

                //aggiorno il mio punteggio
                my_bluetooth_service.addMyScore();
                renderables.set(renderables.size() - 1, new ScoreText("" + my_bluetooth_service.getMyScore()));

                //e ora celebro per un po' il mio successo
                renderables.add(new Text(activity.getResources().getString(R.string.text_goal)));
            } else {
                //se l'altro ha fatto punto

                //aggiorno i punti dell'avversario
                my_bluetooth_service.addOtherScore();

                //rimango in lutto per un po'
                renderables.add(new Text(activity.getResources().getString(R.string.text_not_goal)));
            }

            //aspetto un po'
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //tolgo la scritta dallo schermo
            renderables.remove(renderables.size() - 1);

            //se sono il perdente, servo io il nuovo disco
            if (!scored) {
                //creo un nuovo disco
                renderables.add(new Disc(renderThread, -1, 0, 0));
            }

            //poi posiziono correttamente il playerDisc
            pd.setXY(PublicConstants.SCREEN_WIDTH / 2, 3 * PublicConstants.SCREEN_HEIGHT / 4);

            //rendo il playerDisc nuovamente toccabile (il disco spawnerà dalla parte del perdente)
            pd.active = true;

            //gestisco la colonna sonora
            manageBackgroundMusic();

        }

    }



    private void endgame(boolean i_won){
        if(i_won) {
            mp_buongiorno_professore.start();

            my_bluetooth_service.addMyScore();
            renderables.set(renderables.size() - 1, new ScoreText("" + my_bluetooth_service.getMyScore()));


            renderables.add(new Text(activity.getResources().getString(R.string.text_won)));



        }else{
            my_bluetooth_service.addOtherScore();

            renderables.add(new Text(activity.getResources().getString(R.string.text_lose)));



        }
        renderables.add(new Text2("Rematch?"));
        renderables.add(new ButtonText(activity.getResources().getString(R.string.text_yes), true));
        renderables.add(new ButtonText(activity.getResources().getString(R.string.text_no), false));

        wait_new_game = true;

        data = PublicConstants.GAME_THREAD_STOP;
    }










    //metodi per il mediaplayer
    public void playMp_sfx_1(int audio_id){
        try{

            mp_sfx_1.reset();
            AssetFileDescriptor audio = activity.getResources().openRawResourceFd(audio_id);
            try {
                mp_sfx_1.setDataSource(audio.getFileDescriptor(), audio.getStartOffset(), audio.getLength());
                audio.close();
                mp_sfx_1.prepare();
            }catch(IOException e){
                e.printStackTrace();
            }
            mp_sfx_1.start();
        }catch (Exception e){
            //
        }
    }

    public void playMp_sfx_disc_1(){
        try {
            if (mp_sfx_disc.isPlaying()) {
                mp_sfx_disc.stop();
                try {
                    mp_sfx_disc.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mp_sfx_disc.start();
        }catch (Exception e){
            //
        }
    }

    public void playMp_sfx_disc_2(){
        try {
        if(mp_sfx_disc_player.isPlaying()){
            mp_sfx_disc_player.stop();
            try {
                mp_sfx_disc_player.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mp_sfx_disc_player.start();
        }catch (Exception e){
            //
        }
    }


    public void manageBackgroundMusic() {
        if (my_bluetooth_service.getRole().equals(PublicConstants.SERVER_ROLE)) {
            //se uno dei due giocatori ha appena superato i quattro punti, cambio la colonna sonora riprodotta
            if ((my_bluetooth_service.getMyScore() == 4 || my_bluetooth_service.getOtherScore() == 4) && second_background_music == false) {
                second_background_music = true;
                mp_background_music.reset();
                AssetFileDescriptor audio = activity.getResources().openRawResourceFd(R.raw.mudeth_rapturepunk);
                try {
                    mp_background_music.setDataSource(audio.getFileDescriptor(), audio.getStartOffset(), audio.getLength());
                    audio.close();
                    mp_background_music.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mp_background_music.start();
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(wait_new_game) {

            Log.d("ON TOUCH", "Entered");

            ButtonText yes = (ButtonText) renderables.get(5);
            ButtonText no = (ButtonText) renderables.get(6);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if(yes.buttonRect.contains(event.getX(), event.getY())){
                        Log.d("ON TOUCH", "yes");

                        if(end_other_message_received) {
                            //se l'altro giocatore mi ha già comunicato che ha intenzione di giocare ancora, gli confermo che mi sta bene
                            my_bluetooth_service.myWrite(1000, PublicConstants.MESSAGE_REPLAY_OK);
                            //e faccio ripartire il thread
                            synchronized (lock_replay){
                                cond_replay = true;

                                //faccio ripartire il thread confermando che si può rigiocare
                                rematch_message = PublicConstants.MESSAGE_REPLAY_OK;
                                lock_replay.notify();
                            }
                        }else{
                            //se invece sono io il primo a confermare di voler rigiocare
                            my_bluetooth_service.myWrite(1000, PublicConstants.MESSAGE_REPLAY_QM);
                        }

                        wait_new_game = false;
                    }else if(no.buttonRect.contains(event.getX(), event.getY())){
                        Log.d("ON TOUCH", "no");
                        //comunico all'altro giocatore che non voglio giocare e esco
                        my_bluetooth_service.myWrite(1000, PublicConstants.MESSAGE_NOT_REPLAY);

                        //sveglio questo thread se stava dormendo (perché l'altro invece mi ha detto che vorrebbe rigiocare
                        synchronized (lock_replay){
                            cond_replay = true;

                            //faccio ripartire il thread confermando che si può rigiocare
                            rematch_message = PublicConstants.MESSAGE_NOT_REPLAY;
                            lock_replay.notify();
                        }

                        publishProgress(new String[]{PublicConstants.GAME_THREAD_PUBLISH_PROGRESS_END});
                        cancel(true);

                        wait_new_game = false;
                    }
                    break;
            }
            return true;
        }
        return false;
    }







}
