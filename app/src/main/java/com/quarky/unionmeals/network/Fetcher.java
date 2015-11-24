package com.quarky.unionmeals.network;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.quarky.unionmeals.MealWidget;

//import org.apache.httpcomponents.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Martin Melka (martin.melka@gmail.com)
 * 10. 1. 2015 15:13
 */
public class Fetcher {

    private final int TIMEOUT = 9000;

    Context context;
    int appWidgetId;

    public Fetcher(Context context, int appWidgetId) {
        this.context = context;
        this.appWidgetId = appWidgetId;
    }

    public void getData(String loginString, String passwordString, MealWidget callback) throws Exception {
        Log.i("Fetcher#getData", loginString + " / " + passwordString);
        new DownloadTask(context, appWidgetId).execute(new DownloadTaskParams(loginString, passwordString, callback));
    }

    class DownloadTaskParams {
        public String login;
        public String password;
        public MealWidget callback;

        DownloadTaskParams(String login, String password, MealWidget callback) {
            this.login = login;
            this.password = password;
            this.callback = callback;
        }
    }

    class DownloadTask extends AsyncTask<DownloadTaskParams, Void, FetcherResult> {
        DownloadTaskParams params;
        Context context;
        int appWidgetId;

        DownloadTask(Context context, int appWidgetId) {
            this.context = context;
            this.appWidgetId = appWidgetId;
        }

        @Override
        protected FetcherResult doInBackground(DownloadTaskParams... params) {
            this.params = params[0];

            double balance;
            int swipes;
            String DOMAIN = "https://unioncollege.managemyid.com";

            try {
                // Prepare and Execute http request
                Map<String, String> map = new LinkedHashMap<>();
                map.put("LoginID", params[0].login);
                map.put("LoginPWD", params[0].password);
                map.put("LoginSubmit", "Login");

                Log.i("doInBackground", "Connection #1 initiated");
                Document doc = Jsoup.connect(DOMAIN + "/reference.dca?cdx=login")
                        .data(map)
                        .followRedirects(true)
                        .timeout(TIMEOUT)
                        .post();
                Log.i("doInBackground", "Connection #1 completed");

                // Check if valid login
                boolean loginOk = true;
                Elements err = doc.select(".Error");
                if (!err.isEmpty()) {
                    loginOk = false;
                }

                if (!loginOk) {
                    Log.w("doInBackground", "Wrong login!");

                    return new FetcherResult(FetcherResult.WRONG_LOGIN, FetcherResult.WRONG_LOGIN, doc.html());
                }

                // Parse Refresh meta value
                Element metalink = doc.select("meta[http-equiv=\"REFRESH\"]").first();
                String link = metalink.attr("content");

                // Create new url from the refresh meta info
                String newUrl = DOMAIN + link.substring(7);

                // Another call for the refresh url
                Log.i("doInBackground", "Connection #2 initiated");
                Document doc2 = Jsoup
                        .connect(newUrl)
                        .timeout(TIMEOUT)
                        .post();
                Log.i("doInBackground", "Connection #2 completed");

                Elements balanceElm = doc2.select(".data tr:eq(2) td:eq(1)");
                Elements swipesElm = doc2.select(".data tr:eq(1) td:eq(1)");
                Elements swipes2 = doc2.select(".data tr:eq(0) td:eq(1)");

                String balanceStr = balanceElm.text().substring(1);
                balance = Double.parseDouble(balanceStr);
                swipes = Integer.parseInt(swipesElm.text()) + Integer.parseInt(swipes2.text());

            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e("Fetcher", e.toString());
                return new FetcherResult(FetcherResult.NO_CONNECTION, FetcherResult.NO_CONNECTION);
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                Log.e("Fetcher", e.toString());
                return new FetcherResult(FetcherResult.NO_CONNECTION, FetcherResult.NO_CONNECTION);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Fetcher", e.toString());
                return new FetcherResult(FetcherResult.IOEXCEPTION, FetcherResult.IOEXCEPTION);
            }

            return new FetcherResult(swipes, balance);
        }

        @Override
        protected void onPostExecute(FetcherResult result) {
            params.callback.showValues(context, result, appWidgetId);
        }
    }
}

