package com.example.aggelis.src;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.aggelis.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import app.ArtistName;
import app.Info;
import app.Value;

import static android.os.Environment.getExternalStorageDirectory;

public class ListSongsActivity extends Activity {
    ArrayList<String> songNames = new ArrayList<String>();
    EditText songName;
    Button search, download;
    String artistName;
    Boolean initSongList = false;
    String userName;
    String ip = "192.168.1.131";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_songs);
        userName = getIntent().getExtras().getString("Username");

    }

    @Override
    public void onBackPressed()
    {
        AsyncTaskActivity.SocketHandler.getPrintWriter().println("back");
        finish();
    }

    public void onStart() {
        super.onStart();
        songName = (EditText) findViewById(R.id.searchSong);
        search = (Button) findViewById(R.id.search2);
        download = (Button) findViewById(R.id.download);
        artistName = getIntent().getExtras().getString("ArtistName");

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String song = songName.getText().toString().trim();
                for(String s : songNames){
                    if(song.equalsIgnoreCase(s)){
                        Intent intent = new Intent(view.getContext(),PlayerActivity.class);
                        intent.putExtra("Mode", "Online");
                        intent.putExtra("SongName", song);
                        intent.putExtra("ArtistName", artistName);
                        intent.putExtra("Username", userName);
                        startActivityForResult(intent,0);
                    }else{
                        Toast.makeText(getApplicationContext(),"Song: " + song + " doesn't exist!",Toast.LENGTH_SHORT).show();
                    }
                }


            }
        });

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String song = songName.getText().toString().trim();
                Boolean found = false;
                for(String s : songNames){
                    if(song.equalsIgnoreCase(s)){
                        found = true;
                        Toast.makeText(getApplicationContext(),"Downloading song: " + artistName + " - " + song + "...",Toast.LENGTH_SHORT).show();

                        DownloadTaskRunner runner = new DownloadTaskRunner();
                        runner.execute();

                        Toast.makeText(getApplicationContext(),"Your song has been downloaded successfully!",Toast.LENGTH_SHORT).show();

                    }
                }
                if(!found){
                    Toast.makeText(getApplicationContext(),"Song: " + song + " doesn't exist!",Toast.LENGTH_SHORT).show();
                }
            }
        });


        if(!initSongList) {
            AsyncTaskRunner runner = new AsyncTaskRunner();
            runner.execute();
            initSongList = true;
        }
    }
    private class DownloadTaskRunner extends AsyncTask<String, String, String> {
        String song = songName.getText().toString();

        Value brokerResponse;
        List<Value> chunks = new ArrayList<Value>();
        @Override
        protected String doInBackground(String... strings) {
            publishProgress("Sleeping...");
            AsyncTaskActivity.SocketHandler.getPrintWriter().println(song);
            brokerResponse = null;
            int length = 0;

            try {

                Value brokerResponse = (Value) AsyncTaskActivity.SocketHandler.getObjectInputStream().readObject();

                if (brokerResponse.getMusicFile().getMusicFileExtract() != null) { //Receiving chunks from broker.


                    while (brokerResponse.getMusicFile().getMusicFileExtract() != null) { //Writing chunk mp3 files in the Downloaded Chunks folder.
                        length = length + brokerResponse.getMusicFile().getMusicFileExtract().length;
                        chunks.add(brokerResponse);

                        brokerResponse = (Value) AsyncTaskActivity.SocketHandler.getObjectInputStream().readObject();

                        if (brokerResponse.getMusicFile().getMusicFileExtract() == null) {
                            break;
                        }
                    }

                    byte[] completeSong = new byte[length];
                    int currentPosition = 0; //Shows the position we are currently in for the byte array.
                    for(Value v:chunks){ //Combining chunks into a single byte array.
                        System.arraycopy(v.getMusicFile().getMusicFileExtract(), 0, completeSong, currentPosition, v.getMusicFile().getMusicFileExtract().length);
                        currentPosition = currentPosition + v.getMusicFile().getMusicFileExtract().length;
                    }
                    //Creating full song mp3 file.

                    File externalFilesDir = getExternalFilesDir("");
                    File file = new File(externalFilesDir, userName);
                    boolean result = file.mkdir(); //Creating directory for user.

                    File fullSong = new File (getExternalFilesDir(null) + "/" + userName, chunks.get(0).getMusicFile().getArtistName() + " - " + chunks.get(0).getMusicFile().getTrackName() + ".mp3");
                    fullSong.createNewFile();
                    FileOutputStream fos = new FileOutputStream(fullSong);
                    fos.write(completeSong);
                    fos.flush();
                    fos.close();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {
        ProgressDialog progressDialog;

        String artistName = getIntent().getExtras().getString("ArtistName");
        List<Info> brokerInfo = AsyncTaskActivity.SocketHandler.getBrokerInfo();
        @Override
        protected String doInBackground(String... params) {
            publishProgress("Sleeping...");
            try {
                try {
                    for(Info i:brokerInfo) {
                        for(ArtistName registeredArtist:i.getRegisteredArtists()) {
                            if(artistName.equalsIgnoreCase(registeredArtist.getArtistName())) {
                                if(!(AsyncTaskActivity.SocketHandler.getSocket().getPort() == Integer.parseInt(i.getPort()))){ //if consumer isn't already connected to the correct broker.
                                    AsyncTaskActivity.SocketHandler.getPrintWriter().println("quit"); //Sending terminal message to the Broker so that he can disconnect and  terminate the Thread.
                                    AsyncTaskActivity.SocketHandler.disconnect();
                                    try {
                                        AsyncTaskActivity.SocketHandler.setSocket(new Socket(ip, Integer.parseInt(i.getPort()))); //connecting to a new broker.
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    AsyncTaskActivity.SocketHandler.getPrintWriter().println(AsyncTaskActivity.SocketHandler.getConsumerID()); //sending consumerId to the new broker.
                                }
                                AsyncTaskActivity.SocketHandler.getPrintWriter().println(artistName); //Sending query to the Broker.
                            }
                        }
                    }

                    AsyncTaskActivity.SocketHandler.getPrintWriter().println("List songs!!!");
                    Value brokerResponse = (Value) AsyncTaskActivity.SocketHandler.getObjectInputStream().readObject();

                    if(brokerResponse.getMusicFile().getMusicFileExtract() == null){

                        String availableSongs = AsyncTaskActivity.SocketHandler.getBufferedReaderOut().readLine();
                        for(String songName:availableSongs.split("NEXT")) {//Printing existing songlist of the artist.
                            songNames.add(songName);
                        }
                    }
                } catch (UnknownHostException unknownHost) {
                    System.err.println("You are trying to connect to an unknown host!");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } finally {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            //listedArtists.setAdapter(itemsAdapter);

            ArrayAdapter itemsAdapter = new ArrayAdapter<String>(ListSongsActivity.this, android.R.layout.simple_list_item_1, songNames);
            ListView listView =(ListView)findViewById(R.id.listedSongs);
            listView.setAdapter(itemsAdapter);
            listView.setClickable(true);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String songName = (String) listView.getItemAtPosition(position);
                    Intent s = new Intent(view.getContext(),PlayerActivity.class);
                    s.putExtra("Mode", "Online");
                    s.putExtra("SongName", songName);
                    s.putExtra("ArtistName", artistName);
                    s.putExtra("Username", userName);
                    startActivityForResult(s,0);
                }
            });

            progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(ListSongsActivity.this,
                    "Wait while sleeping",
                    "Still searching...");
        }

        @Override
        protected void onProgressUpdate(String... text) {
        }
    }
}
