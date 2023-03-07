package com.example.aggelis.src;
import android.app.Activity;
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
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import app.ArtistName;
import app.Info;
import app.Value;

public class OfflineModeActivity extends Activity {
    EditText songName;
    Button search;
    ArrayList<String> songNames = new ArrayList<String>();
    Boolean initSongList = false;
    String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.offline_mode);
        userName = getIntent().getExtras().getString("Username");
        Log.d("aa", userName);
    }

    public void onStart() {
        super.onStart();

        songName = (EditText) findViewById(R.id.searchSong);
        search = (Button) findViewById(R.id.search2);



        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String song = songName.getText().toString();
                Boolean found = false;
                for(String s : songNames){
                    if(s.contains(song)){
                        found = true;
                        Intent intent = new Intent(view.getContext(),PlayerActivity.class);
                        intent.putExtra("Mode", "Offline");
                        intent.putExtra("SongName", s);
                        intent.putExtra("Username", userName);
                        startActivityForResult(intent,0);
                    }
                }
                if(!found) {
                    Toast.makeText(getApplicationContext(), "Song: " + song + " doesn't exist!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if(!initSongList) {
            AsyncTaskRunner runner = new AsyncTaskRunner();
            runner.execute();
            initSongList = true;
        }
    }



    private class AsyncTaskRunner extends AsyncTask<String, String, String> {
        ProgressDialog progressDialog;

        @Override
        protected String doInBackground(String... params) {
            publishProgress("Sleeping...");
            File folder = new File(String.valueOf(getExternalFilesDir(null)) + "/" + userName);
            File[] storedSongs = folder.listFiles();

            for (File f : storedSongs) {
                songNames.add(f.getName());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            //listedArtists.setAdapter(itemsAdapter);
            for(String f:songNames){
                Log.d("ay", f);
            }
            ArrayAdapter itemsAdapter = new ArrayAdapter<String>(OfflineModeActivity.this, android.R.layout.simple_list_item_1, songNames);
            ListView listView =(ListView)findViewById(R.id.listedOffSongs);
            listView.setAdapter(itemsAdapter);
            listView.setClickable(true);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    String songName = (String) listView.getItemAtPosition(position);
                    Intent s = new Intent(view.getContext(),PlayerActivity.class);
                    s.putExtra("Mode", "Offline");
                    s.putExtra("SongName", songName);
                    s.putExtra("Username", userName);
                    startActivityForResult(s,0);
                }
            });

            progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(OfflineModeActivity.this,
                    "Wait while sleeping",
                    "Still searching...");
        }

        @Override
        protected void onProgressUpdate(String... text) {
        }
    }
}
