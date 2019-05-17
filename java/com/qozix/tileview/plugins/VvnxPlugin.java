package com.qozix.tileview.plugins;

import com.qozix.tileview.TileView;

import android.widget.LinearLayout;
import android.widget.Button;
import android.graphics.drawable.ShapeDrawable;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.util.AttributeSet;
import android.content.res.TypedArray;

//"custom views"
//https://www.vogella.com/tutorials/AndroidCustomViews/article.html
//https://stackoverflow.com/questions/9195713/do-i-need-all-three-constructors-for-an-android-custom-view

import tileview.demo.R; //pour accéder à res/ qui dépend d'un autre package (le package est défini dans le manifest je suppose...)

//https://developer.android.com/guide/topics/ui/declaring-layout
public class VvnxPlugin extends ViewGroup implements TileView.Plugin {
	

	//il faut un constructor pour cette class.
	
	public VvnxPlugin(Context context, AttributeSet attrs) {
		super(context, attrs);
		// this constructor used when programmatically creating view
		//doAdditionalConstructorWork();
	}	
	
  @Override
  public void install(TileView tileView) {
    //tileView.addView(this, R.layout.vvnx_layout); compile aussi mais je doute qu'il prenne l'arg2 comme un ViewGroup.LayoutParams
    tileView.addView(this);
  }
  
  //si extends ViewGroup il faut ça, si extends View pas la peine, mais compile quand même
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
  }
  
  public void draw(Context context) {
	  ShapeDrawable myShape = new ShapeDrawable();
		
	int x = 10;
    int y = 10;
    int width = 300;
    int height = 50;
    
    myShape.setBounds(x, y, x + width, y + height);
    

	  
  }
  
  
  
  
  
}
