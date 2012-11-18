/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package org.moustaki.raspbaby;

import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.*;

public class raspbaby extends DroidGap
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.loadUrl("file:///android_asset/www/index.html");
        playAudio();
    }
    
    public void playAudio()
    {
    	String fileUrl = "http://192.168.1.65:8000/stream.mp3";
    	MediaPlayer mp = new MediaPlayer();
    	try {
    		mp.setDataSource(fileUrl);
    		mp.prepare();
    		mp.start();
    	} catch (IOException e) {
    		System.err.println("Error");
    	}
    }
}

