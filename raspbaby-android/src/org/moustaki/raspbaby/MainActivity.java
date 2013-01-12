package org.moustaki.raspbaby;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends Activity
{
    private static final String TAG = "MjpegActivity";
    android.os.Handler handler = new android.os.Handler();

    private MjpegView mv;
    
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
        super.onDestroy();
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
    	Log.d(TAG, "Starting audio: "  + mp3_url);
    	new DoPlay().execute(mp3_url);
    }

    public void handleVideo(String mjpeg_url)
    {
    	Log.d(TAG, "Starting video: "  + mjpeg_url);
    	new DoRead().execute(mjpeg_url);
    }
    
    public class DoPlay extends AsyncTask<String, Void, Void> {
    	protected Void doInBackground(String... url) {
    		boolean retry = true;
    		while (retry) {
	    		int shortSizeInBytes = Short.SIZE/Byte.SIZE;
	    		Log.d(TAG, "Starting to play audio...");
	            // define the buffer size for audio track
	            Decoder decoder = new Decoder();
	            int bufferSize = (int) decoder.getOutputBlockSize() * shortSizeInBytes;
	            Log.d(TAG, "Buffer size: " + bufferSize);
	            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
	            audioTrack.play();
	            try {
	            	HttpClient client = new DefaultHttpClient();
	            	HttpGet request = new HttpGet();
	    			request.setURI(new URI(url[0]));
	    			HttpResponse response = client.execute(request);
	    			HttpEntity entity = response.getEntity();
	    			InputStream inputStream = new BufferedInputStream(entity.getContent(), 8 * decoder.getOutputBlockSize());
	                Bitstream bitstream = new Bitstream(inputStream);
	                boolean done = false;
	                while (! done) { 
	                	try { 
	                        Header frameHeader = bitstream.readFrame();
		                	SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
		                	short[] pcm = output.getBuffer();
		                	Log.d(TAG, "decoded " + pcm.length + " samples!"); 
		                    audioTrack.write(pcm, 0, output.getBufferLength());
		                    bitstream.closeFrame();
	                	} catch (ArrayIndexOutOfBoundsException e) { 
	                		Log.d(TAG, "Index out of bounds: " + e.getMessage()); 
	                	}
	                }
	                audioTrack.stop();
	                audioTrack.release();
	                bitstream.closeFrame();
	                inputStream.close();
	            } catch (IOException e) { 
	                Log.e(TAG, e.getMessage());
	            } catch (DecoderException e) {
	            	Log.e(TAG, e.getMessage());
	            } catch (BitstreamException e) {
	            	Log.e(TAG, e.getMessage());
	            } catch (URISyntaxException e) {
	            	Log.e(TAG, e.getMessage());
	            	retry = false;
				} catch (NullPointerException e) { 
	        		Log.d(TAG, "Null pointer: " + e.getMessage()); 
	        	}
    		}
			return null;
    	}
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