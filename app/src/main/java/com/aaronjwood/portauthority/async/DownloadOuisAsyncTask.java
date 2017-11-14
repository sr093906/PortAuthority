package com.aaronjwood.portauthority.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import com.aaronjwood.portauthority.R;
import com.aaronjwood.portauthority.activity.MainActivity;
import com.aaronjwood.portauthority.db.Database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadOuisAsyncTask extends AsyncTask<Void, String, Void> {

    private static final String[] OUI_SERVICES = {"http://standards-oui.ieee.org/oui36/oui36.csv",
            "http://standards-oui.ieee.org/oui28/mam.csv", "http://standards-oui.ieee.org/oui/oui.csv"};
    private WeakReference<Context> context;
    private Database db;
    private ProgressDialog dialog;

    public DownloadOuisAsyncTask(Database db, Context context) {
        this.db = db;
        this.context = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
        final Context ctx = context.get();
        final DownloadOuisAsyncTask task = this;
        if (ctx != null) {
            dialog = new ProgressDialog(ctx, R.style.DialogTheme);
            dialog.setMessage(ctx.getResources().getString(R.string.downloadingOuis));
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    task.cancel(true);
                }
            });
            dialog.show();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        BufferedReader in = null;
        HttpURLConnection connection = null;
        db.clearOuis();
        try {
            for (String service : OUI_SERVICES) {
                URL url = new URL(service);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    publishProgress(connection.getResponseCode() + " " + connection.getResponseMessage());

                    return null;
                }

                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                in.readLine(); // Skip headers.
                String line;
                db.beginTransaction();
                while ((line = in.readLine()) != null) {
                    if (isCancelled()) {
                        return null;
                    }

                    String[] data = line.split(",");
                    if (data.length != 4) {
                        continue; // Format is registry, assignment, name, address.
                    }

                    String mac = data[1];
                    String vendor = data[2];

                    if (db.insertOui(mac, vendor) == -1) {
                        publishProgress("Failed to insert MAC " + mac + " into the database. " +
                                "Please run this operation again");

                        return null;
                    }
                }
                db.setTransactionSuccessful().endTransaction();
            }
        } catch (IOException e) {
            publishProgress(e.toString());
        } finally {
            db.endTransaction();
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        Context ctx = context.get();
        if (ctx != null) {
            Toast.makeText(ctx, progress[0], Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        Context ctx = context.get();
        if (ctx != null) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            MainActivity activity = (MainActivity) ctx;
            activity.setupMac();
        }
    }

}
