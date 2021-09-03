package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.io.IOException;

import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;
import it.di.unipi.sam.stud581578.bluetoothprova2.R;

public class Disc extends Renderable
{
    private final RenderThread renderThread;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mXVelDir, mYVelDir; // pixels-per-second
    private float mSize;

    private PlayerDisc player;

    private int inCollision;    //modello la collisione come una macchina a stati finiti
    //0 = i due dischi non si sono ancora toccati, e inverto e dimezzo le velocità di quello piccino
    //1 = i dischi si sono toccati e ora applico la velocità di quello grande a quello piccino
    //2 = i dischi sono in contatto, ma non applico ulteriori velocità
    private String discPositionCollision = null; //per sapere dove il disco piccolo ha impattato su quello grande
    private int[] mCollisionDebug = {0, 0, 0, 0};

    //variabile usata per non comunicare più volte all'altro dispositivo la posizione del disco
    public boolean isDiscOnMyScreen;
    //quando è true, indica che è possibile comunicare all'altro dispositivo la sua posizione e velocità. Quando è false, no.
    public boolean arrived;

    public Disc(RenderThread renderThread, float dist_from_left, float xvel, float yvel) {
        this.renderThread = renderThread;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.BLACK);
        mXVelDir = -xvel;   //meno ("-") perché, se quell'altro mi ha detto che il disco aveva 10 di velocità, dal mio punto di vista aveva -10 di velocità
        mYVelDir = -yvel;

        isDiscOnMyScreen = true;

        mSize = PublicConstants.SCREEN_WIDTH / 6.0f;

        if(dist_from_left == -1) {
            mRect.left = PublicConstants.SCREEN_WIDTH / 2 - mSize / 2;
            mRect.top = PublicConstants.SCREEN_HEIGHT / 4 - mSize / 2;
            mRect.right = mRect.left + mSize;
            mRect.bottom = mRect.top + mSize;
            arrived = true;
        }else{
            //il disco deve comparire dall'alto con una certa velocità e da una certa posizione
            mRect.left = PublicConstants.SCREEN_WIDTH - (dist_from_left*PublicConstants.SCREEN_WIDTH + mSize/2);
            mRect.bottom = 0;
            mRect.top = mRect.bottom - mSize;
            mRect.right = mRect.right + mSize;
            arrived = false;
        }

        player = (PlayerDisc) renderThread.getRenderable(1);

        inCollision = 0;

    }

    @Override
    public void playfield(int width, int height) {
        mSize = width / 6.0f;
    }

    @Override
    public void update(RectF dirty, double timeDelta)
    {
        if(collided() && inCollision==0){
            //i dischi non si erano ancora mai toccati
            inCollision = 1;

            //se la velocità del disco piccolo è inversa rispetto a quella del disco grande lungo uno degli
            //assi, cambio di segno quella velocità del disco piccolo e la diminuisco, simulando l'attrito
            if(player.myXVelocity * mXVelDir <= 0){
                mXVelDir = -mXVelDir/3;
            }

            if(player.myYVelocity * mYVelDir <= 0){
                mYVelDir = -mYVelDir/3;
            }

            renderThread.gt.playMp_sfx_disc_2();

        }else if(collided() && inCollision == 1) {
            //i dischi si sono toccati e ora devo applciare la velocità di quello grande a quello piccolo
            mXVelDir += player.myXVelocity * 30;
            mYVelDir += player.myYVelocity * 30;

            inCollision = 2;
        }else if(collided() && inCollision == 2){
            //le velocità sono a posto, ma i due dischi sono ancora in contatto. Devo separarli

            //calcolo (in modo grossolano) di quanto spostare il disco basandomi sulla distanza
            //orizzontale e verticale che c'è tra i due dischi
            float x_dist = Math.abs(mRect.centerX() - player.mRect.centerX());
            float y_dist = Math.abs(mRect.centerY() - player.mRect.centerY());

            //5 è un valore arbitrario
            x_dist = (x_dist > 3 ? x_dist/10 : x_dist);
            y_dist = (y_dist > 3 ? y_dist/10 : y_dist);

            //il disco piccolo sta sopra quello grande
            if(mRect.bottom >= player.mRect.top && mRect.centerY() <= player.mRect.centerY()){

                //e se il disco piccolo sta a sinistra rispetto a quello grande
                if(mRect.right >= mRect.left && mRect.centerX() <= player.mRect.centerX() && (discPositionCollision == null || discPositionCollision.equals("ALTO_SINISTRA"))){

                    if(discPositionCollision == null){
                        discPositionCollision = "ALTO_SINISTRA";
                    }

                    mRect.top -= y_dist;
                    mRect.left -= clamp(0, x_dist, PublicConstants.SCREEN_WIDTH - mSize);

                    mCollisionDebug[0]++;
                    Log.d("M COLLISION DEBUG", " = " + mCollisionDebug[0] + " | "  + mCollisionDebug[1] + " | "  + mCollisionDebug[2] + " | " + mCollisionDebug[3]);
                }else {

                    //e se il disco piccolo sta a destra rispetto a quello grande
                    if (mRect.left <= mRect.right && mRect.centerX() > player.mRect.centerX() && (discPositionCollision == null || discPositionCollision.equals("ALTO_DESTRA"))) {

                        if(discPositionCollision == null){
                            discPositionCollision = "ALTO_DESTRA";
                        }

                        mRect.top -= x_dist;
                        mRect.left += clamp(0, x_dist, PublicConstants.SCREEN_WIDTH - mSize);

                        mCollisionDebug[1]++;
                        Log.d("M COLLISION DEBUG", " = " + mCollisionDebug[0] + " | "  + mCollisionDebug[1] + " | "  + mCollisionDebug[2] + " | " + mCollisionDebug[3]);
                    }
                }
            }else {

                //il disco piccolo sta sotto quello grande
                if (mRect.top < player.mRect.bottom && mRect.centerY() > player.mRect.centerY()) {

                    //e se il disco piccolo sta a sinistra rispetto a quello grande
                    if (mRect.right >= mRect.left && mRect.centerX() <= player.mRect.centerX() && (discPositionCollision == null || discPositionCollision.equals("BASSO_SINISTRA"))) {

                        if(discPositionCollision == null){
                            discPositionCollision = "BASSO_SINISTRA";
                        }

                        mRect.top += y_dist;
                        mRect.left -= clamp(0, x_dist, PublicConstants.SCREEN_WIDTH - mSize);

                        mCollisionDebug[2]++;
                        Log.d("M COLLISION DEBUG", " = " + mCollisionDebug[0] + " | "  + mCollisionDebug[1] + " | "  + mCollisionDebug[2] + " | " + mCollisionDebug[3]);
                    }else {

                        //e se il disco piccolo sta a destra rispetto a quello grande
                        if (mRect.left <= mRect.right && mRect.centerX() > player.mRect.centerX() && (discPositionCollision == null || discPositionCollision.equals("BASSO_DESTRA"))) {

                            if(discPositionCollision == null){
                                discPositionCollision = "BASSO_DESTRA";
                            }

                            mRect.top += y_dist;
                            mRect.left += clamp(0, x_dist, PublicConstants.SCREEN_WIDTH - mSize);

                            mCollisionDebug[3]++;
                            Log.d("M COLLISION DEBUG", " = " + mCollisionDebug[0] + " | "  + mCollisionDebug[1] + " | "  + mCollisionDebug[2] + " | " + mCollisionDebug[3]);
                        }
                    }
                }
            }


        }else if(!collided()){
            inCollision = 0;
            discPositionCollision = null;
            for(int i = 0; i < 4; i++){
                mCollisionDebug[i] = 0;
            }
        }



        mRect.left  += (mXVelDir * timeDelta);
        mRect.top   += (mYVelDir * timeDelta);
        mRect.right  = mRect.left + mSize;
        mRect.bottom = mRect.top  + mSize;
        if(mRect.left <= 0){
            mRect.offset(-mRect.left, 0);
            mXVelDir = -mXVelDir;
            renderThread.gt.playMp_sfx_disc_1();
        }
        else if(mRect.right >= renderThread.mWidth){
            mRect.offset(renderThread.mWidth - mRect.right, 0);
            mXVelDir = -mXVelDir;
            renderThread.gt.playMp_sfx_disc_1();
        }
        if(mRect.top <= 0 && isDiscOnMyScreen && arrived){
            //se il disco sale sopra lo schermo, devo distruggere questo disco e comunicare all'altro dispositivo la posizione del disco
            //rispetto alla parete sinistra (in relazione alla dimensione del dispositivo). Però devo farlo una volta sola, quindi uso
            //una variabile per farlo

            Log.d("DISC", "Value of isDiscOnMyScreen = " + isDiscOnMyScreen);

            //quindi: cominciamo col dire all'altro dispositivo che deve ricevere il disco.
            float dist = mRect.centerX() / PublicConstants.SCREEN_WIDTH;
            renderThread.gt.myWriteGame(dist);

            //dopo avergli comunicato la distanza dalla parete sinistra, devo dirgli velocità e orizzontale del disco
            renderThread.gt.myWriteGame(mXVelDir / PublicConstants.SCREEN_WIDTH);
            renderThread.gt.myWriteGame(mYVelDir / PublicConstants.SCREEN_WIDTH);
            Log.d("DISC", "Wrote distance and velocity");

            //poi mi segno che il disco non ce l'ho più io
            isDiscOnMyScreen = false;

        }else if(mRect.bottom < 0 && arrived){
            //se il disco è completamente fuori dallo schermo, lo rimuovo dalla memoria. Tanto verrà riallocato quando ce ne sarà bisogno
            renderThread.cancelDisc();
        }else if(mRect.top > 0 && !arrived){
            //se il disco mi è arrivato dall'altro giocatore e è completamente visibile a schermo, lo considero arrived (posso comunicare
            //dati all'altro giocatore) e nel mio schermo
            arrived = true;
        }




        if(mRect.top >= renderThread.mHeight){
            //se il disco ha toccato il bordo inferiore del mio schermo, devo far sapere all'altro giocatore che ha fatto un punto

            //prima però cancello dalla memoria il disco. Se ne creerà uno nuovo subito dopo i festeggiamenti dell'avversario
            renderThread.cancelDisc();

//            mRect.offset(0, renderThread.mHeight - mRect.bottom);
//            mYVelDir = -mYVelDir;

            //e infine dico all'avversario che ha fatto punto
            renderThread.gt.myWriteGame(PublicConstants.MESSAGE_YOU_SCORED);
        }
    }

    @Override
    public void draw(Canvas c)
    {
        c.drawCircle(mRect.centerX(), mRect.centerY(),
                mSize/ 2.0f, mPaint);
    }




    //funzione per controllare se c'è stata una collisione tra il disco e il player
    private boolean collided(){
        if(player!=null) {
            float r_sum = mSize / 2 + player.mSize / 2;
            float x_dist = mRect.centerX() - player.mRect.centerX();
            float y_dist = mRect.centerY() - player.mRect.centerY();
            if ((Math.pow(x_dist, (double) 2) + (Math.pow(y_dist, (double) 2))) < Math.pow(r_sum, 2)) {
                Log.d("TRUE", "true");
                return true;
            }
        }
        Log.d("FALSE", "false");
        return false;
    }

    //funzione usata per tenere il disco all'interno dello schermo
    private float clamp(float min, float wanted, float max){
        //se sto uscendo dallo schermo da sinistra o dall'alto, voglio rimanere dentro
        if(wanted <= min){
            return min;
        }
        if(wanted >= max){
            return max;
        }
        return wanted;
    }





}
