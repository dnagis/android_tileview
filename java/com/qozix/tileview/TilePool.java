package com.qozix.tileview;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.Queue;

import android.util.Log;

public class TilePool {

  private final Queue<SoftReference<Tile>> mQueue = new ArrayDeque<>();
  private final Factory mFactory;

  public TilePool(Factory factory) {
    mFactory = factory;
  }

  public Tile get() {
    if (mQueue.peek() != null) {
      Tile tile = mQueue.poll().get();
      if (tile != null) {
		//Log.d("vvnx", "TilePool() on réuitlise une tile existante (tile != null) dont column=" + tile.getColumn() + "   et row=" + tile.getRow());
        return tile;
      }
    }
    return mFactory.create();
  }

  public void put(Tile tile) {
    if (tile != null) {
      mQueue.add(new SoftReference<>(tile));
    }
  }

  public void clear() {
    mQueue.clear();
  }

  public interface Factory {
    Tile create();
  }

}
