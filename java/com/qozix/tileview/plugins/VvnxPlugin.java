package com.qozix.tileview.plugins;

import com.qozix.tileview.TileView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import tileview.demo.R; //pour accéder à res/ qui dépend d'un autre package (le package est défini dans le manifest je suppose...)

//https://developer.android.com/guide/topics/ui/declaring-layout
public class VvnxPlugin extends View implements TileView.Plugin {
	

	//il faut un constructor pour cette class. Pour les "custom Views" c'est bien sûr un peu complexe:
	//https://stackoverflow.com/questions/9195713/do-i-need-all-three-constructors-for-an-android-custom-view
	public VvnxPlugin(Context context) {
		super(context);
		// this constructor used when programmatically creating view
		//doAdditionalConstructorWork();
	}	
	
  @Override
  public void install(TileView tileView) {
    tileView.addView(this, R.layout.vvnx_layout);
  }
}
