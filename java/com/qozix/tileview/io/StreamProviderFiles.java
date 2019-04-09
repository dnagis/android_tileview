package com.qozix.tileview.io;

import android.content.Context;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Locale;

import android.util.Log;

//for file in `ls *.jpg`; do adb push $file /sdcard/tiles/ ; done

public class StreamProviderFiles implements StreamProvider {

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
