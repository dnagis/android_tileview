package com.qozix.tileview.io;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import android.util.Log;


public class StreamProviderAssets implements StreamProvider {
  @Override
  public InputStream getStream(int column, int row, Context context, Object data) throws IOException {
    String file = String.format(Locale.US, (String) data, column, row);
    Log.d("vvnx", "file=" + file);
    return context.getAssets().open(file);
  }
}
