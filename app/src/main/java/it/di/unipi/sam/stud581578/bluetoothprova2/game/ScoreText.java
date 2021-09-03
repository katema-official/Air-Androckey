package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.graphics.Canvas;

import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;

public class ScoreText extends Text{

    public ScoreText(String text) {
        super(text);
    }

    @Override
    public void draw(Canvas c){
        c.drawText(text, PublicConstants.SCREEN_WIDTH - PublicConstants.SCREEN_WIDTH/10, PublicConstants.SCREEN_HEIGHT - PublicConstants.SCREEN_HEIGHT/16, mPaint);
    }
}
