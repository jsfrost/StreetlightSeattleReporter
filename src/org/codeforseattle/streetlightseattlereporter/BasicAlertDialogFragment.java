/**
 * A class to wrap an alert dialog in a supported dialog fragment so that the dialog remains present 
 * if the view is redrawn, as it is when the phone is rotated. The support version is used so that 
 * the app runs successfully on Gingerbread.
 */
package org.codeforseattle.streetlightseattlereporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;

public class BasicAlertDialogFragment extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
    	Bundle args = this.getArguments();
    	String title = args.getString("title");
    	if (title == null)
    		title = "";
    	String message = args.getString("message");
    	if (message == null)
    		message = "";
    	Boolean isHtml = args.getBoolean("isHtml", true);
    	final Boolean displayIntent = args.getBoolean("displayIntent", false);
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	if (isHtml)
    		builder = builder.setMessage(Html.fromHtml(message));
    	else
    		builder = builder.setMessage(message);
    	builder.setTitle(title)
    	    .setPositiveButton("OK",  new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                	if (displayIntent)
                	{
                		// Code here executes once the dialog closes.
	    	            Intent zxingIntent = new Intent(Intent.ACTION_VIEW);
	    	            zxingIntent.setData(Uri.parse("market://details?id=com.google.zxing.client.android"));
	    	            startActivity(zxingIntent);
                	}
                }
            });
    	return builder.create();
    }
}