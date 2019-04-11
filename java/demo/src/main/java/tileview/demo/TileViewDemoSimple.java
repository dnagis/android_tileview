package tileview.demo;



import android.app.Activity;
import android.os.Bundle;
//import android.support.annotation.Nullable;
import com.qozix.tileview.io.StreamProvider;
import com.qozix.tileview.io.StreamProviderFiles;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.lang.StringBuilder;
import java.lang.String;
import android.content.Context;

import android.widget.ImageView;

import com.qozix.tileview.TileView;
import com.qozix.tileview.plugins.CoordinatePlugin;
import com.qozix.tileview.plugins.MarkerPlugin;

import android.util.Log;
import android.content.Context;

public class TileViewDemoSimple extends Activity {
	
  public static final double NORTH = -75.17261900652977;
  public static final double WEST = 39.9639998777094;
  public static final double SOUTH = -75.12462846235614;
  public static final double EAST = 39.93699709962642;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
//  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    
    //accéder à mon espace perso en espérant que ce soit plus rapide qu'external storage...
    //Context mContext = getApplicationContext();    
    //Log.d("vvnx", "getfilesdir=" + mContext.getFilesDir()); //me donne /data/user/0/tileview.demo/files
    
    //permissions pour lire sdcard: manifest: android.permission.READ_EXTERNAL_STORAGE
    //***et***
    //pm grant tileview.demo android.permission.READ_EXTERNAL_STORAGE
	File fichier = new File("/sdcard/essai.txt");    
    StringBuilder text = new StringBuilder();    
    try {
	   BufferedReader br = new BufferedReader(new FileReader(fichier));
		String line;

    while ((line = br.readLine()) != null) {
        text.append(line);
        text.append('\n');
    }
    br.close();
    
    } catch(IOException e) {
				    Log.d("vvnx", "erreur" + e.getMessage());
			}

    Log.d("vvnx", "contenu du fichier=" + text);
    
	 /**
	 * dans assets ils mettent phi-1000000-[0-69]_[0-52].jpg de 256*256 
	 * 69*256 = 17920
	 * 52*256 = 13312
	 */

    TileView tileView = findViewById(R.id.tileview);
    new TileView.Builder(tileView)
        .setSize(17934, 13452) 
        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
//        .defineZoomLevel("/data/user/0/tileview.demo/files/tiles/phi-1000000-%1$d_%2$d.jpg")
//        .defineZoomLevel("/sdcard/tiles/ign-%1$d_%2$d.jpg")
//        .setStreamProvider(new StreamProviderFiles())
		.installPlugin(new MarkerPlugin(this))
		.installPlugin(new CoordinatePlugin(WEST, NORTH, EAST, SOUTH))
		.addReadyListener(this::onReady)
        .build();

  }
  
  
  
    private void onReady(TileView tileView) {

    CoordinatePlugin coordinatePlugin = tileView.getPlugin(CoordinatePlugin.class);
    MarkerPlugin markerPlugin = tileView.getPlugin(MarkerPlugin.class);

	double[] coordinate = new double[]{-75.1494000, 39.9487722};

    
	int x = coordinatePlugin.longitudeToX(coordinate[1]);
	int y = coordinatePlugin.latitudeToY(coordinate[0]);
	ImageView marker = new ImageView(this);
	marker.setImageResource(R.drawable.marker);
	markerPlugin.addMarker(marker, x, y, -0.5f, -1f, 0, 0);
    

  }

}
