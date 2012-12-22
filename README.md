Raspbaby
========

An audio-video baby monitor for the raspberry pi, using mjpeg-streamer for mjpeg streaming and darkice/icecast for audio streaming.


Running it
----------

    $ ./setup-pi.sh # Sets up a raspberry pi (assumes Raspbian)


That might take a while, but once done you should be able to see the video stream at http://raspbaby.local:8080 and the audio at http://raspbaby.local:8000/stream.mp3.

There is also the beginning of an Android app in raspbaby-android, with an APK in raspbaby-android/bin/RaspBaby.apk. This is still in very early stages though. 
