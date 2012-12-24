Raspbaby
========

An audio-video baby monitor for the raspberry pi, using mjpeg-streamer for mjpeg streaming and darkice/icecast for audio streaming.


Running it
----------

    $ ./setup-pi.sh # Sets up a raspberry pi (assumes Raspbian)


That might take a while, but once done you should be able to see the video stream at http://raspbaby.local:8080 and the audio at http://raspbaby.local:8000/stream.mp3.

There is also the beginning of an Android app in raspbaby-android, with an APK in raspbaby-android/bin/RaspBaby.apk. This is still in very early stages though (for example it doesn't auto-discover the pi yet). 

I wanted to use HTML5 audio/video elements (hence using HTTP streaming for both audio and video), initially. Sadly there is no API to modify buffer sizes, and default ones seem to be extremely large (default Android browser has a latency of more than 10 seconds). The default Android media player API has the same issue, hence relying on a third-party player for now (VLC seems to work well, with a latency of just over a second).
