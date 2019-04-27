package tileview.demo;



import android.app.Activity;
import android.os.Bundle;
//import android.support.annotation.Nullable;
import com.qozix.tileview.io.StreamProvider;
import com.qozix.tileview.io.StreamProviderObbVvnx;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.lang.StringBuilder;
import java.lang.String;
import java.lang.Double;
import java.lang.Math;
import android.content.Context;

import android.widget.ImageView;

import com.qozix.tileview.TileView;
import com.qozix.tileview.plugins.CoordinatePlugin;
import com.qozix.tileview.plugins.MarkerPlugin;

import android.util.Log;
import android.content.Context;

public class TileViewDemoSimple extends Activity {
	
  //coordonnées: https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames --> en python, mon script latlong, 
  //upper left tile : COL0=33478 ROW0=23948 43.53262042681010 3.90014648437500
  //si j'ai n tiles en horizontal (x, col) et m tiles en vertical (y, row), il faut calculer les coordonnées upper left (ce que latlng donne) de la première tile out of range en bas à droite
  // c'est à dire la tile n° x' y' avec x'=x+n et y'=y+m 
  /*public static final double NORTH = 43.95723647202563 ... WEST = 3.68041992187500 ... SOUTH = 43.85829677916184 EAST = 3.81774902343750;*/
  double WEST;
  double EAST;
  double NORTH;
  double SOUTH;
  double[] coordinates = new double[]{43.9341011047363,3.70944619178772};
  int n_tiles_x, n_tiles_y, col_0, row_0, sizePixelW, sizePixelH;
  
  TileView tileView;
  MarkerPlugin markerPlugin;
  CoordinatePlugin coordinatePlugin;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
//  protected void onCreate(@Nullable Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demos_tileview);
    
    //accéder à mon espace perso en espérant que ce soit plus rapide qu'external storage...
    //Context mContext = getApplicationContext();    
    //Log.d("vvnx", "getfilesdir=" + mContext.getFilesDir()); //me donne /data/user/0/tileview.demo/files
    
	 /**
	 * les tiles de 256*256 dans assets/ au format phi-1000000-[0-69]_[0-52].jpg  
	 * 69*256 = 17920
	 * 52*256 = 13312
	 * 
	 * 7*256 = 1792
	 * 5*256 = 1280
	 */
	 
	 int zoom = 65536; // 2^16
	 
	 
	 //ganges
	 n_tiles_x = 25;
	 n_tiles_y = 25;
	 col_0 = 33438; 
	 row_0 = 23841; 	 
	 
	 
	 
	 //palavas
	 //n_tiles_x = 8; 
	 //n_tiles_y = 6; 
	 //col_0 = 33478; 
	 //row_0 = 23947; 
	 
	 sizePixelW = n_tiles_x*256;
	 sizePixelH = n_tiles_y*256;
	 WEST = (double)col_0/zoom*360.0-180.0;
	 EAST = (double)(col_0+n_tiles_x)/zoom*360.0-180.0;
	 NORTH = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (double)row_0/zoom))));
	 SOUTH = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (double)(row_0+n_tiles_y)/zoom))));
	 
	 //Log.d("vvnx", "onCreate, mes boundaries calculées: WEST=" + WEST + " EAST=" + EAST + " NORTH=" + NORTH + " SOUTH=" + SOUTH);

    tileView = findViewById(R.id.tileview);
    new TileView.Builder(tileView)
		  .setSize(sizePixelW, sizePixelH)			
//        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
		  .defineZoomLevel("/sdcard/tiles/ign-%1$d_%2$d.jpg") //pour obb mettre un leading / et l'enlever pour assets
		  .setCol0(col_0) 
		  .setRow0(row_0) 
		.installPlugin(new MarkerPlugin(this))
		.installPlugin(new CoordinatePlugin(WEST, NORTH, EAST, SOUTH))
//		.addReadyListener(this::onReady)
//		.setStreamProvider(new StreamProviderObbVvnx(this))
		.setDiskCachePolicity(TileView.DiskCachePolicy.CACHE_NONE)
        .build();
     
    if(coordinatePlugin == null) {   
		//Log.d("vvnx", "onCreate, coordinatePlugin null donc on le crée");
		coordinatePlugin = tileView.getPlugin(CoordinatePlugin.class);
		coordinatePlugin.updateWidthHeightVvnx(sizePixelW, sizePixelH);
	}
    markerPlugin = tileView.getPlugin(MarkerPlugin.class);    
	int x = coordinatePlugin.longitudeToX(coordinates[1]);
	int y = coordinatePlugin.latitudeToY(coordinates[0]);
	//Log.d("vvnx", "onCreate, le marker a x=" + x + " et y=" + y);
	ImageView marker = new ImageView(this);
	marker.setImageResource(R.drawable.marker);
	markerPlugin.addMarker(marker, x, y, -0.5f, -1f, 0, 0); 

  }
  
  
  
/*    private void onReady(TileView tileView) {
    coordinatePlugin = tileView.getPlugin(CoordinatePlugin.class);
    markerPlugin = tileView.getPlugin(MarkerPlugin.class);
    recup_latlng_fichier();    
	int x = coordinatePlugin.longitudeToX(coordinates[1]);
	int y = coordinatePlugin.latitudeToY(coordinates[0]);
	Log.d("vvnx", "onReady, le marker a x=" + x + " et y=" + y);
	ImageView marker = new ImageView(this);
	marker.setImageResource(R.drawable.marker);
	markerPlugin.addMarker(marker, x, y, -0.5f, -1f, 0, 0);
  }*/
  
	@Override
    protected void onResume() {
	int x = 1;
	int y = 1;
	super.onResume();
	recup_latlng_fichier();
	if(coordinatePlugin != null) {
	recup_latlng_fichier();
	x = coordinatePlugin.longitudeToX(coordinates[1]);
	y = coordinatePlugin.latitudeToY(coordinates[0]);
	}
	//Log.d("vvnx", "onResume, coordinates[1]="+coordinates[1]+" coordinates[0]="+coordinates[0]+"et on va mettre le marker a x=" + x + " et y=" + y);
	markerPlugin.updateMarkerPos(x, y);
	//on centre sur le Marker (scrollTo -> x et y position upper left, faut centrer donc on enlève la moitié de l'écran à chaque fois
	tileView.scrollTo(x-tileView.getWidth()/2,y-tileView.getMeasuredHeight()/2);	
	
  }
  
  
    public void recup_latlng_fichier(){
	/**récup des coordonnées qui vont servir à créer marker, dans un fichier
    permissions pour lire sdcard: manifest: android.permission.READ_EXTERNAL_STORAGE
    ***et après chaque nvelle install***
    pm grant tileview.demo android.permission.READ_EXTERNAL_STORAGE**/
    
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

    //Log.d("vvnx", "contenu du fichier=" + text);
    
    String[] arrayCoord = text.toString().split(" ");
    coordinates[0] = Double.parseDouble(arrayCoord[1]);
    coordinates[1] = Double.parseDouble(arrayCoord[2]);
    
    //Log.d("vvnx", "les coordonnees dans le fichier /sdcard/gps.txt = " + coordinates[0] + " " + coordinates[1]);
    //aven corniche au nord dolmen du thaurac, pour tests
	//coordinates[0] = 43.92559366355070;
	//coordinates[1] = 3.75732421875000; 
    }

}
