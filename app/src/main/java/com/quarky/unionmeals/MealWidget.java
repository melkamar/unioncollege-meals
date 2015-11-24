package com.quarky.unionmeals;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.quarky.unionmeals.network.Fetcher;
import com.quarky.unionmeals.network.FetcherResult;

import java.text.DecimalFormat;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link MealWidgetConfigureActivity MealWidgetConfigureActivity}
 */
public class MealWidget extends AppWidgetProvider {

    static final String ACTION_CLICK = "ACTION_CLICK_WIDGET";
    static final String BROADCAST_REFRESH = "com.quarky.unionmeans.refresh";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            MealWidgetConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(BROADCAST_REFRESH)) {
            int appWidgetId = intent.getIntExtra("appWidgetId", -1);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            updateAppWidget(context, manager, appWidgetId, true);
        }
    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId, boolean manualRefresh) {
        Log.w("updateAppWidget", "Beginning");

        CharSequence widgetText = MealWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.meal_widget);
        views.setTextViewText(R.id.loginText, widgetText);

        // Create an Intent to launch ExampleActivity
        Intent intent = new Intent(context, MealWidgetConfigureActivity.class);

        String[] res = MealWidgetConfigureActivity.loadLoginPref(context, appWidgetId);
        Log.w("updateAppWidget", "putting extra appWidgetId: " + appWidgetId + " (key " + AppWidgetManager.EXTRA_APPWIDGET_ID + ")");
        Log.w("updateAppWidget", "putting extra login: " + res[0]);
        Log.w("updateAppWidget", "putting extra password: " + res[1]);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("login", res[0]);
        intent.putExtra("password", res[1]);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_provider_layout);
        views.setOnClickPendingIntent(R.id.parentLayout, pendingIntent);

        Intent refreshIntent = new Intent(context, MealWidget.class);
        refreshIntent.setAction(BROADCAST_REFRESH);
        refreshIntent.putExtra("appWidgetId", appWidgetId);
        views.setOnClickPendingIntent(R.id.refresh_button, PendingIntent.getBroadcast(context, 0, refreshIntent, 0));


        try {
            String[] loginData = MealWidgetConfigureActivity.loadLoginPref(context, appWidgetId);

            Log.w("updateAppWidget#try", "loginData[0]: " + loginData[0]);
            Log.w("updateAppWidget#try", "loginData[1]: " + loginData[1]);

//            if (loginData[0] != null && loginData[1] != null) {
            new Fetcher(context, appWidgetId).getData(loginData[0], loginData[1], this);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.w("updateAppWidget", "Instructing to update...");

        // Only show progressbar when user explicitly initiated refresh
        if (manualRefresh) {
            views.setViewVisibility(R.id.loading_layout, View.VISIBLE);
            views.setViewVisibility(R.id.data_layout, View.GONE);
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {
        updateAppWidget(context, appWidgetManager, appWidgetId, false);
    }

    public void showValues(Context context, FetcherResult result, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.meal_widget);

        if (result.getBalance() == -1 || result.getSwipes() == -1) {
            views.setViewVisibility(R.id.data_layout, View.GONE);
            views.setViewVisibility(R.id.loading_layout, View.VISIBLE);
        } else if (result.getBalance() < -1 && result.getSwipes() < -1) {
            views.setTextViewText(R.id.swipesTextView, "");
            views.setViewVisibility(R.id.data_layout, View.VISIBLE);
            views.setViewVisibility(R.id.loading_layout, View.GONE);
            switch (result.getSwipes()) {
                case FetcherResult.WRONG_LOGIN:
                    views.setTextViewText(R.id.cashTextView, "Wrong login!");

                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{"martin.melka@gmail.com"});
                    i.putExtra(Intent.EXTRA_SUBJECT, "UnionMeals#WRONG_LOGIN");
                    i.putExtra(Intent.EXTRA_TEXT, result.getTxt());
                    try {
                        context.startActivity(Intent.createChooser(i, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case FetcherResult.NO_CONNECTION:
                    views.setTextViewText(R.id.cashTextView, "No connection");
                    break;
                case FetcherResult.IOEXCEPTION:
                    views.setTextViewText(R.id.cashTextView, "Error");
                    break;
            }
        } else {
            Double balance = Double.valueOf(result.getBalance());
            DecimalFormat format = new DecimalFormat("#.0");
            views.setTextViewText(R.id.cashTextView, "$" + format.format(result.getBalance()));
            views.setTextViewText(R.id.swipesTextView, (result.getSwipes()) + "");
            views.setViewVisibility(R.id.data_layout, View.VISIBLE);
            views.setViewVisibility(R.id.loading_layout, View.GONE);
        }


        // Create an Intent to launch ExampleActivity
        Intent intent = new Intent(context, MealWidgetConfigureActivity.class);

        String[] res = MealWidgetConfigureActivity.loadLoginPref(context, appWidgetId);
        Log.w("updateAppWidget", "putting extra appWidgetId: " + appWidgetId + " (key " + AppWidgetManager.EXTRA_APPWIDGET_ID + ")");
        Log.w("updateAppWidget", "putting extra login: " + res[0]);
        Log.w("updateAppWidget", "putting extra password: " + res[1]);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("login", res[0]);
        intent.putExtra("password", res[1]);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Get the layout for the App Widget and attach an on-click listener
        // to the button
        views.setOnClickPendingIntent(R.id.parentLayout, pendingIntent);

        // Instruct the widget manager to update the widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


