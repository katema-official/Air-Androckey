package it.di.unipi.sam.stud581578.bluetoothprova2.game;

import android.graphics.Canvas;
import android.graphics.RectF;

//credist: Professor Vincenzo Gervasi, Corso di perfezionamento in Game Design, Università di Pisa

abstract class Renderable {
    protected final RectF mRect = new RectF();

    public abstract void playfield(int width, int height);
    public abstract void update(RectF dirty, double timeDelta);
    public abstract void draw(Canvas c);

    public final void unionRect(RectF dirty) {
        dirty.union(mRect);
    }
}
