package com.qozix.tileview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Process;

import com.qozix.tileview.io.StreamProvider;

import java.io.InputStream;
import java.util.concurrent.ThreadPoolExecutor;

import android.util.Log;

import java.util.Locale;

public class Tile implements Runnable {

  enum State {
    IDLE, DECODING, DECODED
  }

  // variable (settable)
  private int mRow;
  private int mColumn;
  private int mRow0_vvnx;
  private int mCol0_vvnx;
  
  private int mImageSample = 1;
  private Detail mDetail;

  // variable (computed)
  private volatile State mState = State.IDLE;
  private Bitmap mBitmap;

  // lazy
  private String mCacheKey;
  
  // final default
  private final Rect mDestinationRect = new Rect();
  private final BitmapFactory.Options mDrawingOptions = new TileOptions(false);
  private final BitmapFactory.Options mMeasureOptions = new TileOptions(true);
  
  // final
  private final int mSize;
  private final DrawingView mDrawingView;
  private final Listener mListener;
  private final StreamProvider mStreamProvider;
  private final TileView.BitmapCache mMemoryCache;
  private final TileView.BitmapCache mDiskCache;
  private final TileView.BitmapPool mBitmapPool;
  private final TileView.DiskCachePolicy mDiskCachePolicy;
  private final ThreadPoolExecutor mThreadPoolExecutor;
  
  public Tile(
      int size,
      Bitmap.Config bitmapConfig,
      DrawingView drawingView,
      Listener listener,
      ThreadPoolExecutor threadPoolExecutor,
      StreamProvider streamProvider,
      TileView.BitmapCache memoryCache,
      TileView.BitmapCache diskCache,
      TileView.BitmapPool bitmapPool,
      TileView.DiskCachePolicy diskCachePolicy
  ) {
    mSize = size;
    mDrawingOptions.inPreferredConfig = bitmapConfig;
    mDrawingView = drawingView;
    mListener = listener;
    mThreadPoolExecutor = threadPoolExecutor;
    mStreamProvider = streamProvider;
    mMemoryCache = memoryCache;
    mDiskCache = diskCache;
    mBitmapPool = bitmapPool;
    mDiskCachePolicy = diskCachePolicy;
  }

  public State getState() {
    return mState;
  }

  public int getRow() {
    return mRow;
  }

  public void setRow(int row) {
    mRow = row;
  }

  public int getColumn() {
    return mColumn;
  }

  public void setColumn(int column) {
    mColumn = column;
  }
  
  public void setRow0(int row0) {
    mRow0_vvnx = row0;
  }
  
  public void setCol0(int col0) {
    mCol0_vvnx = col0;
  }

  public void setImageSample(int imageSample) {
    mImageSample = imageSample;
    mDrawingOptions.inSampleSize = mImageSample;
  }

  public Detail getDetail() {
    return mDetail;
  }

  public void setDetail(Detail detail) {
    mDetail = detail;
  }

  public Rect getDrawingRect() {
    return mDestinationRect;
  }

  public Bitmap getBitmap() {
    return mBitmap;
  }

  public BitmapFactory.Options getDrawingOptions() {
    return mDrawingOptions;
  }

  public BitmapFactory.Options getMeasureOptions() {
    return mMeasureOptions;
  }

  private void updateDestinationRect() {
    int cellSize = mSize * mDetail.getSample();
    int patchSize = cellSize * mImageSample;
    mDestinationRect.left = mColumn * cellSize;
    mDestinationRect.top = mRow * cellSize;
    mDestinationRect.right = mDestinationRect.left + patchSize;
    mDestinationRect.bottom = mDestinationRect.top + patchSize;
  }

  private String getCacheKey() {
    if (mCacheKey == null) {
	  //vvnx: sans les "_" 21810 pouvait aussi bien être 2*18 que 21*8 j'avais donc des doublons de tile au mauvais endroit		
      mCacheKey = String.valueOf(mColumn) + "_" + String.valueOf(mRow) + "_" + String.valueOf(mImageSample) + String.valueOf(mDetail.getZoom());
    }
    return mCacheKey;
  }

  // if destroyed by the time this is called, make sure bitmap stays null
  // otherwise, set bitmap, update state, send to memory cache and notify drawing view
  private void setDecodedBitmap(Bitmap bitmap) {
    if (mState != State.DECODING) {
      mBitmap = null;
      return;
    }
    mBitmap = bitmap;
    mState = State.DECODED;
    mDrawingView.setDirty();
  }

  protected void decode() throws Exception {
    if (mState != State.IDLE) {
      return;
    }
    mState = State.DECODING;
    //if (mDiskCachePolicy == TileView.DiskCachePolicy.CACHE_NONE) Log.d("vvnx", "Tile decode() CACHE_NONE");
    // this line is critical on some devices - we're doing so much work off thread that anything higher priority causes jank
    Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
    // putting a thread.sleep of even 100ms here shows that maybe we're doing work off screen that we should not be doing
    updateDestinationRect();
    String key = getCacheKey();
    //Log.d("vvnx", "la key dans tile.decode()  " + key );
    Bitmap cached = mMemoryCache.get(key);
    if (cached != null) {
      mMemoryCache.remove(key);
      setDecodedBitmap(cached);
      return;
    }
    Context context = mDrawingView.getContext();
    // garden path - image sample size is 1, we have a detail level defined for this zoom
    if (mImageSample == 1) {
      // if we cache everything to disk (usually because we're fetching from remote sources)
      // check the disk cache now and return out if we can
      if (mDiskCachePolicy == TileView.DiskCachePolicy.CACHE_ALL) {         
        cached = mDiskCache.get(key);
        if (cached != null) {
          setDecodedBitmap(cached);
          return;
        }
      }
      
      /**
      // no strong disk cache policy, go ahead and decode
      InputStream stream = mStreamProvider.getStream(mColumn + mCol0_vvnx, mRow + mRow0_vvnx , context, mDetail.getData());
      if (stream != null) {
        stream.mark(0);//pour pouvoir faire un reset après, sur mon inputstream indispensable, pas avec celui renvoyé par getAssets
        // measure it and populate measure options to pass to cache
        BitmapFactory.decodeStream(stream, null, mMeasureOptions);
        // if we made it this far, the exact bitmap wasn't in memory, but let's grab the least recently used bitmap from the cache and draw over it
        mDrawingOptions.inBitmap = mBitmapPool.getBitmapForReuse(this);
        // the measurement moved the stream's position - it must be reset to use the same stream to draw pixels
        if(stream.markSupported()) Log.d("vvnx", "tile li 203 markSupported");
        Log.d("vvnx", "tile li 204");
        stream.reset();//*****c'est ici le crux!!!!*****
        Log.d("vvnx", "tile li 206");
        Bitmap bitmap = BitmapFactory.decodeStream(stream, null, mDrawingOptions);
        Log.d("vvnx", "tile li 208 (bitmap decoded) width=" + bitmap.getWidth() + " et height=" + bitmap.getHeight() + " et bytes=" + bitmap.getByteCount());
        setDecodedBitmap(bitmap);
        if (mDiskCachePolicy == TileView.DiskCachePolicy.CACHE_ALL) {
          mDiskCache.put(key, bitmap);
        }
      }**/
      
      
      //je refais à ma sauce car l'input stream me donne des images garbled
      //String maData =  "/mnt/obb/dd20969154fdef9467fecebdfe91ac31" + (String) mDetail.getData(); //j'ai l'impression que la path de montage de l'obb est la même à chaque fois...
      
	  //Log.d("vvnx", "tile image sample = 1, column, row,  dans tile.decode()  " + mColumn + "   "  + mRow );
      String maData = (String) mDetail.getData(); 
      String maFile = String.format(Locale.US, maData, mColumn + mCol0_vvnx, mRow + mRow0_vvnx); 



      //mDrawingOptions.inBitmap = mBitmapPool.getBitmapForReuse(this);
      Bitmap bitmap = BitmapFactory.decodeFile(maFile, mDrawingOptions);
      //mDrawingOptions.inBitmap = bitmap;

      setDecodedBitmap(bitmap);
      
      /*if (mDiskCachePolicy == TileView.DiskCachePolicy.CACHE_ALL) {
          mDiskCache.put(key, bitmap);
        }*/
      
      
      
    // on a dézoomé et ya pas de tiles pour ce zoom level: création d'un canvas avec plein de bébé bitmaps dedans: utilisation de BitmapFactory.Options#inSampleSize
    // ce qui crée des mini bitmaps
    // we don't have a defined zoom level, so we need to use image sub-sampling and disk cache even if reading files locally
    } else {
	  //Log.d("vvnx", "tile image sample > 1");
      //cached = mDiskCache.get(key); //bloque, je suppose quand j'ai CACHE_NONE
      cached = null; //vvnx -> du coup je remplace par ça pour débloquer
      if (cached != null) {
        setDecodedBitmap(cached);
        return;
      }
      // if we're patching, we need a base bitmap to draw on
      // let's try to use one from the cache if we have one
      // we need to fake the measurements
      mMeasureOptions.outWidth = mSize;
      mMeasureOptions.outHeight = mSize;
      Bitmap bitmap = mBitmapPool.getBitmapForReuse(this);
      if (bitmap == null) {
        bitmap = Bitmap.createBitmap(mSize, mSize, mDrawingOptions.inPreferredConfig);
      }
      Canvas canvas = new Canvas(bitmap);
      int size = mSize / mImageSample;
      for (int i = 0; i < mImageSample; i++) {
        for (int j = 0; j < mImageSample; j++) {
          // if we got destroyed while decoding, drop out
          if (mState != State.DECODING) {
            return;
          }

          /**InputStream stream = mStreamProvider.getStream(mColumn + mCol0_vvnx + j, mRow + mRow0_vvnx + i, context, mDetail.getData());
          if (stream != null) {
            Bitmap piece = BitmapFactory.decodeStream(stream, null, mDrawingOptions);            
          }**/
          String maData = (String) mDetail.getData(); 
		  String maFile = String.format(Locale.US, maData, mColumn + mCol0_vvnx + j, mRow + mRow0_vvnx + i); 
          Bitmap piece = BitmapFactory.decodeFile(maFile, mDrawingOptions);
          
          canvas.drawBitmap(piece, j * size, i * size, null);
          
          
        }
      }
      setDecodedBitmap(bitmap);
      // we need to cache patches to disk even if local
      if (mDiskCachePolicy != TileView.DiskCachePolicy.CACHE_NONE) {
        mDiskCache.put(key, bitmap);
      }
    }
  }

  // we use this signature to call from the Executor, so it can remove tiles via iterator
  public void destroy(boolean removeFromQueue) {
    if (mState == State.IDLE) {
      return;
    }
    if (removeFromQueue) {
      mThreadPoolExecutor.remove(this);
    }
    if (mState == State.DECODED) {
      mMemoryCache.put(getCacheKey(), mBitmap);
    }
    mBitmap = null;
    mDrawingOptions.inBitmap = null;
    // since tiles are pooled and reused, make sure to reset the cache key or you'll render the wrong tile from cache
    mCacheKey = null;
    mState = State.IDLE;
    mListener.onTileDestroyed(this);
  }

  public void destroy() {
    destroy(true);
  }

  public void run() {
    try {
      decode();
    } catch (Exception e) {
      mListener.onTileDecodeError(this, e);
    }
  }

  public void draw(Canvas canvas) {
    if (mState == State.DECODED && mBitmap != null) {
      canvas.drawBitmap(mBitmap, null, mDestinationRect, null);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Tile) {
      Tile compare = (Tile) obj;
      return compare.mColumn == mColumn
          && compare.mRow == mRow
          && compare.mImageSample == mImageSample
          && compare.mDetail.getZoom() == mDetail.getZoom();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + mColumn;
    hash = hash * 31 + mRow;
    hash = hash * 31 + mImageSample;
    hash = hash * 31 + mDetail.getZoom();
    return hash;
  }

  public interface DrawingView {
    void setDirty();
    Context getContext();
  }

  public interface Listener {
    void onTileDestroyed(Tile tile);
    void onTileDecodeError(Tile tile, Exception e);
  }

  private static class TileOptions extends BitmapFactory.Options {

    //https://developer.android.com/reference/android/graphics/BitmapFactory.Options.html#inTempStorage
    private static final byte[] sInTempStorage = new byte[16 * 1024];

    TileOptions(boolean measure) {
      inMutable = true;
      inPreferredConfig = Bitmap.Config.RGB_565;
      //inTempStorage = sInTempStorage;
      inSampleSize = 1;
      //inJustDecodeBounds = measure;
    }

  }

}
