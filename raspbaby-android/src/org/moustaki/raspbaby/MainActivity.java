package org.moustaki.raspbaby;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

@TargetApi(16)
public class MainActivity extends Activity
{
    private static final String TAG = "MjpegActivity";
    String mjpeg_url = "http://192.168.1.76:8080?action=stream";
    String mp3_url = "http://192.168.1.76:8000/stream.mp3";

    private MjpegView mv;
    private Button playAudioButton;
    
    private NsdManager mNsdManager = null;
    private NsdManager.ResolveListener mResolveListener = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mv = (MjpegView) findViewById(R.id.mjpeg_view);
        //handleAudio(mp3_url);
        //handleVideo(mjpeg_url);
        
        discoverRaspbaby();
        
    }

    public void handleAudio(final String mp3_url)
    {
    	playAudioButton = (Button) findViewById(R.id.play_audio);
        playAudioButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse(mp3_url), "audio/mpeg");
				startActivity(Intent.createChooser(intent, "Listen with:"));
			}
		});
    }

    public void handleVideo(String mjpeg_url)
    {
    	Log.d(TAG, "Starting video: "  + mjpeg_url);
    	new DoRead().execute(mjpeg_url);
    }
    
    public void discoverRaspbaby()
    {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed " + errorCode + " " + serviceInfo);
                mNsdManager.resolveService(serviceInfo, mResolveListener);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
                int port = serviceInfo.getPort();
                InetAddress host = serviceInfo.getHost();
                String serviceType = serviceInfo.getServiceType();
                Log.d(TAG, "Type: " + serviceType);
                if (serviceType.equals("._mjpegstreamer._tcp")) {
                    Log.d(TAG, "Loading http:/"+host+":"+port+"?action=stream");
                	handleVideo("http:/"+host+":"+port+"?action=stream");
                } else if (serviceType.equals("._icecast._tcp")) {
                	Log.d(TAG, "Loading http:/"+host+":"+port+"/stream.mp3");
                	handleAudio("http:/"+host+":"+port+"/stream.mp3");
                } else {
                	Log.e(TAG, "Unknown service");
                }
            }
        };
    	
    	mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        // Instantiate a new DiscoveryListener
    	NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                mNsdManager.resolveService(service, mResolveListener);
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            } 

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };

        mNsdManager.discoverServices("_mjpegstreamer._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        mNsdManager.discoverServices("_icecast._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();     
            Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if(res.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());  
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(true);
        }
    }
}