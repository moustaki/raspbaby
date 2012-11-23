package org.moustaki.raspbaby;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class MainActivity extends Activity {
    private static final String TAG = "MjpegActivity";
    String mjpeg_url = "http://192.168.1.76:8080?action=stream";
    String mp3_url = "http://192.168.1.76:8000/stream.mp3";

    private MjpegView mv;
    private MediaPlayer mp;
    private Button playAudioButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_NO_TITLE); 
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        //WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mv = (MjpegView) findViewById(R.id.mjpeg_view);
        
        playAudioButton = (Button) findViewById(R.id.play_audio);
        playAudioButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse(mp3_url), "audio/mpeg");
				startActivity(Intent.createChooser(intent, "Listen with:"));
			}
		});
        //playAudio();
        //mv = new MjpegView(this);
        //setContentView(mv);
        new DoRead().execute(mjpeg_url);
    }
    
    public void playAudio()
    {
    	mp = new MediaPlayer();
    	try {
	    	mp.setDataSource(mp3_url);
	    	mp.prepare();
	    	mp.start();
	    	mp.setVolume(100, 100);
	    	mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					Log.e(TAG, "Error playing audio " + what + " " + extra);
					mp.start();
					return false;
				}
			});
    	} catch (IOException e) {
    		System.err.println("Error loading audio");
    	}
    }

    public void onPause() {
        super.onPause();
        mv.stopPlayback();
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