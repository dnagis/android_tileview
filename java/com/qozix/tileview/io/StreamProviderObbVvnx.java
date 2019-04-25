package com.qozix.tileview.io;

import android.content.Context;

import java.io.File;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Locale;

import java.util.concurrent.TimeUnit;

import android.os.Bundle;
import android.os.Environment;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;



import android.content.Context;

import android.util.Log;



public class StreamProviderObbVvnx implements StreamProvider {

  
  
  private static String mObbPath;
  private StorageManager mSM;
  private String obbMountedPath;
  public int obbState;
  
  

  
    OnObbStateChangeListener mEventListener = new OnObbStateChangeListener() {
        @Override
        public void onObbStateChange(String path, int state) {            
            obbState = state;
            obbMountedPath = mSM.getMountedObbPath(mObbPath);
            Log.d("vvnx", "onObbStateChange obbMountedPath=" + obbMountedPath + "; state=" + state);
        }
    }; 
  
  //constructeur normalement je devrais y passer qu'une fois??? à moins qu'on repasse dans onCreate() plusieurs fois?  
  public StreamProviderObbVvnx(Context context){
		Log.d("vvnx", "StreamProviderObbVvnx constructor");
		obbState = -99;
	    if (mSM == null) {
		 // Get an instance of the StorageManager
            mSM = (StorageManager) context.getSystemService("storage"); //https://developer.android.com/reference/android/content/Context#STORAGE_SERVICE

        } 
        mObbPath = new File(Environment.getExternalStorageDirectory(), "tiles.obb").getPath();
		Log.d("vvnx", "StreamProviderObbVvnx constructor mObbPath=" + mObbPath);
		mSM.mountObb(mObbPath, null, mEventListener);
		
		}
  

  
  
  @Override
  public InputStream getStream(int column, int row, Context context, Object data) throws Exception {
	
	/*while (obbState!=1){
		Log.d("vvnx", "obb pas encore monté on attends une seconde, obbState=" + obbState);
		TimeUnit.SECONDS.sleep(1);
	}*/
	
	
	String mData =  obbMountedPath + (String) data;
	//Log.d("vvnx", "data:" + mData);
	  
    String file = String.format(Locale.US, mData, column, row);
    
    FileInputStream is = null;
    BufferedInputStream bis = null;
    
    try {
		
  // is = new FileInputStream(file); 
	bis = new BufferedInputStream(new FileInputStream(file));

	} catch (FileNotFoundException ex) {
	Log.d("vvnx", "exception:" + ex);
	}
    
   return bis;    
    
  }

}
