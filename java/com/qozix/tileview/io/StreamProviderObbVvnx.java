package com.qozix.tileview.io;

import android.content.Context;

import java.io.File;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Locale;

import android.os.Bundle;
import android.os.Environment;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;

import android.content.Context;

import android.util.Log;



public class StreamProviderObbVvnx implements StreamProvider {

  
  
  private static String mObbPath;
  private StorageManager mSM;

  
    OnObbStateChangeListener mEventListener = new OnObbStateChangeListener() {
        @Override
        public void onObbStateChange(String path, int state) {
            Log.d("vvnx", "onObbStateChange path=" + path + "; state=" + state);

        }
    }; 
  
  //constructeur normalement je devrais y passer qu'une fois??? à moins qu'on repasse dans onCreate() plusieurs fois?  
  public StreamProviderObbVvnx(Context context){
		Log.d("vvnx", "StreamProviderObbVvnx constructor");
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
    String file = String.format(Locale.US, (String) data, column, row);
    
    FileInputStream is = null;
    
    try {
   is = new FileInputStream(file); 

	} catch (FileNotFoundException ex) {
	Log.d("vvnx", "exception:" + ex);
	}
    
   return is;    
    
  }

}
