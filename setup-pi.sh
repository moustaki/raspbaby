#!/bin/bash

echo "Installing dependencies on pi..."
ssh pi@raspberrypi "sudo apt-get install graphicsmagick-imagemagick-compat darkice icecast2 screen libmp3lame-dev && mkdir raspbaby"
ssh pi@raspberrypi "sudo apt-get build-dep darkice"
echo "Copying code to pi..."
scp -rd upstart/ darkice/ mjpg-streamer/ darkice-conf/ stream-audio.sh stream-video.sh pi@raspberrypi:raspbaby/ 
echo "Compiling mjpg-streamer..."
ssh pi@raspberrypi "cd raspbaby/mjpg-streamer && make"
echo "Compling darkice..."
ssh pi@raspberrypi "cd raspbaby/darkice && ./configure --prefix=/usr --sysconfdir=/usr/share/doc/darkice/examples --with-vorbis-prefix=/usr/lib/arm-linux-gnueabihf/ --with-jack-prefix=/usr/lib/arm-linux-gnueabihf/ --with-alsa-prefix=/usr/lib/arm-linux-gnueabihf/ --with-faac-prefix=/usr/lib/arm-linux-gnueabihf/ --with-aacplus-prefix=/usr/lib/arm-linux-gnueabihf/ --with-samplerate-prefix=/usr/lib/arm-linux-gnueabihf/ --with-lame-prefix=/usr/lib/arm-linux-gnueabihf/ CFLAGS='-march=armv6 -mfpu=vfp -mfloat-abi=hard' && make && sudo make install"
echo "Setting up icecast2..."
ssh pi@raspberrypi "cd raspbaby && sudo cp darkice-conf/icecast.xml /etc/icecast2/icecast.xml && sudo /etc/init.d/icecast2 restart"
echo "Starting streaming..."
ssh pi@raspberrypi "screen -d -m -S audio 'cd raspbaby && ./stream-audio.sh' && screen -d -m -S video 'cd raspbaby && ./stream-video.sh'"
