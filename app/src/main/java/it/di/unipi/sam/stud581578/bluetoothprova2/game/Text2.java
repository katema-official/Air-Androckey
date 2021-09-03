package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.graphics.Canvas;

import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;

public class Text2 extends Text{
    public Text2(String text) {
        super(text);
        mPaint.setTextSize(PublicConstants.SCREEN_WIDTH/9);

    }

    @Override
    public void draw(Canvas c) {
        c.drawText(text, PublicConstants.SCREEN_WIDTH/2, PublicConstants.SCREEN_HEIGHT/2 + PublicConstants.SCREEN_HEIGHT/15, mPaint);
    }
}
