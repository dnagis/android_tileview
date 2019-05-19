//https://developer.android.com/training/basics/network-ops/xml
//https://www.tutorialspoint.com/android/android_xml_parsers.htm
//https://stackoverflow.com/questions/39130409/how-when-to-use-xmlpullparser-require-method

package tileview.demo;

import java.io.InputStream;
import java.io.IOException;
import android.content.Context;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import java.lang.Double;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


public class GpxReader {
	
	private XmlPullParserFactory factory;
	private XmlPullParser parser;
	private Context contextMain;
	private double[] mCoordinates;
	
	
	public GpxReader(Context context, double[] coordinates) {
		contextMain = context;	
		mCoordinates = coordinates;
		init();
    }
    
    public void init()  {
		try {
		factory = XmlPullParserFactory.newInstance();
		factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		parser = factory.newPullParser();
		} catch(XmlPullParserException e) {		}
		
	}
    
    
    
    public ArrayList<double[]> getgpx() {
		
		ArrayList<double[]> sites = new ArrayList<>();
	
		try {
		InputStream is = contextMain.getAssets().open("traces.gpx");
		parser.setInput(is, null);
		parser.nextTag();
		sites = readFichierGpx(parser);
		
		} catch(IOException | XmlPullParserException e) { Log.d("vvnx", "GpxReader.getgpx exception" + e.getMessage());}
		
		Log.d("vvnx", "GpxReader.getgpx on va retourner un array de taille=" + sites.size());	 
		
		calcul_trkpt_distances(sites);
		return sites;
		
		
	}
	
	
	private ArrayList<double[]> readFichierGpx(XmlPullParser parser) throws XmlPullParserException, IOException {
    
		ArrayList<double[]> points_du_trek = new ArrayList();

		parser.require(XmlPullParser.START_TAG, null, "gpx"); //tu peux pas mettre directos trkseg hélas...
    
	    while (parser.next() != XmlPullParser.END_DOCUMENT) {
			//tant qu'on a pas de start tag on avance...
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        //c'est un start tag donc ça a un nom (dans d'autres cas name est null)
	        String name = parser.getName();
			Log.d("vvnx", "GpxReader.readFichierGpx name=" + name);
			//quand on chope un trkseg on récupère les trkpt lat lon dans un array
	        if (name.equals("trkseg")) points_du_trek = readTrkSeg(parser);

	    }	    
	    return points_du_trek;
	}
	
	
	
	

	private ArrayList<double[]> readTrkSeg(XmlPullParser parser) throws XmlPullParserException, IOException {
		
	ArrayList<double[]> points_du_trek = new ArrayList();	
	
	Log.d("vvnx", "GpxReader.readTrkSeg start");	
	

    
    
		int eventType = parser.getEventType();
		
		label_outer_loop: //ça s'appelle un label ça mec! ça permet de faire un pseudo goto...
	    while (eventType != XmlPullParser.END_DOCUMENT) {
			
			
			switch (eventType) {
				case XmlPullParser.START_TAG:
						//String name = parser.getName(); //name est null!! https://stackoverflow.com/questions/25360955/xmlpullparser-getname-returns-null
						
				        if (parser.getName().equals("trkpt")) {
				            double lat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
				            double lon = Double.parseDouble(parser.getAttributeValue(null, "lon")); 
				            points_du_trek.add(new double[]{lat, lon});  
				            Log.d("vvnx", "GpxReader.readTrkSeg lat=" + parser.getAttributeValue(null, "lat") + " lon=" + parser.getAttributeValue(null, "lon"));					            
				        }
						

				        
				        
				case XmlPullParser.TEXT:
					break;
				
				case XmlPullParser.END_TAG:
					Log.d("vvnx", "GpxReader.readTrkSeg end tag avec name=" + parser.getName());
					if (parser.getName().equals("trk")) break label_outer_loop; //permet de sortir des deux loops!!!
					break;
					
				
                default:
                    break;
				        
				 
	        
	        
			}
	        
	        
	        
	        eventType = parser.next();
	    }
    
    
    Log.d("vvnx", "GpxReader.readTrkSeg end");	
    
    return points_du_trek;
	}
	
	
	
	public void calcul_trkpt_distances(ArrayList<double[]> sites) {
		
		Log.d("vvnx", "GpxReader.calcul_trkpt_distances");	
		
		for (double[] trkpt : sites) {
			double d = distance(mCoordinates[0], trkpt[0], mCoordinates[1], trkpt[1]);
           
           Log.d("vvnx", "GpxReader.calcul_trkpt_distances pour le site " + trkpt[0] + " " + trkpt[1] + "  =" + d);		
		}
		
	}
	
	public double distance(double lat1, double lat2, double lon1, double lon2) {

    final int R = 6371; // Radius of the earth

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distance = R * c * 1000; // convert to meters

    distance = Math.pow(distance, 2);

    return Math.sqrt(distance);
	}
	
	
	
	
	
	
}
