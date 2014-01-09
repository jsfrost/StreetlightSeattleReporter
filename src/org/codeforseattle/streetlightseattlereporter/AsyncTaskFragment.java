/**
 * A class to wrap an asynchronous task in a supported fragment so that the background process continues 
 * if the view is redrawn, as it is when the phone is rotated. The support version is used so that 
 * the app runs successfully on Gingerbread.
 */
package org.codeforseattle.streetlightseattlereporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Jerry Frost
 * http://www.vogella.com/articles/AndroidFragments/article.html#headlessfragments
 */
public class AsyncTaskFragment extends Fragment
{
	private MainActivity mainActivity = null;
	private SendHttpRequestTask mTask = null;
	private int statusCode;
	private boolean receivedExpectedResponse;

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
   
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }
   
    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework 
     * will pass us a reference to the newly created Activity after 
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) 
    {
        super.onAttach(activity);
        mainActivity = (MainActivity) activity;
    }
    
    public void performHttpPostRequest(String[] str)
    {
        // Create and execute the background task.
        mTask = new SendHttpRequestTask();
        mTask.execute(str[0], str[1], str[2], str[3], str[4], str[5], str[6], str[7], str[8]);
    }
   
	private class SendHttpRequestTask extends AsyncTask<String, Void, String>
	{	
		  @Override
		  protected void onPreExecute() {
			  statusCode = 0; // reset the status code.
		  }
		  
		/**
		 * Those params are the strings from the fields on the form. They become the value part of the name-value pairs. 
		 * And that becomes the body of the Post request.
		 */
	    @Override
	    protected String doInBackground(String... params)
	    {
	    	/* Request body:
             * LastName=John+Doe&Phone=206-555-5555&PhoneExtension=&Email=johndoe%40yahoo.com&PoleNumber=0000000&StreetNumber=just+a
             * +test.+please+ignore+this.&ProblemType=Unknown&ProblemDescription=Please+ignore+this+request.+Sorry+to+bother
             * +you.&SubmitForm=Submit+Trouble+Report
	    	 */
	    	
		    // Create a new HttpClient and Post Header
		    HttpClient httpClient = new DefaultHttpClient();
		    HttpPost httpPost = new HttpPost("http://www.seattle.gov/light/streetlight/sl_handler.asp");
//		    HttpPost httpPost = new HttpPost("http://www.example.com");

		    try {
		        // Add your data
		        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(9);
		        nameValuePairs.add(new BasicNameValuePair("LastName", params[0]));
		        nameValuePairs.add(new BasicNameValuePair("Phone", params[1]));
		        nameValuePairs.add(new BasicNameValuePair("PhoneExtension", params[2]));
		        nameValuePairs.add(new BasicNameValuePair("Email", params[3]));
		        nameValuePairs.add(new BasicNameValuePair("PoleNumber", params[4]));
		        nameValuePairs.add(new BasicNameValuePair("StreetNumber", params[5]));
		        nameValuePairs.add(new BasicNameValuePair("ProblemType", params[6]));
		        nameValuePairs.add(new BasicNameValuePair("ProblemDescription", params[7]));
		        nameValuePairs.add(new BasicNameValuePair("SubmitForm", params[8]));
		        UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(nameValuePairs);
		        httpPost.setEntity(uefe);
		        
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
		        statusCode = response.getStatusLine().getStatusCode();
		        String body = "", httpResponseStr = "", message = "<html><body>";
		        
//		        if (statusCode != 200)
//		        {
//		        	message = "<html><body><p>With a return status code of " + statusCode + 
//		        			", that report did not go through.</p></body></html>";
//		        	return message;
//		        }
		        
		        HttpEntity httpEntity = response.getEntity();
		        BufferedReader rd = new BufferedReader(new InputStreamReader(httpEntity.getContent()));
		        while ((body = rd.readLine()) != null) 
				{
		        	httpResponseStr += body;
				}
		        if (statusCode != 200)
		        {
		        	return httpResponseStr;
		        }
		        receivedExpectedResponse = true;
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
		        
		        if (receivedExpectedResponse)
		        	return message;
		        else
		        	// If the expected response is not returned, return the response so that the user might figure it out.
		        	return httpResponseStr;
		        
		    } catch (ClientProtocolException e) {
		    	e.printStackTrace();
		    	Log.e("ClientProtocolException", "" + e.getMessage());
		    } catch (IOException e) {
		    	e.printStackTrace();
		    	Log.e("IOException", e.getMessage());
		    } catch (Exception e) {
		    	e.printStackTrace();
		    	Log.e("Exception", e.getMessage());
		    }
		    return null;
	    }
	    
	    /**
	     * After performing the post in the background, this method runs on the main thread.
	     *
	     * @param response the return value from the doInBackground method.
	     */
		@Override
	    protected void onPostExecute(String response)
	    {
        	String title = "Http Response Code " + statusCode;
        	if (receivedExpectedResponse)
	        {
        		title = "Thank You for the Report";
	        }
        	boolean isHtml = true;
        	boolean displayIntent = false;
        	mainActivity.displayDialog(title, response, isHtml, displayIntent);
        	
        	if (receivedExpectedResponse)
	        {
        		boolean clearEmailAddress = false;
        		mainActivity.clearForm(clearEmailAddress);
	        }
        	
	    }
	}
    
}
