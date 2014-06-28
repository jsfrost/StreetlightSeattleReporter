/**
 * The main activity class. The support version is used so that 
 * the app runs successfully on Gingerbread.
 */
package org.codeforseattle.streetlightseattlereporter;

import java.util.HashMap;
import java.util.List;

import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.annotation.TargetApi;
import android.support.v4.app.FragmentActivity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Patterns;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity 
{
	ToastReceiver mToastReceiver = new ToastReceiver();
	protected final static String TOAST_STR = "org.codeforseattle.streetlightseattlereporter.TOAST_STR";
	AddressReceiver mAddressReceiver = new AddressReceiver();
	protected final static String LOCATION_QUERY_STR = "org.codeforseattle.streetlightseattlereporter.LOCATION_QUERY";
	ResponseReceiver mResponseReceiver = new ResponseReceiver();
	protected final static String SUBMIT_RESPONSE_STR = "org.codeforseattle.streetlightseattlereporter.SUBMIT_RESPONSE";
	LocationManager locationManager = null;
    String provider = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initializeGui();
		
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE); // GPS, Network, others
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        provider = locationManager.getBestProvider(criteria, true);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		registerReceiver(mToastReceiver, new IntentFilter(TOAST_STR));
		registerReceiver(mAddressReceiver, new IntentFilter(LOCATION_QUERY_STR));
		registerReceiver(mResponseReceiver, new IntentFilter(SUBMIT_RESPONSE_STR));
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(mToastReceiver);
		unregisterReceiver(mAddressReceiver);
		unregisterReceiver(mResponseReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void initializeGui()
	{
		EditText nameField = (EditText) findViewById(R.id.name_field);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			String userName = getUserName();
			if (userName != null)
				nameField.setText(userName);
		}
		EditText phoneField = (EditText) findViewById(R.id.phone_field);
		String line1Number = getDefaultPhoneNumber();
		if (line1Number != null)
			phoneField.setText(line1Number);
		
		PackageManager pm = getPackageManager();
		boolean cameraPresent = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
		
		Button scanBtn = (Button) findViewById(R.id.scan_barcode_button);
		if (! cameraPresent) // Do not try bar code scanning without the camera.
			scanBtn.setEnabled(false);
		
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO)
		{
			// The LocationManager.requestSingleUpdate() is available starting in API 9
		    Button locationBtn = (Button) findViewById(R.id.get_address_button);
		    locationBtn.setEnabled(false);
		}
		
		setFocus();
	}
	
	/**
	 * Based on code from http://stackoverflow.com/questions/20360506/get-owner-name-of-an-android-device?lq=1
	 * 
	 * @return the phone's display name
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private String getUserName()
	{
		String userName = null;
		Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
		if (c.getCount() != 0)
		{
			c.moveToFirst();
			userName = c.getString(c.getColumnIndex("display_name"));
		}
		c.close();
		return userName;
	}
	
	/**
	 * There is no reliable way to get the phone number. If the phone number is current and stored in 
	 * line 1 of the SIM card, this method should return it.
	 * 
	 * @return the phone number
	 */
	private String getDefaultPhoneNumber()
	{
		TelephonyManager mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String line1Number = mTelephonyMgr.getLine1Number();
		if (line1Number != null && line1Number.startsWith("1") && line1Number.length() == 11)
		{
			line1Number = line1Number.substring(1);
			String formattedNumber = PhoneNumberUtils.formatNumber(line1Number);
			return formattedNumber;
		}
		return line1Number;
	}
	
	/**
	 * Respond to the button click and scan in the bar code.
	 * 
	 * @param view
	 */
	public void onScanBarcodeButtonClick(View view)
    {
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;
      
        if (!isIntentSafe)
        {
        	String title = "Missing ZXing Barcode Scanner";
        	String message = "To scan a barcode, please install the ZXing Barcode Scaner app from Google Playstore.";
        	boolean isHtml = false;
        	boolean displayIntent = true;
        	displayDialog(title, message, isHtml, displayIntent);

            // Check again for the necessary scanner.
            activities = packageManager.queryIntentActivities(intent, 0);
            isIntentSafe = activities.size() > 0;
            if (! isIntentSafe)
            	return;
        }
        intent.putExtra("SCAN_MODE", "CODE_39"); // Seattle city light poles use Code 39 style bar codes.
      
        try {
    	    startActivityForResult(intent, 0);   // Start the Barcode Scanner.
        }
        catch (ActivityNotFoundException ex)
        {
        	String title = "Caught ActivityNotFoundException";
        	String message = "To scan a barcode, please install the ZXing Barcode Scaner app from Google Playstore.";
        	boolean isHtml = false;
        	boolean displayIntent = true;
        	displayDialog(title, message, isHtml, displayIntent);
        }
    }
	
	/**
	 * Respond to the button click and attempt to retrieve and display the user's current location.
	 * 
	 * @param view
	 */
	public void onGetAddressButtonClick(View view)
    {
		MyLocationListener myLocationListener = new MyLocationListener(this, locationManager);
		Looper looper = null; // Update will happen on main thread.
    	locationManager.requestSingleUpdate(provider, myLocationListener, looper);
    	
		EditText poleAddress = (EditText) findViewById(R.id.address_field);
		poleAddress.setText("Locating . . .");
    }
	
	/**
	 * After performing the scan, this code places the scan results in the pole number field. Status results, 
	 * including the type of bar code found, could be displayed if the status field were put back into the GUI.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		if (requestCode == 0)
		{
			// TextView tvStatus = (TextView) findViewById(R.id.tvStatus);
			TextView tvResult = (TextView) findViewById(R.id.pole_number_field);
	      
			if (resultCode == RESULT_OK)
			{
				// tvStatus.setText(intent.getStringExtra("SCAN_RESULT_FORMAT"));
				tvResult.setText(intent.getStringExtra("SCAN_RESULT"));
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				// tvStatus.setText("Press a button to start a scan.");
				tvResult.setText("Scan cancelled.");
			}
		}
	}
	
	/**
	 * Respond to the button click and clear the form.
	 * 
	 * @param view
	 */
	public void onClearFormButtonClick(View view)
	{
		// Ensure that the keyboard disappears when the button is pressed.
		InputMethodManager inputManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
		inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		
		boolean clearEmailAddress = true;
		clearForm(clearEmailAddress);
		
		Toast.makeText(MainActivity.this, "Form cleared.", Toast.LENGTH_SHORT).show();
	}
	
	protected void clearForm(boolean clearEmailAddress)
	{
		Resources resources = getResources();
		clearField(R.id.extension_field, resources.getString(R.string.extension_message));
		clearField(R.id.pole_number_field, resources.getString(R.string.pole_number_message));
		clearField(R.id.address_field, resources.getString(R.string.address_message));
		((Spinner) findViewById(R.id.spinner_problem_type)).setSelection(0);
		clearField(R.id.problem_description_field, resources.getString(R.string.comments_message));
		if (clearEmailAddress)
			clearField(R.id.email_field, resources.getString(R.string.email_message));
		
		setFocus();
	}
	
	private void clearField(int id, String hint)
	{
	    EditText field = (EditText)findViewById(id);
	    field.setText("");
	    field.setHint(hint);
	}
	
	private void setFocus()
	{
		EditText nameField  = (EditText) findViewById(R.id.name_field);
		EditText phoneField = (EditText) findViewById(R.id.phone_field);
		EditText emailField = (EditText) findViewById(R.id.email_field);
		
		if (nameField.getText().toString().trim().equals(""))
			nameField.requestFocus();
		else if (phoneField.getText().toString().trim().equals(""))
			phoneField.requestFocus();
		else if (emailField.getText().toString().trim().equals(""))
			emailField.requestFocus();
		else
			((EditText) findViewById(R.id.pole_number_field)).requestFocus();
	}
	
	/**
	 * Get the text from the fields on the form and put it into a string array.
	 * @param view
	 */
	public void onSubmitTroubleReportButtonClick(View view)
	{
		// ensure that the keyboard disappears when the button is pressed.
		InputMethodManager inputManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE); 
		inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); 
		
		boolean fieldsValid = validateFields();
		if (! fieldsValid)
			return;

		HashMap<String, String> hashMap = new HashMap<String, String>();
		hashMap.put("LastName", ((EditText) findViewById(R.id.name_field)).getText().toString().trim());
		hashMap.put("Phone",((EditText) findViewById(R.id.phone_field)).getText().toString().trim());
		hashMap.put("PhoneExtension", ((EditText) findViewById(R.id.extension_field)).getText().toString().trim());
		hashMap.put("Email", ((EditText) findViewById(R.id.email_field)).getText().toString().trim());
		hashMap.put("PoleNumber", ((EditText) findViewById(R.id.pole_number_field)).getText().toString().trim());
		hashMap.put("StreetNumber", ((EditText) findViewById(R.id.address_field)).getText().toString().trim());
		hashMap.put("ProblemType", ((Spinner) findViewById(R.id.spinner_problem_type)).getSelectedItem().toString());
		hashMap.put("ProblemDescription", ((EditText) findViewById(R.id.problem_description_field)).getText().toString().trim());
		hashMap.put("SubmitForm", getString(R.string.submit_button));
		
		SubmitService.performHttpPostRequest(this, hashMap);
	}
	
	protected void displayDialog(String title, String message, boolean isHtml, final boolean displayIntent)
	{
		BasicAlertDialogFragment alertDialog = new BasicAlertDialogFragment();
		Bundle args = new Bundle();
		args.putString("title", title);
		args.putString("message", message);
		args.putBoolean("isHtml", isHtml);
		args.putBoolean("displayIntent", displayIntent);
		alertDialog.setArguments(args);
		alertDialog.show(getSupportFragmentManager(), "alert dialog");
	}
	
	private boolean validateFields()
	{
		String message = "";
		String str = ((EditText) findViewById(R.id.name_field)).getText().toString().trim();
		if (str.equals(""))	{
        	message += "<p>Include your name.</p>";
		}
		str = ((EditText) findViewById(R.id.phone_field)).getText().toString().trim();
		if (str.equals(""))	{
			message += "<p>Include your phone number.</p>";
		}
		str = ((EditText) findViewById(R.id.email_field)).getText().toString().trim();
		if (str.equals(""))	{
			message += "<p>Enter your email address.</p>";
		}
		else {
			boolean isValidEmailAddress = Patterns.EMAIL_ADDRESS.matcher(str).matches();
			if (! isValidEmailAddress)
				message += "<p>Please correct your email address.</p>";
		}
		str = ((EditText) findViewById(R.id.pole_number_field)).getText().toString().trim();
		if (str.equals(""))	{
			message += "<p>Either scan or enter the 7-digit lamppost identifier.</p>";
		}
		str = ((EditText) findViewById(R.id.address_field)).getText().toString().trim();
		if (str.equals(""))	{
			message += "<p>Note the pole's location.</p>";
		}
		if (message.equals(""))	{
			return true;
		}
		String title = "";
		boolean isHtml = true;
    	boolean displayIntent = false;
    	displayDialog(title, "<html><body><b>Before submitting your report, please fix these issues:</b>" + 
    	    message + "</body></html>", isHtml, displayIntent);
    	return false;
	}
	
	public class ResponseReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive (Context context, Intent intent)
		{
			String message = intent.getExtras().getString("message");
			int statusCode = intent.getExtras().getInt("statusCode");
			boolean receivedExpectedResponse = intent.getExtras().getBoolean("receivedExpectedResponse", true);
			
        	String title = "Http Response Code " + statusCode;
        	if (receivedExpectedResponse)
	        {
        		title = "Thank You for the Report";
	        }
        	boolean isHtml = true;
        	boolean displayIntent = false;
        	displayDialog(title, message, isHtml, displayIntent);
        	
        	if (receivedExpectedResponse)
	        {
        		boolean clearEmailAddress = false;
        		clearForm(clearEmailAddress);
	        }
		}
	}
	
	public class AddressReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive (Context context, Intent intent)
		{
			String address = intent.getExtras().getString("address");
			
			EditText poleAddress = (EditText) findViewById(R.id.address_field);
			poleAddress.setText(address);
		}
	}
	
	public class ToastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive (Context context, Intent intent)
		{
			String toastTest = intent.getExtras().getString("toast");
			
			Toast.makeText(MainActivity.this, toastTest, Toast.LENGTH_SHORT).show();
		}
	}

}
