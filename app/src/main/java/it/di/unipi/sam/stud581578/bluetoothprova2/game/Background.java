package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

class Background extends Renderable
{
    private final RenderThread renderThread;
    private final Paint mBg = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Background(RenderThread renderThread)
    {
        this.renderThread = renderThread;
        mBg.setColor(Color.WHITE);
    }

    @Override
    public void playfield(int width, int height)
    {

    }

    @Override
    public void update(RectF dirty, double timeDelta)
    {

    }

    @Override
    public void draw(Canvas c)
    {
        c.drawPaint(mBg);
    }
}