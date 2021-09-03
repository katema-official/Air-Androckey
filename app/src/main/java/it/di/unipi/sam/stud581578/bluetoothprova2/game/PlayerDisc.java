package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;

public class PlayerDisc extends Renderable{

    private final RenderThread renderThread;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public float mSize;

    private boolean touched = false;

    private float newX;
    private float newY;

    public float myXVelocity;
    public float myYVelocity;

    //per indicare se è legale toccare questo playerDisc oppure no
    public boolean active = false;

    public PlayerDisc(RenderThread renderThread, int color) {
        this.renderThread = renderThread;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color);

        mSize = PublicConstants.SCREEN_WIDTH / 3.0f;

        newX = PublicConstants.SCREEN_WIDTH/2;
        newY = 3*PublicConstants.SCREEN_HEIGHT/4;

        mRect.left  = newX - mSize/2;
        mRect.top   = newY - mSize/2;
        mRect.right  = mRect.left + mSize;
        mRect.bottom = mRect.top  + mSize;

        myXVelocity = 0;
        myYVelocity = 0;
    }


    @Override
    public void playfield(int width, int height) {
        mSize = width / 3.0f;
    }

    @Override
    public void update(RectF dirty, double timeDelta) {
        mRect.left  = newX - mSize/2;
        mRect.top   = newY - mSize/2;
        mRect.right  = mRect.left + mSize;
        mRect.bottom = mRect.top  + mSize;
    }

    @Override
    public void draw(Canvas c) {
        c.drawCircle(mRect.centerX(), mRect.centerY(),
                mSize/2, mPaint);
    }





    //metodi aventi a che fare con il touch
    public void down(int x, int y){
        if(active) {
            if (mRect.contains(x, y)) {
                touched = true;
            }
        }
    }

    public void move(int x, int y){
        if(active) {
            if (touched) {
                float xret = clamp(mRect.width() / 2,
                        x,
                        renderThread.mWidth - mRect.width() / 2);
                float yret = clamp(mRect.height() / 4 + (mSize),     //offset per la dimensione del disco
                        y,
                        renderThread.mHeight - mRect.height() / 2);
                newX = xret;
                newY = yret;
            }
        }
    }

    public void up(){
        if(active) {
            touched = false;
        }
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

    public void setXandYVelocity(float xvel, float yvel){
        this.myXVelocity = xvel;
        this.myYVelocity = yvel;
    }



    //metodo per impostare le coordinate del disco
    public void setXY(float x, float y){
        newX = x;
        newY = y;
    }

}