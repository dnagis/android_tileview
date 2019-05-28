/*
 * 
le layout est dans res/layout/activity_demos_tileview.xml --> car setContentView(R.layout.activity_demos_tileview);

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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.CornerPathEffect;
import android.util.DisplayMetrics;

import android.widget.ImageView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ToggleButton;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.widget.TextView;

import com.qozix.tileview.TileView;
import com.qozix.tileview.plugins.CoordinatePlugin;
import com.qozix.tileview.plugins.MarkerPlugin;
import com.qozix.tileview.plugins.InfoWindowPlugin;
import com.qozix.tileview.plugins.PathPlugin;

import android.util.TypedValue;
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
	double[] coordinates = new double[]{44.4017,3.8456};
	int n_tiles_x, n_tiles_y, col_0, row_0, sizePixelW, sizePixelH, tile_loc_x, tile_loc_y;
	

	TileView tileView;
	MarkerPlugin markerPlugin;
	ToggleButton myButton, trkButton;
	CoordinatePlugin coordinatePlugin;
	PathPlugin pathPlugin;
	public LocationManager mLocationManager;
	private BaseDeDonnees maBDD;
	TextView infoTextView;
	
	
	private static final int MIN_TIME = 1000; //long: minimum time interval between location updates, in milliseconds
    private static final int MIN_DIST = 1; //float: minimum distance between location updates, in meters


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//protected void onCreate(@Nullable Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demos_tileview);
		infoTextView = (TextView) findViewById(R.id.textview1);
		myButton = (ToggleButton)  findViewById(R.id.bouton2);
		trkButton = (ToggleButton)  findViewById(R.id.bouton3);
		
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, this);
		maBDD = new BaseDeDonnees(this);
		
		int zoom = 65536; // 2^16 avec 16=niveau de zoom des tiles
		
		n_tiles_x = 50;
		n_tiles_y = n_tiles_x;
		
		sizePixelW = n_tiles_x*256;
		sizePixelH = n_tiles_y*256;
		
		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	
		if (lastKnownLocationGPS != null) {
			coordinates[0] = lastKnownLocationGPS.getLatitude();
			coordinates[1] = lastKnownLocationGPS.getLongitude();
			//Log.d("vvnx", "onCreate, on a une lastKnownLoc lat=" + coordinates[0] + " lng=" + coordinates[1] );
		}
		
		//coordonnées -> x_tile, y_tile (pour voir les tiles avec leur n°: http://tools.geofabrik.de/map/#16/43.9174/3.7322&type=Geofabrik_Standard&grid=1)
		tile_loc_x = (int)((coordinates[1] + 180.0) / 360 * zoom);
		double lat_rad = Math.toRadians(coordinates[0]);
		tile_loc_y = (int)((1.0 - Math.log(Math.tan(lat_rad) + (1 / Math.cos(lat_rad))) / Math.PI) / 2.0 * zoom);
		
		col_0 = tile_loc_x - (n_tiles_x / 2);
		row_0 = tile_loc_y - (n_tiles_y / 2);
		
		//col_0 row_0 -> coordonnées pour coordinates plugin
		WEST = (double)col_0/zoom*360.0-180.0;
		EAST = (double)(col_0+n_tiles_x)/zoom*360.0-180.0;
		NORTH = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (double)row_0/zoom))));
		SOUTH = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (double)(row_0+n_tiles_y)/zoom))));
		
		Log.d("vvnx", "onCreate, tile_loc_x=" + tile_loc_x + " tile_loc_y=" + tile_loc_y + " col_0=" + col_0 + " row_0=" + row_0);
		//Log.d("vvnx", "onCreate, mes boundaries calculées: WEST=" + WEST + " EAST=" + EAST + " NORTH=" + NORTH + " SOUTH=" + SOUTH);
		
		tileView = findViewById(R.id.tileview);
		new TileView.Builder(tileView)
			.setSize(sizePixelW, sizePixelH)			
			//        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
			//.defineZoomLevel("/sdcard/tiles/ign-%1$d_%2$d.jpg") //pour obb mettre un leading / et l'enlever pour assets
			.defineZoomLevel("/storage/BCC1-1AEC/tiles/ign-%1$d_%2$d.jpg") // storage/BCC1-1AEC/ c'est la carte sd removable
			.setCol0(col_0) 
			.setRow0(row_0) 
			.installPlugin(new MarkerPlugin(this))
			.installPlugin(new PathPlugin())
			.installPlugin(new CoordinatePlugin(WEST, NORTH, EAST, SOUTH))
			//		.addReadyListener(this::onReady)
			//		.setStreamProvider(new StreamProviderObbVvnx(this))
			//.installPlugin(new InfoWindowPlugin(infoView))
			.setDiskCachePolicity(TileView.DiskCachePolicy.CACHE_NONE)
			.build();
		 
		if(coordinatePlugin == null) {   
			//Log.d("vvnx", "onCreate, coordinatePlugin null donc on le crée");
			coordinatePlugin = tileView.getPlugin(CoordinatePlugin.class);
			coordinatePlugin.updateWidthHeightVvnx(sizePixelW, sizePixelH);
		}
		
		//MarkerPlugin pour location mark
		
		markerPlugin = tileView.getPlugin(MarkerPlugin.class); 
		//ces xy tiennent compte de la scale. normalement on doit être à 1 car on vient de builder une TileView   
		int x = coordinatePlugin.longitudeToX(coordinates[1]);
		int y = coordinatePlugin.latitudeToY(coordinates[0]);
		
		ImageView marker = new ImageView(this);
		marker.setImageResource(R.drawable.marker); //le png
		markerPlugin.addMarker(marker, x, y, -0.5f, -1f, 0, 0); 
		
		/*PathPlugin pour gpx. Il faut Paint et Points
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(0xFF4286f4);
		paint.setStrokeWidth(0);
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics));
		
		List<Point> points = new ArrayList<>();
		
		GpxReader gpxReader = new GpxReader(this, coordinates);
		ArrayList<double[]> sites = gpxReader.getgpx();
		
		for (double[] coordinate : sites) {
		  Point point = new Point();
		  point.x = coordinatePlugin.longitudeToX(coordinate[1]);
		  point.y = coordinatePlugin.latitudeToY(coordinate[0]);
		  points.add(point);
		}
		
		pathPlugin = tileView.getPlugin(PathPlugin.class);
		pathPlugin.drawPath(points, paint);*/
		
}

  
	@Override
	protected void onResume() {
		super.onResume();		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, this);
	}
	
  
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	/**
	 * Bouton pour recentrer sur le marker et passer à scale 1
	 */
	
	public void ActionPressBouton1(View v) {
		int x = 1;
		int y = 1;	
		int x_at_scale_1 = 1;
		int y_at_scale_1 = 1;
		/*Log.d("vvnx", "bouton 1 pressé x=" + x_r + " et y=" + y_r);
		int x_r = (int)(Math.random()*((2000)+1))+2000;
		int y_r = (int)(Math.random()*((2000)+1))+2000;		
		markerPlugin.updateMarkerPos(x_r, y_r);*/
		
		if(coordinatePlugin != null) {		
			x = coordinatePlugin.longitudeToX(coordinates[1]);
			y = coordinatePlugin.latitudeToY(coordinates[0]);
			//markerplugin applique une correction scale, il ne faut pas cumuler celle de coordinatePlugin et de markerPlugin -> je crée des méthodes "at_scale_1" dans coordinatePlugin
			x_at_scale_1 = coordinatePlugin.longitudeToX_at_scale_1_vvnx(coordinates[1]);
			y_at_scale_1 = coordinatePlugin.latitudeToY_at_scale_1_vvnx(coordinates[0]);		
		}

		tileView.setScale(1f);
		/**on centre sur le Marker (scrollTo -> x et y position upper left, faut centrer donc on enlève la moitié de l'écran à chaque fois
		méthode provient de ScalingScrollView.java on utilise x et y avec correction scale**/		
		tileView.scrollTo(x_at_scale_1-tileView.getWidth()/2,y_at_scale_1-tileView.getMeasuredHeight()/2);
		

		
		
	}
	
	/**
	 * Le state de ce bouton est utilisé pour décider si le marker est actualisé au location received (parce que j'ai l'impression
	 * que ça peut faire sauter la view)
	 */	
	public void ActionPressBouton2(View v) {
			//if ( trkButton.isChecked() == true ) { Log.d("vvnx", "bouton 3 on"); } else { Log.d("vvnx", "bouton 3 off"); }
	}
	
	//bouton pour la path
	public void ActionPressBouton3(View v) {
		if ( trkButton.isChecked() == true ) { Log.d("vvnx", "bouton 3 on"); } else { Log.d("vvnx", "bouton 3 off"); }
		
		
		//pathPlugin.toggle_transparent(trkButton.isChecked()); //set la couleur de paint transparent
		//si je ne demande pas un redraw (redecorate()) du canvas, je n'ai l'effet qu'au prochain mouvement: c'est moche!	
		//tileView.setDirty(); //dirty, invalidate, postvalidate etc... ça dit en gros 'je suis outdated, redessine moi!!!'
		
		//soucis lors du test avant Lozère: si path étendue: perte fluidité que c'est rien de le dire. 
		//donc en attendant mieux: workaround moche mais efficace (destruction de la path)
		
		//pathPlugin.clear();
	}
	
	
	
	//implements LocationListener --> il faut les 4 méthodes     
    @Override	
    public void onLocationChanged(Location location) {
        Log.d("vvnx", location.getLatitude() + ",  " + location.getLongitude() + ",  " + location.getAccuracy() + ",  " + location.getAltitude() + ",  " + location.getTime());
        maBDD.logFix(location.getTime()/1000, location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getAltitude());
                
        //il faut envoyer x et y "at scale 1" pour ne pas cumuler la correction scale (zoom) de markerplugin et celle de coordinateplugin
        
        int x = coordinatePlugin.longitudeToX_at_scale_1_vvnx(location.getLongitude());
        int y = coordinatePlugin.latitudeToY_at_scale_1_vvnx(location.getLatitude());
                
        coordinates[0] = location.getLatitude();
		coordinates[1] = location.getLongitude();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		//Log.d("vvnx", "essai time="+sdf.format(location.getTime()));
		
		infoTextView.setText(sdf.format(location.getTime()) + " || " + (int)location.getAccuracy());
		
		//update markerpos seulement si bouton gps on, parce que fait sauter tileview quand redraw, chiant pour browser.
		if ( myButton.isChecked() == true ) markerPlugin.updateMarkerPos(x, y);
        
     
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
