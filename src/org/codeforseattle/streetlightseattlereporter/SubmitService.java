package org.codeforseattle.streetlightseattlereporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.common.base.StringUtil;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
// import android.util.Log;

/**
 * An IntentService subclass for handling asynchronous task requests in a service on a separate handler thread.
 */
public class SubmitService extends IntentService 
{
	/**
	 * Starts the service to perform the Post request with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 * @throws InterruptedException 
	 * 
	 * @see IntentService
	 */
	public static void performHttpPostRequest(Context context, HashMap<String, String> hashMap)
	{
		Intent intent = new Intent(context, SubmitService.class);
		intent.putExtra("hashMap", hashMap);
		context.startService(intent);
	}

	public SubmitService() {
		super("SubmitService");
	}

	/**
	 * The fields entered on the form arrive here in the hashMap, where they are turned into the name-value pairs in the body of the Post request.
	 */
	@Override
	protected void onHandleIntent(Intent intent) 
	{
		if (intent == null)
			return;
		
		// Retries happen at increasingly long, powers-of-2-second intervals, up to an hour.
		for (int interval = 2; interval <= 12; interval++)
		{
			if (isOnline()) {
				break;
			}
			try {
				int numMilliSec = (int) (1000 * Math.pow(2, interval));
				String message = "Connection unavailable. Sleeping for "  + numMilliSec/1000 + " sec.";
				Intent toastIntent = new Intent();
				toastIntent.setAction(MainActivity.TOAST_STR);
				toastIntent.putExtra("toast", message);
				sendBroadcast(toastIntent);
				Thread.sleep(numMilliSec);
			}
			catch (InterruptedException xcpt) {
				// Log.e("SubmitService.onHandleIntent", xcpt.getMessage());
			}
		}
		
    	/* Request body:
         * LastName=John+Doe&Phone=206-555-5555&PhoneExtension=&Email=johndoe%40yahoo.com&PoleNumber=0000000&StreetNumber=just+a
         * +test.+please+ignore+this.&ProblemType=Unknown&ProblemDescription=Please+ignore+this+request.+Sorry+to+bother
         * +you.&SubmitForm=Submit+Trouble+Report
    	 */
	    // Create a new HttpClient and Post Header
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpPost httpPost = new HttpPost("http://www.seattle.gov/light/streetlight/sl_handler.asp");
//	    HttpPost httpPost = new HttpPost("http://www.example.com");
	    Intent broadcastIntent = new Intent();
	    broadcastIntent.setAction(MainActivity.SUBMIT_RESPONSE_STR);
		
		@SuppressWarnings("unchecked")
		HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra("hashMap");
		// The values the user entered in the activity were put in a hashmap that was included with the intent. 
		// Here we work with those values as these parameters.
		List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		for (HashMap.Entry<String, String> entry : hashMap.entrySet()) {
		    parameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
	    try {
	    	// Add the name, phone number, etc. fields to the request body.
	    	UrlEncodedFormEntity entity = new UrlEncodedFormEntity(parameters);
			httpPost.setEntity(entity);
	        
		    httpPost.addHeader("Host", "www.seattle.gov");
		    httpPost.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		    httpPost.addHeader("Origin", "http://www.seattle.gov");
		    httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36");
		    httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		    httpPost.addHeader("Referer", "http://www.seattle.gov/light/streetlight/form.asp");
		    httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		    httpPost.addHeader("Connection", "keep-alive");

	        // Execute HTTP Post Request
	        HttpResponse response = httpClient.execute(httpPost);
	        int statusCode = response.getStatusLine().getStatusCode();
	        broadcastIntent.putExtra("statusCode", statusCode);
	        String body = "", httpResponseStr = "", message = "<html><body>";
	        
	        HttpEntity httpEntity = response.getEntity();
	        BufferedReader rd = new BufferedReader(new InputStreamReader(httpEntity.getContent()));
	        while ((body = rd.readLine()) != null) 
			{
	        	httpResponseStr += body;
			}
	        if (statusCode != 200)
	        {
	        	broadcastIntent.putExtra("message", httpResponseStr);
	        	sendBroadcast(broadcastIntent);
	        	return;
	        }
	        boolean receivedExpectedResponse = true;
	        String str = getString(R.string.response_1);
	        
	        // Check for the expected strings in the response.
	        if (httpResponseStr.contains(str))
	        	message += "<h1>" + str + "</h1>";
	        else
	        	receivedExpectedResponse = false;
	        str = getString(R.string.response_2);
	        if (httpResponseStr.contains(str))
	        	message += "<p>" + str + "</p>";
	        else
	        	receivedExpectedResponse = false;
	        str = getString(R.string.response_3);
	        if (httpResponseStr.contains(str))
	        	message += "<p>" + str + getString(R.string.response_3a) + "</p>";
	        else
	        	receivedExpectedResponse = false;
	        str = getString(R.string.response_4);
	        if (httpResponseStr.contains(str))
	        	message += "<p>" + str + "</p>";
	        else
	        	receivedExpectedResponse = false;
	        
	        if (receivedExpectedResponse) {
	        	broadcastIntent.putExtra("message", message);
	        }
	        else {
	        	// If the expected response is not returned, return the response so that the user might figure it out.
	        	broadcastIntent.putExtra("message", httpResponseStr);
	        }
	        broadcastIntent.putExtra("receivedExpectedResponse", receivedExpectedResponse);
	        sendBroadcast(broadcastIntent);

	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
	    try {
			createRecordInSpreadsheet(parameters); // Add a row to the google doc spreadsheet.
		} catch (AuthenticationException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}
    }

	/**
	 * Recording a copy of the information somebody used the Streetlight Seattle Reporter app to send to Seattle City Light. 
	 * Google Docs approach inspired by: https://developers.google.com/google-apps/spreadsheets/#adding_a_list_row
	 * Spreadsheet viewable at: https://docs.google.com/spreadsheet/ccc?key=0AvDYc0olGEwRdE56dTlKMVVZdGYxQkc5eGJEQzJLNkE&usp=drive_web#gid=0
	 * 
	 * @param parameters names and values from the activity; names are column headers in the spreadsheet. 
	 * @throws AuthenticationException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ServiceException
	 */
	private void createRecordInSpreadsheet(List<NameValuePair> parameters)
			throws AuthenticationException, MalformedURLException, IOException, ServiceException
	{
		String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
		parameters.add(new BasicNameValuePair("Date", timeStamp));
		//parameters.remove(object)
		
		SpreadsheetService service = new SpreadsheetService("CodeForSeattle-StreetlightSeattleReporter-v1");

		// Authorize the service object for a specific user (see other sections)
		final String user = "streetlightseattlereporter@gmail.com";
		final String pword = StringUtil.repeat(getString(R.string.app_initials), 3) + "333";
		service.setUserCredentials(user, pword);

		// Define the URL to request.  This should never change.
		URL SPREADSHEET_FEED_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");

		// Make a request to the API and get all spreadsheets.
		SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
		List<SpreadsheetEntry> spreadsheets = feed.getEntries();

		if (spreadsheets.size() == 0) {
			// There were no spreadsheets, act accordingly.
			return;
		}
		SpreadsheetEntry spreadsheet = null;
		for (SpreadsheetEntry s : spreadsheets)
		{
			String title = s.getTitle().getPlainText();
			if (title.equals("StreetlightSeattleReporter"))
			{
				spreadsheet = s;
				break;
			}
			if (spreadsheet == null) // Backup plan.
				spreadsheet = spreadsheets.get(0);
		}

		// Get the first worksheet of the first spreadsheet.
		WorksheetFeed worksheetFeed = service.getFeed(spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
		List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
		WorksheetEntry worksheet = worksheets.get(0);

		// Fetch the list feed of the worksheet.
		URL listFeedUrl = worksheet.getListFeedUrl();
		ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);

		// Create a local representation of the new row.
		ListEntry row = new ListEntry();
		
		// Set the values of the new row.
		for (NameValuePair param : parameters) {
			String key = param.getName();
			// Refrain from storing these 3 fields in the Google Docs spreadsheet.
			if (key.equals("LastName") || key.equals("Phone") || key.equals("Email"))
				continue;
			row.getCustomElements().setValueLocal(key, param.getValue());
		}
		// Send the new row to the API for insertion.
		row = service.insert(listFeedUrl, row);
	}
	
	/**
	 * http://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-timeouts
	 * 
	 * @return
	 */
	private boolean isOnline()
	{
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
	    if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}

}
