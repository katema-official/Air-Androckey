package it.di.unipi.sam.stud581578.bluetoothprova2.game;


import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//credist: Professor Vincenzo Gervasi, Corso di perfezionamento in Game Design, Università di Pisa

public class RenderThread extends Thread {

    final BallSurfaceView ballSurfaceView;
    SurfaceHolder mHolder;
    boolean       mQuit;
    int           mWidth;
    int           mHeight;
    CopyOnWriteArrayList<Renderable> mRenderables = new CopyOnWriteArrayList<Renderable>();



    public GameThread gt;

    Paint mDebugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    final boolean mDebugEnable = false; // flip me to see clip rect


    public RenderThread(BallSurfaceView ballSurfaceView, SurfaceHolder holder, Activity activity) {
        super(RenderThread.class.getSimpleName());
        this.ballSurfaceView = ballSurfaceView;
        mHolder = holder;


        gt = new GameThread(activity, mRenderables, this, ballSurfaceView);
        gt.execute();

        mRenderables.add(new Background(this));

        mDebugPaint.setColor(Color.GREEN);

        Log.d("RT DEBUG", "1");

    }

    public void setSize(int width, int height) {
        mWidth  = width;
        mHeight = height;
        for(int i = 0; i < mRenderables.size(); i++){
            mRenderables.get(i).playfield(width, height);
        }
    }

    @Override
    public void run() {
        mQuit = false;
        Rect dirty  = new Rect();
        RectF dirtyF = new RectF();
        double dt = 1 / 60.0; // upper-bound 60fps
        double currentTime = SystemClock.elapsedRealtime();
        while(!mQuit){
            double newTime = SystemClock.elapsedRealtime();
            double frameTime = (newTime - currentTime) / 1000.0f;
            currentTime = newTime;
            dirtyF.setEmpty();
            while(frameTime > 0.0){
                double deltaTime = Math.min(frameTime, dt);
                integrate(dirtyF, 1.0f * deltaTime);
                frameTime -= deltaTime;
            }
            dirty.set((int)dirtyF.left, (int)dirtyF.top,
                    (int)Math.round(dirtyF.right),
                    (int)Math.round(dirtyF.bottom));
            render(dirty);
        }
    }

    private void integrate(RectF dirty, double timeDelta) {
        for(int i = 0; i < mRenderables.size(); i++){
            final Renderable renderable = mRenderables.get(i);
            renderable.unionRect(dirty);
            renderable.update(dirty, timeDelta);
            renderable.unionRect(dirty);
        }
    }

    private void render(Rect dirty) {
        Canvas c = mHolder.lockCanvas(!mDebugEnable ? dirty : null);
        if(c != null){
            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if(mDebugEnable){
                c.drawRect(dirty, mDebugPaint);
            }
            for(int i = 0; i < mRenderables.size(); i++){
                mRenderables.get(i).draw(c);
            }
            mHolder.unlockCanvasAndPost(c);
        }
    }

    public void quit() {
        mQuit = true;
        try{
            ballSurfaceView.mThread.join();
        }
        catch(InterruptedException e){
            //
        }
    }




    //per passare alla BallSurfaceView un riferimento a un Renderable
    public Renderable getRenderable(int i){
        return mRenderables.get(i);
    }

    //per cancellare il disco quando passa nell'altro schermo
    public void cancelDisc(){
        Log.d("RT - CANCEL DISC", "1) La dimensione di mRenderable è " + mRenderables.size());
        for(int i = 0; i < mRenderables.size(); i++) {
            if(mRenderables.get(i) instanceof Disc){
                mRenderables.remove(i);
            }
        }
        Log.d("RT - CANCEL DISC", "2) La dimensione di mRenderable è " + mRenderables.size());
    }

}
