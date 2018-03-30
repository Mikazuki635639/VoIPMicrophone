package com.example.wacif.myapplication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import android.content.Intent;
import android.app.Activity;
import android.net.rtp.RtpStream;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Build;
import android.view.*;
import android.widget.Button;
import android.widget.ToggleButton;
import android.widget.EditText;
import android.util.Log;
import static java.lang.Thread.currentThread;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.*;

public class Microphone extends AppCompatActivity implements View.OnTouchListener {

    //public byte[] buffer;
    //public static DatagramSocket socket;
    private final int port=5005;

    AudioRecord recorder;
    //RtpStream recorder;
    volatile boolean status = false;

    private final int sampleRate = 16000 ; // 44100 for music
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone);

        ToggleButton pushToTalkButton = (ToggleButton) findViewById(R.id.btn_Microphone);
        pushToTalkButton.setOnTouchListener(this);

        Button wifiSearchButton = (Button) findViewById(R.id.btn_RoomSelection);
        //wifiSearchButton.setOnClickListener(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            status = true;
            Log.d("VS", "Pressed");
            v.performClick();
            startStreaming();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            status = false;
            Log.d("VS", "Released");
        }

        return false;
    }

    public void startStreaming() {
        final EditText destinationIP = (EditText) findViewById(R.id.txt_UserName);
        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    DatagramSocket socket = new DatagramSocket();

                    byte[] buffer = new byte[minBufSize];
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName(destinationIP.getText().toString()); //me
                    //final InetAddress destination = InetAddress.getByName("10.190.6.43"); //hugo

                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);

                    //apply a bunch of audio fixers
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        boolean agcAvailable = AutomaticGainControl.isAvailable();
                        if (agcAvailable)
                        {
                            AutomaticGainControl.create(recorder.getAudioSessionId());
                        }
                        boolean noiseSupressAvailable = NoiseSuppressor.isAvailable();
                        if (noiseSupressAvailable)
                        {
                            NoiseSuppressor.create(recorder.getAudioSessionId());
                        }

                        boolean echoCancelAvailable = AcousticEchoCanceler.isAvailable();
                        if (echoCancelAvailable)
                        {
                            AcousticEchoCanceler.create(recorder.getAudioSessionId());
                        }
                    }
                    recorder.startRecording();


                    while (status) {


                        //reading data from MIC into buffer
                        minBufSize = recorder.read(buffer, 0, buffer.length);

                        //putting buffer in the packet
                        packet = new DatagramPacket(buffer, buffer.length, destination, port);

                        socket.send(packet);
                        System.out.println("MinBufferSize: " + minBufSize);
                    }

                    recorder.stop();
                    recorder.release();
                    recorder = null;


                } catch (UnknownHostException e) {
                    Log.e("VS", "UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("VS", "IOException");
                }
            }

        });
        streamThread.start();
    }

    public void SearchWifi(View view)
    {
        int requestCode = 0;
        Intent intent = new Intent(Microphone.this, WifiSearch.class);
        //startActivityForResult(intent,requestCode);
        startActivity(intent);
    }
}
