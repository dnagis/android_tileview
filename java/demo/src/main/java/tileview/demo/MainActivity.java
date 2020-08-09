/*
 * 
adb uninstall tileview.demo

adb install out/target/product/generic_arm64/system/app/tv_vvnx/tv_vvnx.apk

le layout est dans res/layout/activity_demos_tileview.xml --> car setContentView(R.layout.activity_demos_tileview);

adb shell pm grant tileview.demo android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant tileview.demo android.permission.ACCESS_FINE_LOCATION


sqlite3 /data/data/tileview.demo/databases/loc.db "select datetime(FIXTIME, 'unixepoch', 'localtime'), LAT, LONG, ACC, ALT from loc;"
 
*/
package tileview.demo;

import android.app.Activity;
import android.os.Bundle;
//import android.support.annotation.Nullable; //existe pas dans mon aosp
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
import android.widget.TextView;
import android.widget.PopupMenu;
import android.view.View;
import android.view.MenuInflater;
import android.view.MenuItem;

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

public class MainActivity extends Activity implements LocationListener, PopupMenu.OnMenuItemClickListener {
	


	//Sur la SD tu ne peux pas mettre plus de 21000 fichier par dir (que le nom soit ign-32830_24162.jpg ou 32830_24162.jpg ça change rien)
	
	//String tiles_provider = "/storage/BCC1-1AEC/tiles/ign/ign-%1$d_%2$d.jpg"; //la carte SD (la vraie, physique)
	String tiles_provider = "/storage/emulated/0/tiles/ign/ign-%1$d_%2$d.jpg"; //le storage local non SD (a besoin de l'autorisation READ_EXTERNAL_STORAGE aussi)
	//String tiles_provider = "/storage/BCC1-1AEC/tiles/otm/otm-%1$d_%2$d.png";
	double WEST;
	double EAST;
	double NORTH;
	double SOUTH;
	//lozere->44.4321,3.7285 >arles->43.66815,4.62878 die->44.75441,5.37030 
	boolean fonctionnement_normal = true; //pour bloquer l utilisation de lastknownlocation dans onCreate() pour te permettre de vérifier que tu as les bonnes tiles
	double[] coordinates_centre = new double[]{44.4321,3.7285};
	double[] coordinates_loc = Arrays.copyOf(coordinates_centre, 2); //une copie de coordinates_centre (coordinates_loc = coordinates_centre pas possible)
	int n_tiles_x, n_tiles_y, col_0, row_0, sizePixelW, sizePixelH, tile_loc_x, tile_loc_y;
	

	TileView tileView;
	MarkerPluginLoc markerPluginLoc;
	MarkerPluginGpx markerPluginGpx;
	ToggleButton myButton;
	CoordinatePlugin coordinatePlugin;
	PathPlugin pathPlugin;
	public LocationManager mLocationManager;
	private BaseDeDonnees maBDD;
	TextView infoTextView;
	List<Point> points_gpx;
	PopupMenu popup;
	MenuInflater inflater;
	boolean afficheGPX;
	
	 //long: minimum time interval between location updates, in milliseconds
	private static final int MIN_TIME_HIGH = 1000;
	int MIN_TIME_LOW = 10 * 1000; //301 * 1000 -> un peu plus que 5 min car LoctionManagerService.java: max interval a loc request can have and still be considered "high power" HIGH_POWER_INTERVAL_MS = 5 * 60 * 1000;
	
	
    private static final int MIN_DIST = 0; //float: minimum distance between location updates, in meters
    private static final int MIN_DIST_BACKGRND = 0; //au on_stop, on_pause


	//éviter de repasser là dedans à chaque rotation (portrait/paysage) de l'écran -> manifest: android:configChanges="orientation|screenLayout|screenSize"
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//protected void onCreate(@Nullable Bundle savedInstanceState) {
		Log.d("vvnx", "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demos_tileview);
		infoTextView = (TextView) findViewById(R.id.textview1);
		myButton = (ToggleButton)  findViewById(R.id.bouton2);
		afficheGPX = false;

		
		maBDD = new BaseDeDonnees(this);
		
		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_HIGH, MIN_DIST, this);		
		Location lastKnownLocationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	
		if (fonctionnement_normal && lastKnownLocationGPS != null) {
			coordinates_loc[0] = lastKnownLocationGPS.getLatitude();
			coordinates_loc[1] = lastKnownLocationGPS.getLongitude();
		}
		
		if (!fonctionnement_normal) infoTextView.setText("ATTENTION TEST MODE");
		createTileviewMain();
		
		//foreground service pour importance (am package-importance com.example.android.hellogps) à 125
		startForegroundService(new Intent(this, ForegroundService.class));		
	}

	public void createTileviewMain() {
		//Log.d("vvnx", "createTileviewMain");
		
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
		
		Log.d("vvnx", "onCreate, tile_loc_x=" + tile_loc_x + " tile_loc_y=" + tile_loc_y + " col_0=" + col_0 + " row_0=" + row_0);
		//Log.d("vvnx", "onCreate, mes boundaries calculées: WEST=" + WEST + " EAST=" + EAST + " NORTH=" + NORTH + " SOUTH=" + SOUTH);
		
		//if (tileView == null) Log.d("vvnx", "createTileViewMain tileview est null...");
		
		tileView = findViewById(R.id.tileview);
		new TileView.Builder(tileView)
			.setSize(sizePixelW, sizePixelH)			
			//        .defineZoomLevel("tiles/phi-1000000-%1$d_%2$d.jpg")
			//.defineZoomLevel("/sdcard/tiles/ign-%1$d_%2$d.jpg") //pour obb mettre un leading / et l'enlever pour assets
			//.defineZoomLevel("/storage/BCC1-1AEC/tiles/ign/ign-%1$d_%2$d.jpg") // storage/BCC1-1AEC/ c'est la carte sd removable
			.defineZoomLevel(tiles_provider)
			.setCol0(col_0) 
			.setRow0(row_0) 
			.installPlugin(new MarkerPluginLoc(this))
			.installPlugin(new MarkerPluginGpx(this))
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
		int x = coordinatePlugin.longitudeToX_at_scale_1_vvnx(coordinates_centre[1]);
		int y = coordinatePlugin.latitudeToY_at_scale_1_vvnx(coordinates_centre[0]);
		//Log.d("vvnx", "marker a ajouter a latlng=" + coordinates_loc[0] + "," + coordinates_loc[1] + " avec x y = " + x + " " +y);			
		ImageView markerLocation = new ImageView(this);
		markerLocation.setImageResource(R.drawable.marker); //le png
		markerPluginLoc.addMarker(markerLocation, x, y, -0.5f, -1f, 0, 0);

		//Creation de la liste des points gpx de la trace la plus proche(assets/traces.gpx) voir la classe GPXReader
		points_gpx = new ArrayList<>();		
		GpxReader gpxReader = new GpxReader(this, coordinates_centre);
		ArrayList<double[]> sites = gpxReader.getgpx();		
		for (double[] coordinate : sites) {
		  Point point = new Point();
		  point.x = coordinatePlugin.longitudeToX(coordinate[1]);
		  point.y = coordinatePlugin.latitudeToY(coordinate[0]);
		  points_gpx.add(point);
		}

		//Affichage des points gpx
		if(markerPluginGpx == null) markerPluginGpx = tileView.getPlugin(MarkerPluginGpx.class);
		markerPluginGpx.removeAllViews();
		for (Point point : points_gpx) {
		  //Log.d("vvnx", "add point x=" + point.x + " et y=" + point.y);
		  ImageView markerGpx = new ImageView(this); //car pas possible dutiliser plusieurs fois la même view (erreur runtime this child already has a parent)
		  markerGpx.setImageResource(R.drawable.dot);
		  markerPluginGpx.addMarker(markerGpx, point.x, point.y, -0.5f, -1f, 0, 0);
		}
		
		markerPluginGpx.toggleVisibility(afficheGPX);
	}

  
	@Override
	protected void onResume() {
		super.onResume();		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_HIGH, MIN_DIST, this);
	}	
  
	@Override
	protected void onPause() {
		super.onPause();
		Log.d("vvnx", "onPause");
		//mLocationManager.removeUpdates(this);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_LOW, MIN_DIST_BACKGRND, this);
	}
	
	@Override
    protected void onStop() {
        super.onStop();
        Log.d("vvnx", "onStop");
        //mLocationManager.removeUpdates(this);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_LOW, MIN_DIST_BACKGRND, this);
    }
	
	
	
	
	/**
	 * Menu
	 * res/menu/menu_main.xml
	 * 
	 * 
	 * */	
	
	public void showPopup(View v) {
		if (popup == null) { //Si nouvelle instance PopupMenu à chaque fois on retain pas le state isChecked des items...
			Log.d("vvnx", "Popup menu null");
		    popup = new PopupMenu(this, v);
		    inflater = popup.getMenuInflater();
		    inflater.inflate(R.menu.menu_main, popup.getMenu());
		    popup.setOnMenuItemClickListener(this); //PopupMenu.OnMenuItemClickListener
		}
	 	    popup.show();
	}	
		
	@Override
	public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
		//menu1_1 et menu1_2 mutuellement exclusifs car <group android:checkableBehavior="single">
        case R.id.menu1_1:			
            if (!item.isChecked()) {
				//Log.d("vvnx", "menu IGN");
				item.setChecked(true);
				tiles_provider = "/storage/emulated/0/tiles/ign/ign-%1$d_%2$d.jpg";
			};
            return true;
        case R.id.menu1_2:
            if (!item.isChecked()) {
				//Log.d("vvnx", "menu OTM");
				item.setChecked(true);
				tiles_provider = "/storage/emulated/0/tiles/otm/otm-%1$d_%2$d.png";
			};
            return true;
        case R.id.menu2:
			afficheGPX = !afficheGPX;
			markerPluginGpx.toggleVisibility(afficheGPX);
			item.setChecked(afficheGPX);
			return true;
		case R.id.menu3_1:
			reCreateTVonLoc(44.75441,5.37030);
			return true;
		case R.id.menu3_2:
			reCreateTVonLoc(44.48271,6.41672);
			return true;
		case R.id.menu3_3:
			reCreateTVonLoc(44.72210,6.43404);
			return true;
		case R.id.menu3_4:
			reCreateTVonLoc(42.81492,-0.29097);
			return true;
		case R.id.menu3_5:
			reCreateTVonLoc(42.78428,-0.07257);
			return true;
		case R.id.menu4_1:
		    if (!item.isChecked()) {
				item.setChecked(true);
				MIN_TIME_LOW = 10 * 1000;
			};			
			return true;
		case R.id.menu4_2:
		    if (!item.isChecked()) {
				item.setChecked(true);
				MIN_TIME_LOW = 301 * 1000;
			};			
			return true;
		case R.id.menu4_3:
		    if (!item.isChecked()) {
				item.setChecked(true);
				MIN_TIME_LOW = 30 * 60 * 1000;
			};			
			return true;
		case R.id.menu5:
		    Log.d("vvnx", "menu 5 (quit)");		
		    stopService(new Intent(this, ForegroundService.class));
		    onStop();		    
			return true;	
        default:
            return false;
		}
	}
	
	public void reCreateTVonLoc(double y_deg, double x_deg) {
		coordinates_centre[0] = y_deg;
		coordinates_centre[1] = x_deg;
		coordinatePlugin = null; 
		markerPluginLoc.removeMarkers();
		markerPluginLoc = null;
		tileView = null;
		createTileviewMain();		
		//ToDo: sticky bitmaps qui font chier... invalidate???
		tileView.setScale(1.0f);		
	}
	
	
	
	/**
	 *
	 * Boutons
	 * 
	 * 
	 * */
	//Bouton pour recentrer sur coordinates_loc et passer à scale 1
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
					//Log.d("vvnx", "La loc est off-grid");
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
	
	//Le state de ce bouton est utilisé pour décider si le marker est actualisé au location received (parce que j'ai l'impression que ça peut faire sauter la view)
	public void ActionPressBouton2(View v) {
			//if ( trkButton.isChecked() == true ) { Log.d("vvnx", "bouton 3 on"); } else { Log.d("vvnx", "bouton 3 off"); }
	}
	
	/**bouton pour toggle GPX, enlevé car menu
	public void ActionPressBouton3(View v) {
		markerPluginGpx.toggleVisibility(trkButton.isChecked());		
		/**pathPlugin.toggle_transparent(trkButton.isChecked()); //set la couleur de paint transparent
		si je ne demande pas un redraw (redecorate()) du canvas, je n'ai l'effet qu'au prochain mouvement: c'est moche!	
		tileView.setDirty(); dirty, invalidate, postvalidate etc... ça dit en gros 'je suis outdated, redessine moi!!!'		
		soucis lors du test avant Lozère: si path de grande taille: perte fluidité que cest rien de le dire ("performance canvas ondraw path")
		donc en attendant mieux: workaround moche mais efficace (destruction de la path)		
		pathPlugin.clear();* * /
		} **/
	
	//re-création tileview autour du centre de lecran visible du tel
	public void ActionPressBouton4(View v) {
		
		Point centre_ecran = tileView.centreEcran(); //faut accéder a scaledviewport, chiant à faire ici
		double scaleBefore = tileView.getScale();
		//Log.d("vvnx", "bouton 4 centre en pixels:  --> " + centre_ecran.x + " " + centre_ecran.y);
		double lng = coordinatePlugin.xToLongitude_at_scale_1_vvnx(centre_ecran.x);
		double lat = coordinatePlugin.yToLatitude_at_scale_1_vvnx(centre_ecran.y);		
		//Log.d("vvnx", "bouton 4 centre latlng:  --> " + lat + "," + lng);			
		coordinatePlugin = null; 
		markerPluginLoc.removeMarkers();
		markerPluginLoc = null;
		tileView = null;
		coordinates_centre[0] = lat;
		coordinates_centre[1] = lng;		
		createTileviewMain();		
		//si je bouge pas jai des sticky bitmaps faut donc bouger... au centre de tileview...
		tileView.setScale((float)scaleBefore);
		tileView.scrollTo((int)(6400*scaleBefore),(int)(6400*scaleBefore)); //nb ça le met en haut à gauche... pas super grave...
	}

	/**
	 *
	 * MainActivity implements LocationListener --> il faut les 4 méthodes 
	 * 
	 * 
	 **/    
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
		
		if (fonctionnement_normal) infoTextView.setText(sdf.format(location.getTime()) + " || " + (int)location.getAccuracy());
		
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
