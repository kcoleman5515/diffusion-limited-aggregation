/*
 *  Copyright 2022 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.dla.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import edu.cnm.deepdive.dla.model.Direction;
import java.util.BitSet;
import java.util.Random;

public class LatticeView extends View {

  private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
  private static final int MAX_CLICK_TRAVEL_SQUARED = 20;
  private static final int MAX_HUE = 360;

  private final Rect source = new Rect();
  private final Rect dest = new Rect();

  private OnSeedListener listener;
  private BitSet lattice;
  private Bitmap bitmap;
  private int size;
  private Random rng = new Random();
  private int startX;
  private int startY;

  public LatticeView(Context context) {
    super(context);
  }

  public LatticeView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public LatticeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public LatticeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int width = resolveSizeAndState(
        getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth(), widthMeasureSpec, 0);
    int height = resolveSizeAndState(
        getPaddingTop() + getPaddingBottom() + getSuggestedMinimumHeight(), heightMeasureSpec, 0);
    int size = Math.max(width, height);
    setMeasuredDimension(size, size);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean handled = false;
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        startX = Math.round(event.getX());
        startY = Math.round(event.getY());
        handled = true;
        break;
      case MotionEvent.ACTION_UP:
        if (event.getEventTime() - event.getDownTime() < LONG_PRESS_TIMEOUT) {
          int deltaX = Math.round(event.getX()) - startX;
          int deltaY = Math.round(event.getY()) - startY;
          if (deltaX * deltaX + deltaY * deltaY < MAX_CLICK_TRAVEL_SQUARED) {
            handled = true;
            performClick();
          }
        }
        break;
      default:
        handled = super.onTouchEvent(event);
        ;
    }
    return handled;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    listener.onSeed(this, startX * size / getWidth(), startY * size / getWidth());
    return true;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (bitmap != null) {
      dest.set(0, 0, getWidth(), getHeight());
      canvas.drawBitmap(bitmap, source, dest, null);
    }
  }

  public void setOnSeedListener(OnSeedListener listener) {
    this.listener = listener;
  }

  public void clear() {
    lattice = null;
    setSize(size);
  }

  public void setSize(int size) {
    this.size = size;
    bitmap = Bitmap.createBitmap(size, size, Config.ARGB_8888);
    source.set(0, 0, size, size);
    bitmap.eraseColor(Color.BLACK);
    if (lattice != null) {
      drawOnBitmap(lattice);
      postInvalidate();
    }
  }

  public void setLattice(BitSet lattice) {
    if (bitmap != null) {
      if (this.lattice != null) {
        if (lattice != null) {
          this.lattice.xor(lattice);
          drawOnBitmap(this.lattice);
          postInvalidate();
          this.lattice = (BitSet) lattice.clone();
        }
      } else if (lattice != null) {
        drawOnBitmap(lattice);
        postInvalidate();
        this.lattice = (BitSet) lattice.clone();
      }
    }
  }

  public void setRng(Random rng) {
    this.rng = rng;
  }

  private void drawOnBitmap(BitSet lattice) {
    for (int offset = lattice.nextSetBit(0); offset > -1; offset = lattice.nextSetBit(offset + 1)) {
      int x = offset % size;
      int y = offset / size;
      drawPixel(x, y);
    }
  }

  private void drawPixel(int x, int y) {
    float[] hsv = new float[]{0, 1, 1};
    @ColorInt int color;
    float neighborHue = getNeighborHue(x, y);
    if (neighborHue < 0) {
      neighborHue = rng.nextInt(MAX_HUE);
    } else {
      neighborHue += rng.nextInt(3) - 1;
      neighborHue %= MAX_HUE;
      if (neighborHue < 0) {
        neighborHue += MAX_HUE;
      }
    }
    hsv[0] = neighborHue;
    bitmap.setPixel(x, y, Color.HSVToColor(hsv));
  }

  private float getNeighborHue(int x, int y) {
    float[] hsv = new float[]{0, 1, 1};
    int neighborCount = 0;
    float neighborHue = -1;
    for (Direction d : Direction.values()) {
      int neighborX = x + d.getOffsetX();
      int neighborY = y + d.getOffsetY();
      if (neighborX >= 0
          && neighborX < size
          && neighborY >= 0
          && neighborY < size
      ) {
        @ColorInt int pixel = bitmap.getPixel(neighborX, neighborY);
        if (pixel != Color.BLACK
            && (++neighborCount == 1 || rng.nextInt(neighborCount) == 0)) {
          Color.colorToHSV(pixel, hsv);
          neighborHue = hsv[0];
        }
      }
    }
    return neighborHue;
  }

  public interface OnSeedListener {

    void onSeed(LatticeView view, int x, int y);

  }

}
