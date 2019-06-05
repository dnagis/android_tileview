package com.qozix.tileview.plugins;

import com.qozix.tileview.TileView;

import android.util.Log;

/**
 * Note that coordinates are generally expressed as lat, lng
 * while 2D space is generally x, y
 * these are reversed - latitude is the y-axis of the earth, and longitude is the x-axis
 *
 * North and south are longitude; east and west are latitude.
 */
public class CoordinatePlugin implements TileView.Plugin, TileView.Listener, TileView.ReadyListener {

  private float mScale = 1;

  private double mWest;  // lat
  private double mNorth; // lng

  private double mDistanceLatitude;
  private double mDistanceLongitude;

  private int mPixelWidth;
  private int mPixelHeight;

  public CoordinatePlugin(double west, double north, double east, double south) {
    mWest = west;
    mNorth = north;
    mDistanceLongitude = east - west;
    mDistanceLatitude = south - north;
  }
  
  public void updateWidthHeightVvnx(int w, int h) {
	  mPixelWidth = w;
	  mPixelHeight = h;
	}

  @Override
  public void install(TileView tileView) {
    tileView.addReadyListener(this);
    tileView.addListener(this);
  }

  @Override
  public void onReady(TileView tileView) {
    mPixelWidth = tileView.getContentWidth();
    mPixelHeight = tileView.getContentHeight();
    //Log.d("vvnx", "coordPlugin mPixelWidth=" + mPixelWidth);
  }

  // coordinate to pixel is multiplied by scale, pixel to coordinate is divided by scale
  @Override
  public void onScaleChanged(float scale, float previous) {
    mScale = scale;
  }

  /**
   * Translate longitude coordinate to an x pixel value.
   *
   * @param longitude The longitude.
   * @return The pixel value.
   */
  public int longitudeToX(double longitude) {
    double factor = (longitude - mWest) / mDistanceLongitude;
    //Log.d("vvnx", "coordPlugin mScale=" + mScale + " et factor=" + factor);
    return (int) ((mPixelWidth * factor) * mScale);
  }

  /**
   * Translate latitude coordinate to a y pixel value.
   *
   * @param latitude The latitude.
   * @return The pixel value.
   */
  public int latitudeToY(double latitude) {
    double factor = (latitude - mNorth) / mDistanceLatitude;
    return (int) ((mPixelHeight * factor) * mScale);
  }
  
    /**
	* latlng -> xy sans tenir compte de scale, pour passer à markerplugin qui applique déjà la correction scale
   */
  public int longitudeToX_at_scale_1_vvnx(double longitude) {
    double factor = (longitude - mWest) / mDistanceLongitude;
    return (int) (mPixelWidth * factor);
  }

  public int latitudeToY_at_scale_1_vvnx(double latitude) {
    double factor = (latitude - mNorth) / mDistanceLatitude;
    return (int) (mPixelHeight * factor);
  }

  /**
   * Translate an x pixel value to a longitude.
   *
   * @param x The x value to be translated.
   * @return The longitude.
   */
  public double xToLongitude(int x) {
    return mWest + (x / mScale) * mDistanceLongitude / mPixelWidth;
  }

  /**
   * Translate a y pixel value to a latitude.
   *
   * @param y The y value to be translated.
   * @return The latitude.
   */
  public double yToLatitude(int y) {
    return mNorth + (y / mScale) * mDistanceLatitude / mPixelHeight;
  }
  
  
  public double xToLongitude_at_scale_1_vvnx(int x) {
	Log.d("vvnx", "coordPlugin mPixelWidth=" + mPixelWidth + " " + mScale);
    return mWest + x * mDistanceLongitude / mPixelWidth;
  }
  
  public double yToLatitude_at_scale_1_vvnx(int y) {
    return mNorth + y * mDistanceLatitude / mPixelHeight;
  }

}
