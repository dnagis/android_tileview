package tileview.demo;

import android.app.Activity;
import android.os.Bundle;
//import android.support.annotation.Nullable;
import com.qozix.tileview.io.StreamProvider;
import com.qozix.tileview.io.StreamProviderFiles;

import com.qozix.tileview.TileView;

public class TileViewDemoSimple extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
//  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    
 /**
 * dans assets ils mettent phi-1000000-[0-69]_[0-52].jpg de 256*256 
 * 69*256 = 17920
 * 52*256 = 13312
 */

    TileView tileView = findViewById(R.id.tileview);
    new TileView.Builder(tileView)
        .setSize(17934, 13452) 
//        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
        .defineZoomLevel("tiles/ign-%1$d_%2$d.jpg")
        .setStreamProvider(new StreamProviderFiles())
        .build();

  }

}
