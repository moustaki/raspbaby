#!/bin/bash

SSH_PI=pi@raspberrypi

echo "Installing dependencies on pi..."
ssh $SSH_PI "sudo apt-get update && sudo apt-get install graphicsmagick-imagemagick-compat darkice icecast2 screen libmp3lame-dev avahi-daemon libasound2-dev wpasupplicant && mkdir raspbaby"
ssh $SSH_PI "sudo apt-get build-dep darkice"
echo "Copying code to pi..."
scp -rd upstart/ darkice/ mjpg-streamer/ darkice-conf/ stream-audio.sh stream-video.sh rc.local $SSH_PI:raspbaby/ 
echo "Compiling mjpg-streamer..."
ssh $SSH_PI "cd raspbaby/mjpg-streamer && make"
echo "Compling darkice..."
ssh $SSH_PI "cd raspbaby/darkice && ./configure --prefix=/usr --sysconfdir=/usr/share/doc/darkice/examples --with-vorbis-prefix=/usr/lib/arm-linux-gnueabihf/ --with-jack-prefix=/usr/lib/arm-linux-gnueabihf/ --with-alsa-prefix=/usr/lib/arm-linux-gnueabihf/ --with-faac-prefix=/usr/lib/arm-linux-gnueabihf/ --with-aacplus-prefix=/usr/lib/arm-linux-gnueabihf/ --with-samplerate-prefix=/usr/lib/arm-linux-gnueabihf/ --with-lame-prefix=/usr/lib/arm-linux-gnueabihf/ CFLAGS='-march=armv6 -mfpu=vfp -mfloat-abi=hard' && make && sudo make install"
echo "Setting up icecast2..."
ssh $SSH_PI "cd raspbaby && sudo cp darkice-conf/icecast-default /etc/default/icecast2 && sudo cp darkice-conf/icecast.xml /etc/icecast2/icecast.xml && sudo /etc/init.d/icecast2 restart"
echo "Bumping capture volume up..."
ssh $SSH_PI "amixer -c 1 set Mic 50%"
echo "Updating rc.local..."
ssh $SSH_PI "sudo cp raspbaby/rc.local /etc/rc.local"
echo "Updating hostname..."
ssh $SSH_PI "sudo sh -c 'echo raspbaby > /etc/hostname'"
echo "Rebooting pi..."
ssh $SSH_PI "sudo reboot"
