package org.moustaki.raspbaby;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity
{
    private static final String TAG = "MjpegActivity";
    android.os.Handler handler = new android.os.Handler();

    private MjpegView mv;
    private Button playAudioButton;
    
	MulticastLock lock;
    JmDNS jmdns;
    ServiceListener listener;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mv = (MjpegView) findViewById(R.id.mjpeg_view);
        
        Thread t = new Thread() {
        	@Override
        	public void run()
        	{
                setUpDiscovery();
                startDiscovery("_workstation._tcp.local.");
        	}
        };
        t.start();
    }
    
    private void setUpDiscovery() 
	{
    	Log.d(TAG, "Setup of discovery");
		WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		lock = wifi.createMulticastLock("HeeereDnssdLock");
	    lock.setReferenceCounted(true);
	    lock.acquire();
	}
    
    protected void onDestroy() {
        if (lock != null) lock.release();
    }
    
    public void startDiscovery(String type)
	{
    	
		try {
			Log.d(TAG, "Starting discovery");
			String ip = getIpAddr();
			Log.d(TAG, "IP of Android device: " + ip);
			jmdns = JmDNS.create(InetAddress.getByName(ip));
			jmdns.addServiceListener(type, listener = new ServiceListener() {
		        public void serviceResolved(ServiceEvent ev) {
		            Log.d(TAG, "Service resolved: "
		                     + ev.getInfo().getQualifiedName()
		                     + " port:" + ev.getInfo().getPort());
		            Log.d(TAG, ev.getInfo().getName());
		        	if (ev.getInfo().getName().startsWith("raspbaby")) {
		        		String raspIp = ev.getInfo().getHostAddresses()[0];
		        		Log.d(TAG, "IP of Raspbaby device: " + raspIp);
		        		handleVideo("http://" + raspIp + ":8080?action=stream");
		        		handleAudio("http://" + raspIp + ":8000/stream.mp3");
		        	}
		        }
		        public void serviceRemoved(ServiceEvent ev) {
		            Log.d(TAG, "Service removed: " + ev.getName());
		        }
		        public void serviceAdded(ServiceEvent event) {
		            // Required to force serviceResolved to be called again
		            // (after the first search)
		            jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
		        }
		    });
		} catch (IOException e) {
			Log.e(TAG, "Discovery failed for " + type);
		}
	}
    
    public String getIpAddr() {
 	    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    	int ip = wifiInfo.getIpAddress();
    	String ipString = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    	return ipString;
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