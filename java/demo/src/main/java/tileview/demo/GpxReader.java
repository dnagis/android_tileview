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
	
	
	public GpxReader(Context context) {
		contextMain = context;		
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
		
		Log.d("vvnx", "GpxReader.getgpx");	 
		
	 
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
	        String name = parser.getName();
			Log.d("vvnx", "GpxReader.readFichierGpx name=" + name);
			//et on catche celui qui contient tout les points
	        if (name.equals("trkseg")) points_du_trek = readTrkSeg(parser);

	    }	    
	    return points_du_trek;
	}
	
	
	
	

	private ArrayList<double[]> readTrkSeg(XmlPullParser parser) throws XmlPullParserException, IOException {
		
	ArrayList<double[]> points_du_trek = new ArrayList();	
	
	Log.d("vvnx", "GpxReader.readTrkSeg start");	
	

    
    
		int eventType = parser.getEventType();
	    while (eventType != XmlPullParser.END_DOCUMENT) {
			
			
			switch (eventType) {
				case XmlPullParser.START_TAG:
						String name = parser.getName(); //name est null!! https://stackoverflow.com/questions/25360955/xmlpullparser-getname-returns-null
						
				        if (name.equals("trkpt")) {
				            double lat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
				            double lon = Double.parseDouble(parser.getAttributeValue(null, "lon")); 
				            points_du_trek.add(new double[]{lat, lon});  
				            Log.d("vvnx", "GpxReader.readTrkSeg lat=" + parser.getAttributeValue(null, "lat") + " lon=" + parser.getAttributeValue(null, "lon"));					            
				        }
						

				        
				        
				case XmlPullParser.TEXT:
					break;
				
				case XmlPullParser.END_TAG:
					break;
					
				
                default:
                    break;
				        
				 
	        
	        
			}
	        
	        
	        
	        eventType = parser.next();
	    }
    
    
    Log.d("vvnx", "GpxReader.readTrkSeg end");	
    
    return points_du_trek;
	}
	
	
	
	
	
	
}
