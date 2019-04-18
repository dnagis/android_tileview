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
import java.lang.Double;
import android.content.Context;

import android.widget.ImageView;

import com.qozix.tileview.TileView;
import com.qozix.tileview.plugins.CoordinatePlugin;
import com.qozix.tileview.plugins.MarkerPlugin;

import android.util.Log;
import android.content.Context;

public class TileViewDemoSimple extends Activity {
	
  //coordonnées: https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames --> en python, mon script latlong, 
  //palavas
  //upper left tile : COL0=33478 ROW0=23948 43.53262042681010 3.90014648437500
  //angle bas droite j'ai tile max bas à dte 33484 23952 donc je calcule upper left corner de 33484+1 23952+1 43.51270490464819 3.93859863281250
  public static final double NORTH = 43.53262042681010;
  public static final double WEST = 3.90014648437500;
  public static final double SOUTH = 43.51270490464819;
  public static final double EAST = 3.93859863281250;
  //43.5196571350098,3.91340827941895 marine du prevost au bout de la promenade 
  double[] coordinate = new double[]{43.5196571350098,3.91340827941895};
  

  @Override
  protected void onCreate(Bundle savedInstanceState) {
//  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    
    //accéder à mon espace perso en espérant que ce soit plus rapide qu'external storage...
    //Context mContext = getApplicationContext();    
    //Log.d("vvnx", "getfilesdir=" + mContext.getFilesDir()); //me donne /data/user/0/tileview.demo/files
    
    
    //récup des coordonnées qui vont servir à créer marker, dans un fichier
    //permissions pour lire sdcard: manifest: android.permission.READ_EXTERNAL_STORAGE
    //***et***
    //pm grant tileview.demo android.permission.READ_EXTERNAL_STORAGE
	File fichier = new File("/sdcard/gps.txt");    
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
				    Log.d("vvnx", "erreur" + e.getMessage());	}

    Log.d("vvnx", "contenu du fichier=" + text);
    
    String[] monArray = text.toString().split(" ");
    //coordinate[0] = Double.parseDouble(monArray[1]);
    //coordinate[1] = Double.parseDouble(monArray[2]);
    
    Log.d("vvnx", "les coordonnees quon va utiliser pr le marker = " + coordinate[0] + " " + coordinate[1]);
     
    
	 /**
	 * dans assets ils mettent phi-1000000-[0-69]_[0-52].jpg de 256*256 
	 * 69*256 = 17920
	 * 52*256 = 13312
	 * 
	 * 7*256 = 1792
	 * 5*256 = 1280
	 */

    TileView tileView = findViewById(R.id.tileview);
    new TileView.Builder(tileView)
//        .setSize(17934, 13452) //pour 69 col par 52 row
			.setSize(1792, 1280) //pour 7 col par 5 row
//        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
		  .defineZoomLevel("tiles/ign-%1$d_%2$d.jpg")
//		  .setRow0(23820) //ganges
//		  .setCol0(33408) //ganges
		  .setCol0(33478) //pal
		  .setRow0(23948) //pal
		.installPlugin(new MarkerPlugin(this))
		.installPlugin(new CoordinatePlugin(WEST, NORTH, EAST, SOUTH))
		.addReadyListener(this::onReady)
        .build();

  }
  
  
  
    private void onReady(TileView tileView) {
    CoordinatePlugin coordinatePlugin = tileView.getPlugin(CoordinatePlugin.class);
    MarkerPlugin markerPlugin = tileView.getPlugin(MarkerPlugin.class);    
	int x = coordinatePlugin.longitudeToX(coordinate[1]);
	int y = coordinatePlugin.latitudeToY(coordinate[0]);
	Log.d("vvnx", "le marker a x=" + x + " et y=" + y);
	ImageView marker = new ImageView(this);
	marker.setImageResource(R.drawable.marker);
	markerPlugin.addMarker(marker, x, y, -0.5f, -1f, 0, 0);
  }

}
