package com.quarky.unionmeals;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.quarky.unionmeals.network.Fetcher;
import com.quarky.unionmeals.network.FetcherResult;
import com.quarky.unionmeals.utils.SecurePreferences;


/**
 * The configuration screen for the {@link MealWidget MealWidget} AppWidget.
 */
public class MealWidgetConfigureActivity extends Activity implements View.OnClickListener {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EditText mAppWidgetText;
    private static final String PREFS_NAME = "com.quarky.unionmeals.MealWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    public MealWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mOnClickListener = this;

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.meal_widget_configure);
        mAppWidgetText = (EditText) findViewById(R.id.loginText);
        findViewById(R.id.fab).setOnClickListener(mOnClickListener);

        findViewById(R.id.help_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MealWidgetConfigureActivity.this)
                        .title("How to use Union Meals")
                        .content("You need to be registered at https://unioncollege.managemyid.com. \n\n" +
                                "Enter login information from that site, not your Union info!")
                        .neutralText("OK")
                        .positiveText("Register")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);

                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("https://unioncollege.managemyid.com"));
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        });

        Log.i("onCreate", "Before");

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String loginText = "";
        String passwdText = "";
        if (extras != null) {
            Log.i("onCreate", "extras not null. Trying to get int from key " + AppWidgetManager.EXTRA_APPWIDGET_ID);
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            loginText = extras.getString("login", "");
            passwdText = extras.getString("password", "");

            Log.i("onCreate", "login: " + extras.getString("login", "not set"));
        } else {
            Log.i("onCreate", "extras null");
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.i("onCreate", "invalid_appwidget_id");
            finish();
            return;
        }

//        mAppWidgetText.setText(loadTitlePref(MealWidgetConfigureActivity.this, mAppWidgetId));


        ((EditText) findViewById(R.id.loginText)).setText(loginText);
        ((EditText) findViewById(R.id.passwordText)).setText("");
        if (!passwdText.isEmpty()) {
            ((EditText) findViewById(R.id.passwordText)).setHint("(unchanged)");
        } else {
            ((EditText) findViewById(R.id.passwordText)).setHint("");
        }
    }

    View.OnClickListener mOnClickListener;

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.commit();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return context.getString(R.string.appwidget_text);
        }
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveLoginPref(Context context, int appWidgetId, String login, String password) {
        String key = Settings.Secure.ANDROID_ID;
        if (key == null) {
            key = "alksdhgewitzLHSDLGhersd65g4as6d87as6dfqweflkashdglDKSLFHLQEK";
        }

        SecurePreferences securePreferences = new SecurePreferences(context, PREFS_NAME, key, true);
        securePreferences.put(PREF_PREFIX_KEY + appWidgetId + "-login", login);
        securePreferences.put(PREF_PREFIX_KEY + appWidgetId + "-password", password);

//        --- OLD VERSION ---
//        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
//        prefs.putString(PREF_PREFIX_KEY + appWidgetId + "-login", login);
//        prefs.putString(PREF_PREFIX_KEY + appWidgetId + "-password", password);
//        prefs.commit();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource

    /**
     * Get saved login preferences.
     *
     * @param context
     * @param appWidgetId
     * @return Two-field String array. Element 0 contains login, element 1 the password.
     */
    static String[] loadLoginPref(Context context, int appWidgetId) {
        String key = Settings.Secure.ANDROID_ID;
        if (key == null) {
            key = "alksdhgewitzLHSDLGhersd65g4as6d87as6dfqweflkashdglDKSLFHLQEK";
        }

        SecurePreferences prefs = new SecurePreferences(context, PREFS_NAME, key, true);
        String loginValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId + "-login");
        String passwordValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId + "-password");

        if (loginValue == null){
            loginValue = "";
        }
        if (passwordValue == null){
            passwordValue = "";
        }

//        --- OLD ---
//        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
//        String loginValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId + "-login", "");
//        String passwordValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId + "-password", "");
        return new String[]{loginValue, passwordValue};
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.commit();
    }

    @Override
    public void onClick(View v) {
        final Context context = MealWidgetConfigureActivity.this;

        // When the button is clicked, store the string locally
        String widgetText = mAppWidgetText.getText().toString();
        saveTitlePref(context, mAppWidgetId, widgetText);

        EditText passwdEditText = (EditText) findViewById(R.id.passwordText);
        String passwd;
        if (passwdEditText.getText().toString().isEmpty()) {
            passwd = loadLoginPref(context, mAppWidgetId)[1];
        } else {
            passwd = passwdEditText.getText().toString();
        }

        saveLoginPref(context, mAppWidgetId,
                ((EditText) findViewById(R.id.loginText)).getText().toString().trim(),
                passwd);

        // It is the responsibility of the configuration activity to update the app widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        new MealWidget().updateAppWidget(context, appWidgetManager, mAppWidgetId, true);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}



