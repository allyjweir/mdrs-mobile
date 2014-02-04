package ggow.teamt.mdrs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {
	
	@Override 
    public void onEnabled(Context context) {  
          //Log.v("toggle_widget","Enabled is being called"); 

          AppWidgetManager mgr = AppWidgetManager.getInstance(context); 
          //retrieve a ref to the manager so we can pass a view update 

          Intent i = new Intent(); 
          i.setClassName("ggow.teamt.mdrs", "ggow.teamt.mdrs.RecordingActivity"); 
          PendingIntent myPI = PendingIntent.getService(context, 0, i, 0); 
          //intent to start service 

        // Get the layout for the App Widget 
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout); 

        //attach the click listener for the service start command intent 
        views.setOnClickPendingIntent(R.id.widget_layout, myPI); 

        //define the componenet for self 
        ComponentName comp = new ComponentName(context.getPackageName(), ToggleWidget.class.getName()); 

        //tell the manager to update all instances of the toggle widget with the click listener 
        mgr.updateAppWidget(comp, views); 
} 
}
