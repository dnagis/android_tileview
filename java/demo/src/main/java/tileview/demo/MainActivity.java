/*

pm grant tileview.demo android.permission.READ_EXTERNAL_STORAGE
pm grant tileview.demo android.permission.ACCESS_FINE_LOCATION

sqlite3 /data/data/tileview.demo/databases/loc.db "select datetime(FIXTIME/1000, 'unixepoch', 'localtime'), LAT, LONG, ACC, ALT from loc;"
 
*/
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
import android.content.Intent;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class MainActivity extends Activity implements LocationListener {
	
	//coordonnées: https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames --> en python, mon script latlong, 
	//upper left tile : COL0=33478 ROW0=23948 43.53262042681010 3.90014648437500
	//si j'ai n tiles en horizontal (x, col) et m tiles en vertical (y, row), il faut calculer les coordonnées upper left (ce que latlng donne) de la première tile out of range en bas à droite
	// c'est à dire la tile n° x' y' avec x'=x+n et y'=y+m 
	/*public static final double NORTH = 43.95723647202563 ... WEST = 3.68041992187500 ... SOUTH = 43.85829677916184 EAST = 3.81774902343750;*/
	//
	double WEST;
	double EAST;
	double NORTH;
	double SOUTH;
	//12rpdl->43.93421087,3.71005111 fucking bartas->43.9161529541016,3.73525381088257   
	double[] coordinates = new double[]{43.9161529541016,3.73525381088257};
	int n_tiles_x, n_tiles_y, col_0, row_0, sizePixelW, sizePixelH, tile_loc_x, tile_loc_y;
	
	TileView tileView;
	MarkerPlugin markerPlugin;
	CoordinatePlugin coordinatePlugin;
	public LocationManager mLocationManager;
	private BaseDeDonnees maBDD;
	
	
	private static final int MIN_TIME = 1000; //long: minimum time interval between location updates, in milliseconds
    private static final int MIN_DIST = 1; //float: minimum distance between location updates, in meters


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//protected void onCreate(@Nullable Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demos_tileview);
		
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, this);
		maBDD = new BaseDeDonnees(this);
		
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
		
		//coordonnées -> x_tile, y_tile (geofabrik)
		tile_loc_x = (int)((coordinates[1] + 180.0) / 360 * zoom);
		double lat_rad = Math.toRadians(coordinates[0]);
		tile_loc_y = (int)((1.0 - Math.log(Math.tan(lat_rad) + (1 / Math.cos(lat_rad))) / Math.PI) / 2.0 * zoom);
		
		//col_0 row_0 -> coordonnées
		WEST = (double)col_0/zoom*360.0-180.0;
		EAST = (double)(col_0+n_tiles_x)/zoom*360.0-180.0;
		NORTH = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (double)row_0/zoom))));
		SOUTH = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (double)(row_0+n_tiles_y)/zoom))));
		
		Log.d("vvnx", "onCreate, tile_loc_x=" + tile_loc_x + " tile_loc_y=" + tile_loc_y);
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
		//ces xy tiennent compte de la scale. normalement on doit être à 1 car on vient de builder une TileView   
		int x = coordinatePlugin.longitudeToX(coordinates[1]);
		int y = coordinatePlugin.latitudeToY(coordinates[0]);
		
		ImageView marker = new ImageView(this);
		marker.setImageResource(R.drawable.marker); //le png
		markerPlugin.addMarker(marker, x, y, -0.5f, -1f, 0, 0); 	
	}

  
	@Override
	protected void onResume() {
		super.onResume();	
			int x = 1;
			int y = 1;	
			int x_at_scale_1 = 1;
			int y_at_scale_1 = 1;			
		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, this);
		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	
		if (lastKnownLocationGPS != null) {
			coordinates[0] = lastKnownLocationGPS.getLatitude();
			coordinates[1] = lastKnownLocationGPS.getLongitude();
		}
		
		
		if(coordinatePlugin != null) {		
			x = coordinatePlugin.longitudeToX(coordinates[1]);
			y = coordinatePlugin.latitudeToY(coordinates[0]);
			//markerplugin applique une correction scale, il ne faut pas cumuler celle de coordinatePlugin et de markerPlugin -> je crée des méthodes "at_scale_1" dans coordinatePlugin
			x_at_scale_1 = coordinatePlugin.longitudeToX_at_scale_1_vvnx(coordinates[1]);
			y_at_scale_1 = coordinatePlugin.latitudeToY_at_scale_1_vvnx(coordinates[0]);		
		}
		
		//on centre sur le Marker (scrollTo -> x et y position upper left, faut centrer donc on enlève la moitié de l'écran à chaque fois
		//méthode provient de ScalingScrollView.java
		//on utilise x et y avec correction scale
		tileView.scrollTo(x-tileView.getWidth()/2,y-tileView.getMeasuredHeight()/2);			
		
		markerPlugin.updateMarkerPos(x_at_scale_1, y_at_scale_1);			
	}
  
	@Override
	protected void onPause() {
		super.onPause();
		//revenir au zoom le plus fort sinon à onResume() j'ai le marker n'importe où: x et y dépendent du scale...
		//tileView.setScale(1f);
	}
	
	
	
	//implements LocationListener --> il faut les 4 méthodes     
    @Override	
    public void onLocationChanged(Location location) {
        Log.d("vvnx", location.getLatitude() + ",  " + location.getLongitude() + ",  " + 	location.getAccuracy() + ",  " + location.getAltitude() + ",  " + location.getTime());
        maBDD.logFix(location.getTime(), location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getAltitude());
        //il faut envoyer x et y "at scale 1" pour ne pas cumuler la correction scale (zoom) de markerplugin et celle de coordinateplugin
        int x = coordinatePlugin.longitudeToX_at_scale_1_vvnx(location.getLongitude());
        int y = coordinatePlugin.latitudeToY_at_scale_1_vvnx(location.getLatitude());
        markerPlugin.updateMarkerPos(x, y);	        
    }
        
	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
  


}
