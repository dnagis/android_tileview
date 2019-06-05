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
import android.graphics.Rect;
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
import com.qozix.tileview.plugins.MarkerPluginLoc;
import com.qozix.tileview.plugins.MarkerPluginGpx;
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
	//12rpdl->43.93421087,3.71005111 fucking bartas->43.9161529541016,3.73525381088257 lozere: 44.4017,3.8456
	double[] coordinates_centre = new double[]{43.93421087,3.71005111};
	double[] coordinates_loc = new double[]{43.9161529541016,3.73525381088257};
	int n_tiles_x, n_tiles_y, col_0, row_0, sizePixelW, sizePixelH, tile_loc_x, tile_loc_y;
	

	TileView tileView;
	MarkerPluginLoc markerPluginLoc;
	MarkerPluginGpx markerPluginGpx;
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
		
		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	
		if (lastKnownLocationGPS != null) {
			coordinates_loc[0] = lastKnownLocationGPS.getLatitude();
			coordinates_loc[1] = lastKnownLocationGPS.getLongitude();
		}
		
		createTileviewMain();
		
		
		
}

	public void createTileviewMain() {
		Log.d("vvnx", "createTileviewMain");
		
		//Définitions pour créer la grid
		int zoom = 65536; // 2^16 avec 16=niveau de zoom des tiles		
		n_tiles_x = 50;
		n_tiles_y = n_tiles_x;		
		sizePixelW = n_tiles_x*256;
		sizePixelH = n_tiles_y*256;				
		
		//coordonnées -> x_tile, y_tile (pour voir les tiles avec leur n°: http://tools.geofabrik.de/map/#16/43.9174/3.7322&type=Geofabrik_Standard&grid=1)
		tile_loc_x = (int)((coordinates_centre[1] + 180.0) / 360 * zoom);
		double lat_rad = Math.toRadians(coordinates_centre[0]);
		tile_loc_y = (int)((1.0 - Math.log(Math.tan(lat_rad) + (1 / Math.cos(lat_rad))) / Math.PI) / 2.0 * zoom);
		
		col_0 = tile_loc_x - (n_tiles_x / 2);
		row_0 = tile_loc_y - (n_tiles_y / 2);
		
		//col_0 row_0 -> latlng pour coordinates plugin
		WEST = (double)col_0/zoom*360.0-180.0;
		EAST = (double)(col_0+n_tiles_x)/zoom*360.0-180.0;
		NORTH = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (double)row_0/zoom))));
		SOUTH = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * (double)(row_0+n_tiles_y)/zoom))));
		
		//Log.d("vvnx", "onCreate, tile_loc_x=" + tile_loc_x + " tile_loc_y=" + tile_loc_y + " col_0=" + col_0 + " row_0=" + row_0);
		//Log.d("vvnx", "onCreate, mes boundaries calculées: WEST=" + WEST + " EAST=" + EAST + " NORTH=" + NORTH + " SOUTH=" + SOUTH);
		
		if (tileView == null) Log.d("vvnx", "createTileViewMain tileview est null...");
		
		tileView = findViewById(R.id.tileview);
		new TileView.Builder(tileView)
			.setSize(sizePixelW, sizePixelH)			
			//        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
			//.defineZoomLevel("/sdcard/tiles/ign-%1$d_%2$d.jpg") //pour obb mettre un leading / et l'enlever pour assets
			.defineZoomLevel("/storage/BCC1-1AEC/tiles/ign-%1$d_%2$d.jpg") // storage/BCC1-1AEC/ c'est la carte sd removable
			.setCol0(col_0) 
			.setRow0(row_0) 
			.installPlugin(new MarkerPluginLoc(this))
			//.installPlugin(new MarkerPluginGpx(this))
			//.installPlugin(new PathPlugin())
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
		markerPluginLoc = tileView.getPlugin(MarkerPluginLoc.class); 
		//ces xy tiennent compte de la scale. normalement on doit être à 1 car on vient de builder une TileView 
		 
		int x = coordinatePlugin.longitudeToX_at_scale_1_vvnx(coordinates_loc[1]);
		int y = coordinatePlugin.latitudeToY_at_scale_1_vvnx(coordinates_loc[0]);
		//Log.d("vvnx", "marker a ajouter a latlng=" + coordinates_loc[0] + "," + coordinates_loc[1] + " avec x y = " + x + " " +y); 
			
		ImageView markerLocation = new ImageView(this);
		markerLocation.setImageResource(R.drawable.marker); //le png
		markerPluginLoc.addMarker(markerLocation, x, y, -0.5f, -1f, 0, 0); //

		
		/**
		//Creation de la liste des points en x et y a partir de gpx (assets/traces.gpx) voir la classe GPXReader
		List<Point> points = new ArrayList<>();		
		GpxReader gpxReader = new GpxReader(this, coordinates);
		ArrayList<double[]> sites = gpxReader.getgpx();		
		for (double[] coordinate : sites) {
		  Point point = new Point();
		  point.x = coordinatePlugin.longitudeToX(coordinate[1]);
		  point.y = coordinatePlugin.latitudeToY(coordinate[0]);
		  points.add(point);
		}				
		
		//Affichage des points gpx
		markerPluginGpx = tileView.getPlugin(MarkerPluginGpx.class);
		for (Point point : points) {
		  //Log.d("vvnx", "add point x=" + point.x + " et y=" + point.y);
		  ImageView markerGpx = new ImageView(this); //car pas possible dutiliser plusieurs fois la même view (erreur runtime this child already has a parent)
		  markerGpx.setImageResource(R.drawable.dot);
		  markerPluginGpx.addMarker(markerGpx, point.x, point.y, -0.5f, -1f, 0, 0);
		}*/
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
	 * Bouton pour recentrer sur coordinates_loc et passer à scale 1 */
	
	public void ActionPressBouton1(View v) {	
		int x_at_scale_1 = 1;
		int y_at_scale_1 = 1;				
		//x = coordinatePlugin.longitudeToX(coordinates_loc[1]);
		//y = coordinatePlugin.latitudeToY(coordinates_loc[0]);
		//markerplugin applique une correction scale, il ne faut pas cumuler celle de coordinatePlugin et de markerPlugin -> je crée des méthodes "at_scale_1" dans coordinatePlugin
		x_at_scale_1 = coordinatePlugin.longitudeToX_at_scale_1_vvnx(coordinates_loc[1]);
		y_at_scale_1 = coordinatePlugin.latitudeToY_at_scale_1_vvnx(coordinates_loc[0]);		
				
		//est ce la loc est dans la grid?
		//Log.d("vvnx", "bouton 1 pressé x@1=" + x_at_scale_1 + " et y@1=" + y_at_scale_1 + "  pour latlng=" + coordinates_loc[0] + "," + coordinates_loc[1]);
		Rect test_r = new Rect(0, 0, sizePixelW, sizePixelH);
		
		if (! test_r.contains(x_at_scale_1, y_at_scale_1)) { //si on est dans la grid, on va centrer sur x_at_scale_1, y_at_scale_1, rien à faire...
					Log.d("vvnx", "La loc est off-grid");
					//on est off-grid: faut recréer une tileview centrée sur coordinates_loc[]
					tileView = null;
					coordinatePlugin = null; 
					markerPluginLoc.removeMarkers();
					markerPluginLoc = null;
					coordinates_centre[0] = coordinates_loc[0];
					coordinates_centre[1] = coordinates_loc[1];
					createTileviewMain();
					x_at_scale_1 = coordinatePlugin.longitudeToX_at_scale_1_vvnx(coordinates_loc[1]);
					y_at_scale_1 = coordinatePlugin.latitudeToY_at_scale_1_vvnx(coordinates_loc[0]);			
			};
			
		tileView.setScale(1f);
		/**on centre sur x_at_scale_1, y_at_scale_1 (scrollTo -> x et y position upper left, faut centrer donc on enlève la moitié de l'écran à chaque fois
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
	
	//bouton pour toggle gpx
	public void ActionPressBouton3(View v) {
		//if ( trkButton.isChecked() == true ) { Log.d("vvnx", "bouton 3 on"); } else { Log.d("vvnx", "bouton 3 off"); }
		
		//markerPluginGpx.toggleVisibility(trkButton.isChecked());
		
		/**pathPlugin.toggle_transparent(trkButton.isChecked()); //set la couleur de paint transparent
		si je ne demande pas un redraw (redecorate()) du canvas, je n'ai l'effet qu'au prochain mouvement: c'est moche!	
		tileView.setDirty(); dirty, invalidate, postvalidate etc... ça dit en gros 'je suis outdated, redessine moi!!!'		
		soucis lors du test avant Lozère: si path de grande taille: perte fluidité que cest rien de le dire ("performance canvas ondraw path")
		donc en attendant mieux: workaround moche mais efficace (destruction de la path)		
		pathPlugin.clear();**/
		}
	
	//re-création tileview autour du centre de lecran visible du tel
	public void ActionPressBouton4(View v) {
		
		Point centre_ecran = tileView.centreEcran(); //faut accéder a scaledviewport, chiant à faire ici
		double scaleBefore = tileView.getScale();
		//Log.d("vvnx", "bouton 4 centre en pixels:  --> " + centre_ecran.x + " " + centre_ecran.y);
		double lng = coordinatePlugin.xToLongitude_at_scale_1_vvnx(centre_ecran.x);
		double lat = coordinatePlugin.yToLatitude_at_scale_1_vvnx(centre_ecran.y);		
		//Log.d("vvnx", "bouton 4 centre latlng:  --> " + lat + "," + lng);		
		tileView = null;
		coordinatePlugin = null; //pour avoir nouveau set de coordonnées
		markerPluginLoc.removeMarkers();
		markerPluginLoc = null;
		coordinates_centre[0] = lat;
		coordinates_centre[1] = lng;
		createTileviewMain();
		
		//si je bouge pas jai des sticky bitmaps faut donc bouger... au centre de tileview...
		tileView.setScale((float)scaleBefore);
		tileView.scrollTo((int)(6400*scaleBefore),(int)(6400*scaleBefore)); //nb ça le met en haut à gauche... pas super grave...
	}
	
	
	
	//implements LocationListener --> il faut les 4 méthodes     
    @Override	
    public void onLocationChanged(Location location) {
        Log.d("vvnx", location.getLatitude() + ",  " + location.getLongitude() + ",  " + location.getAccuracy() + ",  " + location.getAltitude() + ",  " + location.getTime());
        maBDD.logFix(location.getTime()/1000, location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getAltitude());
                
        //il faut envoyer x et y "at scale 1" pour ne pas cumuler la correction scale (zoom) de markerplugin et celle de coordinateplugin
        
        int x = coordinatePlugin.longitudeToX_at_scale_1_vvnx(location.getLongitude());
        int y = coordinatePlugin.latitudeToY_at_scale_1_vvnx(location.getLatitude());
                
        coordinates_loc[0] = location.getLatitude();
		coordinates_loc[1] = location.getLongitude();
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		//Log.d("vvnx", "essai time="+sdf.format(location.getTime()));
		
		infoTextView.setText(sdf.format(location.getTime()) + " || " + (int)location.getAccuracy());
		
		//update markerpos seulement si bouton gps on, parce que fait sauter tileview quand redraw, chiant pour browser.
		if ( myButton.isChecked() == true ) markerPluginLoc.updateMarkerPos(x, y);
        
     
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
