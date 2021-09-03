package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import it.di.unipi.sam.stud581578.bluetoothprova2.PublicConstants;

public class ButtonText extends Text{

    private Paint buttonPaint;
    public RectF buttonRect;
    private int textHeight;

    public ButtonText(String text, boolean yes) {
        super(text);

        buttonPaint = new Paint();
        buttonRect = new RectF(mRect);
        mPaint.setTextSize(PublicConstants.SCREEN_WIDTH/8);


        if (yes) {
            buttonPaint.setColor(Color.GREEN);
            buttonRect.left = PublicConstants.SCREEN_WIDTH/8;

        }else{
            buttonPaint.setColor(Color.rgb(255,140,0));
            buttonRect.left = 5*PublicConstants.SCREEN_WIDTH/8;

        }
        buttonRect.right = buttonRect.left + PublicConstants.SCREEN_WIDTH/4;
        buttonRect.top = 5*PublicConstants.SCREEN_HEIGHT/8;
        buttonRect.bottom = buttonRect.top + PublicConstants.SCREEN_HEIGHT/8;

        Rect bounds = new Rect();
        mPaint.getTextBounds(text, 0, text.length(), bounds);
        textHeight = bounds.height();
    }


    @Override
    public void draw(Canvas c){
        c.drawRect(buttonRect, buttonPaint);
        c.drawText(text, buttonRect.centerX(), buttonRect.centerY() + textHeight/2, mPaint);
    }

}
