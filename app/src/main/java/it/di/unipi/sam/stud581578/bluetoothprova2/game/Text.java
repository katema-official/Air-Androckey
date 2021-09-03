package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;

public class Text extends Renderable{

    String text;
    Paint mPaint;

    public Text(String text){
        this.text = text;
        mPaint = new Paint();
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(5);
        mPaint.setColor(Color.DKGRAY);
        mPaint.setTextSize(PublicConstants.SCREEN_WIDTH/6);

        mRect.left = 0;
        mRect.right = PublicConstants.SCREEN_WIDTH;
        mRect.top = PublicConstants.SCREEN_HEIGHT/4;
        mRect.bottom = PublicConstants.SCREEN_HEIGHT;
    }

    @Override
    public void playfield(int width, int height) {

    }

    @Override
    public void update(RectF dirty, double timeDelta) {

    }

    @Override
    public void draw(Canvas c) {
        c.drawText(text, PublicConstants.SCREEN_WIDTH/2, PublicConstants.SCREEN_HEIGHT/2, mPaint);
    }
}
