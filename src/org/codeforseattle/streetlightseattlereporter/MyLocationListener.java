/**
 * 
 */
package org.codeforseattle.streetlightseattlereporter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
// import android.util.Log;

/**
 * @author Jerry Frost
 *
 */
public class MyLocationListener implements LocationListener 
{
	Context context = null;
	LocationManager locationManager = null;
	Location location = null;
	
	public MyLocationListener(Context context, LocationManager locationManager)
	{
		this.context = context;
		this.locationManager = locationManager;
	}

	/* Update application based on new location
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	@Override
	public void onLocationChanged(Location location)
	{
		locationManager.removeUpdates(this);
		String address ="Could not find location.";
		this.location = location;
		new MyAsyncTask().execute(address);
		
//		if (location != null) {
//			address = findStreetAddress(location);
//		}
//	    Intent broadcastIntent = new Intent();
//	    broadcastIntent.setAction(MainActivity.LOCATION_QUERY_STR);
//	    broadcastIntent.putExtra("address", address);
//	    context.sendBroadcast(broadcastIntent);
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}
	
    // Retry twice, as the geocoder server times out occasionally.
    private String findStreetAddress(Location location)
    {
    	String str = getAddressFromLocation(location);
    	if (str.equals("Could not retrieve the address.")) {
    		// Log.e("Geocode Server Timeout", "1st automatic retry.");
    		str = getAddressFromLocation(location);
    	}
    	if (str.equals("Could not retrieve the address.")) {
    		// Log.e("Geocode Server Timeout", "2nd automatic retry.");
    		str = getAddressFromLocation(location);
    	}
    	return str;
    }
    
    // http://stackoverflow.com/questions/9409195/how-to-get-complete-address-from-latitude-and-longitude
    private String getAddressFromLocation(Location location)
    {
    	Geocoder geocoder = new Geocoder(context, Locale.getDefault());
    	List<Address> addresses = null;
		try {
			addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
		} catch (IOException e) {
			e.printStackTrace();
			// Log.e("Exception", e.getMessage());
            // Log.w("Current address location", "Cannot retrieve Address!");
		}
		if (addresses == null)
			return "Could not retrieve the address.";
		
		Address address = addresses.get(0);
		String streetAddress = address.getAddressLine(0);
		String postalCode = address.getPostalCode();
		return streetAddress + ", " + postalCode;

//    	String streetAddress = addresses.get(0).getAddressLine(0);
//    	String cityStateZip = addresses.get(0).getAddressLine(1);
//    	return streetAddress + ", " + cityStateZip;
    }
    
    /**
     * Using ther Geocoder can be time consuming.
     * @author Jerry
     *
     */
    private class MyAsyncTask extends AsyncTask<String, Integer, String>
    {
		@Override
		protected String doInBackground(String... parameter)
		{
			String address = parameter[0];
			if (location != null) {
				address = findStreetAddress(location);
			}
			Intent broadcastIntent = new Intent();
		    broadcastIntent.setAction(MainActivity.LOCATION_QUERY_STR);
		    broadcastIntent.putExtra("address", address);
		    context.sendBroadcast(broadcastIntent);
			
			return null;
		}
    	
		@Override
		protected void onPostExecute(String result) {
			// Synchronized to UI thread.
		}
    }

}
