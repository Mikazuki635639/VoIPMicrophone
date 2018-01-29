package com.example.wacif.myapplication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.*;
import android.widget.ToggleButton;
import android.util.Log;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;


public class Microphone extends AppCompatActivity implements View.OnTouchListener {

    //public byte[] buffer;
    //public static DatagramSocket socket;
    private final int port=50005;

    AudioRecord recorder;
    static boolean status = false;

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
            recorder.stop();
            recorder.release();
            recorder = null;
        }
        return false;
    }

    public void startStreaming() {

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    DatagramSocket socket = new DatagramSocket();

                    byte[] buffer = new byte[minBufSize];
                    DatagramPacket packet;

                    final InetAddress destination = InetAddress.getByName("192.168.0.100"); //me
                    //final InetAddress destination = InetAddress.getByName("10.190.6.43"); //hugo

                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize * 10);
                    recorder.startRecording();

                    while (status) {


                        //reading data from MIC into buffer
                        minBufSize = recorder.read(buffer, 0, buffer.length);

                        //putting buffer in the packet
                        packet = new DatagramPacket(buffer, buffer.length, destination, port);

                        socket.send(packet);
                        System.out.println("MinBufferSize: " + minBufSize);
                    }


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
}
