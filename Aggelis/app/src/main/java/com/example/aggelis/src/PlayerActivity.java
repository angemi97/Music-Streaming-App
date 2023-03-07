package com.example.aggelis.src;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.example.aggelis.R;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import app.ArtistName;
import app.Info;
import app.Value;

public class PlayerActivity extends Activity {
    Button b1,pause,back,resume;
    ImageView iv;
    MediaPlayer mp;
    MediaPlayer bufferMp;
    Boolean mpPaused, bufferMpPaused;
    String artistName, songName, userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_activity);

        b1 = (Button) findViewById(R.id.button);
        pause = (Button) findViewById(R.id.pauseButton);
        back = (Button)findViewById(R.id.button3);
        resume = (Button)findViewById(R.id.resumeButton);
        iv = (ImageView)findViewById(R.id.imageView);
        artistName = getIntent().getExtras().getString("ArtistName");
        songName = getIntent().getExtras().getString("SongName");
        userName = getIntent().getExtras().getString("Username");
        Log.d("aa", userName);

        //tx2 = (TextView)findViewById(R.id.textView3);
        //tx3 = (TextView)findViewById(R.id.textView4);

        mpPaused = false;
        bufferMpPaused = false;

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mp.isPlaying()){
                    mp.pause();
                    mpPaused = true;
                }else if(bufferMp.isPlaying()){
                    bufferMp.pause();
                    bufferMpPaused = true;
                }
            }
        });

        resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mpPaused){

                    mp.start();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mpPaused = false;

                }else if(bufferMpPaused){

                    bufferMp.start();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    bufferMpPaused = false;
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp.pause();
                mp.stop();
                if(getIntent().getExtras().getString("Mode").equalsIgnoreCase("Offline")){
                    finish();
                }else {
                    bufferMp.pause();
                    bufferMp.stop();
                    try {
                        AsyncTaskActivity.SocketHandler.refreshObjectIoStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //AsyncTaskActivity.SocketHandler.getPrintWriter().println("back");
                    Intent s = new Intent(view.getContext(), AsyncTaskActivity.class);
                    s.putExtra("Username", userName);
                    startActivityForResult(s, 0);
                }
            }
        });


        mp = new MediaPlayer();
        bufferMp = new MediaPlayer();
    }

    @Override
    public void onBackPressed()
    {
        mp.pause();
        mp.stop();
        finish();
    }

    public void onStart() {
        super.onStart();

        if(getIntent().getExtras().getString("Mode").equalsIgnoreCase("Offline")){
            OfflineTaskRunner runner = new OfflineTaskRunner();
            runner.execute();
        }else {
            AsyncTaskRunner runner = new AsyncTaskRunner();
            runner.execute();
        }
    }

    private class OfflineTaskRunner extends AsyncTask<String, String, String> {
        ProgressDialog progressDialog;
        String songName = getIntent().getExtras().getString("SongName");
        File folder;
        File[] storedSongs;
        File storedMp3;
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected String doInBackground(String... params) {
            publishProgress("Sleeping...");
            folder = new File(getExternalFilesDir(null) + "/" + userName);
            storedSongs = folder.listFiles();

            runOnUiThread(new Runnable() { //updating UI view.

                @Override
                public void run() {

                    TextView artistName = (TextView) findViewById(R.id.textView2); //Here it's the name of the song.
                    artistName.setText(songName);
                }
            });

            try {
                mp.setDataSource(getExternalFilesDir("") + "/" + userName + "/" + songName);
                mp.prepare();
                mp.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            //listedArtists.setAdapter(itemsAdapter);
            //progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            /*progressDialog = ProgressDialog.show(PlayerActivity.this,
                    "Wait while sleeping",
                    "Still searching...");

             */
        }

        @Override
        protected void onProgressUpdate(String... text) {
        }
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {
        ProgressDialog progressDialog;
        String songName = getIntent().getExtras().getString("SongName");
        File tempMp3;
        Value brokerResponse;
        List<Value> chunks = new ArrayList<Value>();

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected String doInBackground(String... params) {
            publishProgress("Sleeping...");
            AsyncTaskActivity.SocketHandler.getPrintWriter().println(songName);

            brokerResponse = null;
            try {
                int c = 0;
                int fileRead = 0; //Shows the index to the last file that has been played.
                tempMp3 = File.createTempFile("chunk" + c, "mp3", getCacheDir());
                FileOutputStream fos = new FileOutputStream(tempMp3);
                FileInputStream fis = new FileInputStream(tempMp3);

                brokerResponse = (Value) AsyncTaskActivity.SocketHandler.getObjectInputStream().readObject();
                chunks.add(brokerResponse);
                runOnUiThread(new Runnable() { //updating UI view.

                    @Override
                    public void run() {

                        TextView artistNameView = (TextView) findViewById(R.id.textView2);
                        artistNameView.setText(artistName + " - " + songName);
                    }
                });


                if(brokerResponse.getMusicFile().getMusicFileExtract() != null){ //Receiving chunks from broker.

                    fos.write(brokerResponse.getMusicFile().getMusicFileExtract());
                    mp.setDataSource(tempMp3.getAbsolutePath());
                    mp.prepare();
                    mp.start();
                    while(brokerResponse.getMusicFile().getMusicFileExtract() != null) { //Writing chunk mp3 files in the Downloaded Chunks folder.
                        c++;
                        File tempMp3 = File.createTempFile("chunk" + c, "mp3", getCacheDir());
                        fos = new FileOutputStream(tempMp3);
                        brokerResponse = (Value) AsyncTaskActivity.SocketHandler.getObjectInputStream().readObject();
                        if(brokerResponse.getMusicFile().getMusicFileExtract()!=null) {
                            chunks.add(brokerResponse);
                            fos.write(brokerResponse.getMusicFile().getMusicFileExtract());
                            fos.close();
                        }


                        if(mp.isPlaying()) {
                                bufferMp.setDataSource(tempMp3.getAbsolutePath());
                                bufferMp.prepare();
                                mp.setNextMediaPlayer(bufferMp);
                                while (mp.isPlaying() || mpPaused) {
                                    // Log.d("aa", "Duration: " + String.valueOf(mp.getDuration()) + "Current: " + mp.getCurrentPosition());
                                }
                                mp.stop();
                                mp.reset();
                        }else{
                                mp.setDataSource(tempMp3.getAbsolutePath());
                                mp.prepare();
                                bufferMp.setNextMediaPlayer(mp);
                                while (bufferMp.isPlaying() || bufferMpPaused) {
                                    // Log.d("aa", "sleeping2");
                                }
                                bufferMp.stop();
                                bufferMp.reset();
                        }


                        if(brokerResponse.getMusicFile().getMusicFileExtract() == null) {
                            break;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            //listedArtists.setAdapter(itemsAdapter);
            //progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            /*progressDialog = ProgressDialog.show(PlayerActivity.this,
                    "Wait while sleeping",
                    "Still searching...");

             */
        }

        @Override
        protected void onProgressUpdate(String... text) {
        }
    }
}
