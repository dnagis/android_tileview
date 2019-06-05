package com.qozix.tileview.plugins;

import android.content.Context;
//import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import android.util.Log;

import com.qozix.tileview.TileView;

// TODO: recycling
public class MarkerPluginLoc extends ViewGroup implements TileView.Plugin, TileView.Listener {

  private float mScale = 1;
  public int x_mp; //vvnx: mp = marker plugin
  public int y_mp;

public MarkerPluginLoc(Context context) {
//  public MarkerPlugin(@NonNull Context context) {
    super(context);
  }

  @Override
  public void install(TileView tileView) {
    tileView.addListener(this);
    tileView.addView(this);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	//Log.d("vvnx", "MarkerPlugin.onMeasure()");
    measureChildren(widthMeasureSpec, heightMeasureSpec);
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      populateLayoutParams(child);
    }
    int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
    int availableHeight = MeasureSpec.getSize(heightMeasureSpec);
    setMeasuredDimension(availableWidth, availableHeight);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
	//Log.d("vvnx", "MarkerPlugin.onLayout() l t r b" + l + " " + t + " "  + r + " "  + b);
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        child.layout(layoutParams.mLeft, layoutParams.mTop, layoutParams.mRight, layoutParams.mBottom);
      }
    }
  }

  private LayoutParams populateLayoutParams(View child) {
    MarkerPluginLoc.LayoutParams layoutParams = (MarkerPluginLoc.LayoutParams) child.getLayoutParams();
    if (child.getVisibility() != View.GONE) {
      // actual sizes of children
      int actualWidth = child.getMeasuredWidth(); //126 - 126*180 doit dépendre du png je suppose le png fait 84x120 on est à 1.5xW et 1.5xH
      int actualHeight = child.getMeasuredHeight(); //180 
      // calculate combined anchor offsets
      float widthOffset = actualWidth * layoutParams.relativeAnchorX + layoutParams.absoluteAnchorX; //-63.0 = 126 * -0.5f
      float heightOffset = actualHeight * layoutParams.relativeAnchorY + layoutParams.absoluteAnchorY; //-180.0 = 180 * -1f
      // get offset position
      int scaledX = (int) (x_mp * mScale); //et non plus (int) (layoutParams.x * mScale)
      int scaledY = (int) (y_mp * mScale); //idem
      // par défaut android cale en haut à gauche, mais nous ce qui nous intéresse c'est la pointe du pinpoint, qui est au milieu en bas, donc ...
      layoutParams.mLeft = (int) (scaledX + widthOffset); //... à x, on enlève la moitié de la largeur...
      layoutParams.mTop = (int) (scaledY + heightOffset); //... et à y, on enlève la totalité de la hauteur, comme ça le x et le y correspondent au milieu en bas = pointe du pinpoint
      layoutParams.mRight = layoutParams.mLeft + actualWidth;
      layoutParams.mBottom = layoutParams.mTop + actualHeight;
      //Log.d("vvnx", "MarkerPlugin.populateLayoutParams mScale scaledX scaledY " + mScale + " " + scaledX + " " + scaledY);
    }
    return layoutParams;
  }

  private void reposition() {
	//Log.d("vvnx", "MarkerPlugin.reposition");
    for (int i = 0; i < getChildCount(); i++) { //get child count= toujours =1
      View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        LayoutParams layoutParams = populateLayoutParams(child);
        child.setLeft(layoutParams.mLeft);
        child.setTop(layoutParams.mTop);
        child.setRight(layoutParams.mRight);
        child.setBottom(layoutParams.mBottom);
        //Log.d("vvnx", "MarkerPlugin.reposition L T R B" + layoutParams.mLeft + " " + layoutParams.mTop + " " + layoutParams.mRight + " " + layoutParams.mBottom); // R - L = 126, B - T = 180 constant 
      }
    }
  }
  

  
  public void updateMarkerPos(int x, int y) {
	//on a reçu de nouvelles coordonnées, les garder.  
	x_mp = x;
	y_mp = y;  
		 
	for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			//child.setVisibility(VISIBLE); //si tu veux pas avoir de markers tant que t'as pas une vraie loc...
			//Log.d("vvnx", "updateMarkerPos avec x=" + x + " et y=" + y + " scale=" + mScale); 
			int left = (int) ((x * mScale) - 63.0) ;
			int top = (int) ((y * mScale) - 180.0) ;
			child.setLeft(left);
	        child.setTop(top);
	        child.setRight(left + 126);
	        child.setBottom(top + 180);
			child.invalidate();      
		}
    }
    
    
  @Override
  public void onScaleChanged(float scale, float previous) {
	//Log.d("vvnx", "MarkerPlugin.onScaleChanged()");
    mScale = scale;
    reposition();
  }

  public void addMarker(View view, int left, int top, float relativeAnchorLeft, float relativeAnchorTop, float absoluteAnchorLeft, float absoluteAnchorTop) {
    LayoutParams layoutParams = new MarkerPluginLoc.LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
        left, top,
        relativeAnchorLeft, relativeAnchorTop,
        absoluteAnchorLeft, absoluteAnchorTop);
    addView(view, layoutParams);
    //view.setVisibility(INVISIBLE); //si tu veux pas avoir de markers tant que t'as pas une vraie loc...
    //les premières coordonnées, on les garde
    x_mp = left;
    y_mp = top;
  }
  
 public void removeMarkers() {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      removeView(child);
    }
  }
  



  
  //https://developer.android.com/reference/android/view/ViewGroup.LayoutParams.html
  public static class LayoutParams extends ViewGroup.LayoutParams {

    public int x;
    public int y;
    public float relativeAnchorX;
    public float relativeAnchorY;
    public float absoluteAnchorX;
    public float absoluteAnchorY;

    private int mTop;
    private int mLeft;
    private int mBottom;
    private int mRight;

    public LayoutParams(int width, int height, int left, int top, float relativeAnchorLeft, float relativeAnchorTop, float absoluteAnchorLeft, float absoluteAnchorTop) {
      super(width, height); // -2 et -2 <--> https://developer.android.com/reference/android/view/ViewGroup.LayoutParams.html#WRAP_CONTENT
      //Log.d("vvnx", "layout params w et h" + width + " " + height);
      x = left;
      y = top;
      relativeAnchorX = relativeAnchorLeft;
      relativeAnchorY = relativeAnchorTop;
      absoluteAnchorX = absoluteAnchorLeft;
      absoluteAnchorY = absoluteAnchorTop;
    }
    


  }

}
