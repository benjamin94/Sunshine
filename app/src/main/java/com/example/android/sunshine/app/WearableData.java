package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.Date;

/**
 * Created by benjamin.lize on 08/06/2016.
 */
public class WearableData extends BroadcastReceiver implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    public static String LOCAL_INTENT_FILTER_NAME = "WEARABLE_DATA_READY";
    public static String HIGH_TEMP = "HIGH_TEMPERATURE";
    public static String LOW_TEMP = "LOW_TEMPERATURE";
    public static String IMAGE = "IMAGE";

    GoogleApiClient mGoogleClient;
    Context mContext;
    Bundle mBundle;

    @Override
    public void onReceive(Context context, Intent intent) {
        mBundle = intent.getExtras();

        mContext = context;
        // Build a new GoogleApiClient for the the Wearable API
        mGoogleClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleClient.connect();


    }

    public static void sendBroadcast(Context context, Bundle data) {
        Intent i = new Intent(LOCAL_INTENT_FILTER_NAME);
        i.putExtras(data);
        context.sendBroadcast(i);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        double high = mBundle.getDouble(HIGH_TEMP);
        double low = mBundle.getDouble(LOW_TEMP);
        int imageResourceId = mBundle.getInt(IMAGE);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), imageResourceId);
        Asset asset;
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.art_clear);
        }
        asset = createAssetFromBitmap(bitmap);

        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        dataMap.putLong("time", new Date().getTime());
        dataMap.putDouble(HIGH_TEMP,high);
        dataMap.putDouble(LOW_TEMP, low);
        dataMap.putAsset(IMAGE,asset);
        //Requires a new thread to avoid blocking the UI
        Toast.makeText(mContext, "sending!!!", Toast.LENGTH_SHORT).show();
        new SendToDataLayerThread("/wearable_data", dataMap).start();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            // Construct a DataRequest and send over the data layer
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.getDataMap().putAll(dataMap);
            PutDataRequest request = putDMR.asPutDataRequest();
            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(mGoogleClient, request).await();
            if (result.getStatus().isSuccess()) {
                Log.v("myTag", "DataMap: " + dataMap + " sent successfully to data layer ");
            } else {
                // Log an error
                Log.v("myTag", "ERROR: failed to send DataMap to data layer");
            }
            mGoogleClient.disconnect();

        }
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

}
