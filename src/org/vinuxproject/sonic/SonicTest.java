// This file was written by me, Bill Cox in 2011.
// I place this file into the public domain.  Feel free to copy from it.
// Note, however that libsonic, which this application links to,
// is licensed under LGPL.  You can link to it in your commercial application,
// but any changes you make to sonic.c or sonic.h need to be shared under
// the LGPL license.

package org.vinuxproject.sonic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.badlogic.gdx.audio.io.Mpg123Decoder;
import com.badlogic.gdx.files.FileHandle;

public class
        SonicTest extends Activity {
    private Thread thread;
    private volatile boolean kill = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    private static final int shortToByte(short[] shortArray, byte[] byteArray, int size) {

        int b = 0;
        for (int i = 0; i < size; i++) {
            short s = shortArray[i];
            byteArray[b++] = (byte) s;
            byteArray[b++] = (byte) ((s & 0xFF00) >> 8);
        }
        return size * 2;
    }

    int skipAmount = 0;
    public void skip(View v){
        skipAmount += 290;
    }

    public void play(View view) {

        final Button button = (Button) findViewById(R.id.button);
        final TextView duration = (TextView) findViewById(R.id.txtDuration);
        final TextView position = (TextView) findViewById(R.id.txtPosition);

        if (thread == null) {
            button.setText("Stop");
            thread = new Thread(new Runnable() {
                public void run() {
                    final EditText speedEdit = (EditText) findViewById(R.id.speed);
                    final EditText pitchEdit = (EditText) findViewById(R.id.pitch);
                    final EditText rateEdit = (EditText) findViewById(R.id.rate);
                    final EditText volumeEdit = (EditText) findViewById(R.id.volume);
                    float speed = Float.parseFloat(speedEdit.getText().toString());
                    float pitch = Float.parseFloat(pitchEdit.getText().toString());
                    float rate = Float.parseFloat(rateEdit.getText().toString());
                    float volume = Float.parseFloat(volumeEdit.getText().toString());

                    FileHandle fileHandle = new FileHandle("/sdcard/AudioBooks/Test/Test2.mp3");
                    Mpg123Decoder mpg123Decoder = new Mpg123Decoder(fileHandle);
                    int channels = mpg123Decoder.getChannels();
                    int rate1 = mpg123Decoder.getRate();
                    final float length = mpg123Decoder.getLength();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            duration.setText("Duration: " + length);
                        }
                    });

                    int totalSamples = 0;
//                    totalSamples = mpg123Decoder.skipSamples(rate1 * channels * 5);

                    AndroidAudioDevice device = new AndroidAudioDevice(rate1, channels);
                    Sonic sonic = new Sonic(rate1, channels);
                    short samplesShort[] = new short[4096 / 2];
                    byte samples[] = new byte[4096];
                    byte modifiedSamples[] = new byte[2048];
//                InputStream soundFile = getResources().openRawResource(R.raw.talking);

                    int bytesRead = 0;
                    if (fileHandle.exists()) {
                        sonic.setSpeed(speed);
                        sonic.setPitch(pitch);
                        sonic.setRate(rate);
                        sonic.setVolume(volume);
//                        float volume = sonic.getVolume();
//                        sonic.setVolume(volume*3f);
                        do {


                            if(skipAmount > 0){
                                int amt = skipAmount;
                                skipAmount = 0;
                                totalSamples = mpg123Decoder.skipSamples(totalSamples + (rate1*channels*amt));
                            }

//				        try {
//							bytesRead = soundFile.read(samples, 0, samples.length);
                            bytesRead = mpg123Decoder.readSamples(samples, 0, samples.length);
                            totalSamples += bytesRead / 2;
                            //totalSamples += bytesRead;
//                            bytesRead = shortToByte(samplesShort, samples, shortsRead);
//                        bytesRead = ShortToByte_Twiddle_Method(samplesShort, samples, shortsRead);

//						} catch (IOException e) {
//							e.printStackTrace();
//							return;
//						}
                            if (bytesRead > 0) {
                                sonic.putBytes(samples, bytesRead);
                            } else {
                                sonic.flush();
                            }
                            int available = sonic.availableBytes();
                            if (available > 0) {
                                if (modifiedSamples.length < available) {
                                    modifiedSamples = new byte[available * 2];
                                }
                                sonic.receiveBytes(modifiedSamples, available);
                                device.writeSamples(modifiedSamples, available);
                            }

                            final int currentTime = totalSamples/(rate1 * channels);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    position.setText("Position: " + currentTime);
                                }
                            });
                        } while (bytesRead > 0 && !kill);
                        device.flush();
                        kill = false;
                        thread = null;
                    }
                }
            });
            thread.start();
        } else {
            kill = true;
            button.setText("Play");
        }
    }
}
