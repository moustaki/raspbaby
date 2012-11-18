import time, sys
import pymedia.audio.sound as sound
import pymedia.audio.acodec as acodec

def voiceRecorder( secs, name ):
  f= open( name, 'wb' )
  # Minimum set of parameters we need to create Encoder

  cparams= { 'id': acodec.getCodecId( 'mp3' ),
             'bitrate': 128000,
             'sample_rate': 44100,
             'channels': 2 } 
  ac= acodec.Encoder( cparams )
  snd= sound.Input( 44100, 2, sound.AFMT_S16_LE )
  snd.start()
  
  # Loop until recorded position greater than the limit specified

  while snd.getPosition()<= secs:
    s= snd.getData()
    if s and len( s ):
      for fr in ac.encode( s ):
        # We definitely should use mux first, but for

        # simplicity reasons this way it'll work also

        f.write( fr )
    else:
      time.sleep( .003 )
  
  # Stop listening the incoming sound from the microphone or line in

  snd.stop()

# ----------------------------------------------------------------------------------

# Record stereo sound from the line in or microphone and save it as mp3 file

# Specify length and output file name

# http://pymedia.org/

if __name__ == "__main__":
  if len( sys.argv )!= 3:
    print 'Usage: voice_recorder <seconds> <file_name>'
  else:
    voiceRecorder( int( sys.argv[ 1 ] ), sys.argv[ 2 ]  )
