package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;

import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;

public class BallSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    public RenderThread mThread;
    private VelocityTracker vt;
    private Activity activity;


    public BallSurfaceView(Activity activity){
        super(activity);
        this.activity = activity;
        setKeepScreenOn(true);
        getHolder().addCallback(this);
        Log.d("BSV DEBUG", "1");

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(vt == null){
            //prendo l'oggetto VelocityTracker
            vt = VelocityTracker.obtain();
        }else{
            //resetto quello che già ho
            vt.clear();
        }

        mThread = new RenderThread(this, holder, activity);
        mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mThread.setSize(width, height);
        PublicConstants.SCREEN_WIDTH = width;
        PublicConstants.SCREEN_HEIGHT = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread.quit();
        vt.recycle();
        vt = null;
    }

    @Override
    public void draw(Canvas canvas){
        super.draw(canvas);
        canvas.drawColor(Color.WHITE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        super.onTouchEvent(event);
        PlayerDisc p = (PlayerDisc) mThread.getRenderable(1);   //finché il playerDisc rimane nella posizione 1, va tutto bene (quindi occhio)
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(p!=null) {
                    p.down((int) event.getX(), (int) event.getY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(p!=null) {
                    vt.addMovement(event);
                    vt.computeCurrentVelocity(10);
                    p.setXandYVelocity(vt.getXVelocity(), vt.getYVelocity());
                    p.move((int) event.getX(), (int) event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                if(p!=null){
                    p.up();
                }
                break;
        }
        return true;
    }

}
