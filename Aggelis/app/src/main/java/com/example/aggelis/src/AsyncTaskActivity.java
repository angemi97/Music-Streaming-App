package com.example.aggelis.src;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aggelis.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import app.ArtistName;
import app.Info;

import static app.Node.FIRSTBROKER;
import static app.Node.SECONDBROKER;
import static app.Node.THIRDBROKER;
import static app.Node.ip;

public class AsyncTaskActivity extends Activity {
    TextView tv1;
    ListView listedArtists;
    ArrayList<String> artistNames = new ArrayList<String>();
    String userName;
    String ip = "192.168.1.131";
    int portF = 3547;
    int portS = 4531;
    int portT = 5478;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.async_task);
        tv1 = (TextView) findViewById(R.id.listOfArtists);
        userName = getIntent().getExtras().getString("Username");


    }

    public void onStart() {
        super.onStart();

        EditText artistName = (EditText) findViewById(R.id.searchArtist);
        Button search = (Button) findViewById(R.id.search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String artist = artistName.getText().toString().trim();
                Boolean found = false;

                for(String a:artistNames){
                    if(a.equalsIgnoreCase(artist)){
                        found = true;
                        Intent s = new Intent(view.getContext(),ListSongsActivity.class);
                        s.putExtra("ArtistName", artist);
                        s.putExtra("Username", userName);
                        startActivityForResult(s,0);
                    }
                }
                if(!found){
                    Toast.makeText(getApplicationContext(),"Artist: " + artist + " doesn't exist!",Toast.LENGTH_SHORT).show();
                }
            }
        });

        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute();

    }

    @Override
    public void onBackPressed()
    {
        AsyncTaskActivity.SocketHandler.getPrintWriter().println("quit");
        SocketHandler.disconnect();
        finish();
    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String> {
        ProgressDialog progressDialog;

        Socket requestSocket;

        String consumerID;
        List<Info> brokerInfo = new ArrayList<>();
        @Override
        protected String doInBackground(String... params) {
            publishProgress("Sleeping...");
            try {
                if(SocketHandler.getBrokerInfo() == null){
                    try {

                        if(SocketHandler.getSocket() == null) {
                            Random randGen = new Random();
                            int random = randGen.nextInt(3);
                            if(random == 0) {
                                SocketHandler.setSocket(new Socket(ip, portF));
                            }else if(random == 1) {
                                SocketHandler.setSocket(new Socket(ip, portS));
                            }else {
                                SocketHandler.setSocket(new Socket(ip, portT));
                            }
                        }

                        requestSocket = SocketHandler.getSocket();

                        SocketHandler.getPrintWriter().println("Initialize broker list.");
                        Random randGen = new Random();
                        consumerID = String.valueOf(randGen.nextInt(100000));
                        SocketHandler.setConsumerID(consumerID);
                        SocketHandler.getPrintWriter().println(consumerID); //sending consumerId.


                        Info info = (Info) SocketHandler.getObjectInputStream().readObject();//getting broker info

                        while(!info.getIp().equalsIgnoreCase("")) {
                            brokerInfo.add(info);
                            info = (Info) SocketHandler.getObjectInputStream().readObject();
                        }
                        SocketHandler.setBrokerInfo(brokerInfo);




                    } catch (UnknownHostException unknownHost) {
                        System.err.println("You are trying to connect to an unknown host!");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    } finally {
                    }
                }

                for(Info i:SocketHandler.getBrokerInfo()) {
                    for(ArtistName artist:i.getRegisteredArtists()) {
                        artistNames.add(artist.getArtistName());
                    }
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

            ArrayAdapter itemsAdapter = new ArrayAdapter<String>(AsyncTaskActivity.this, android.R.layout.simple_list_item_1, artistNames);
            ListView listView =(ListView)findViewById(R.id.listedArtists);
            listView.setAdapter(itemsAdapter);
            listView.setClickable(true);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String artistName = (String) listView.getItemAtPosition(position);



                    Intent s = new Intent(view.getContext(),ListSongsActivity.class);
                    s.putExtra("ArtistName", artistName);
                    s.putExtra("Username", userName);
                    startActivityForResult(s,0);
                }
            });

            progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(AsyncTaskActivity.this,
                    "Wait while sleeping",
                    "Still searching...");

        }

        @Override
        protected void onProgressUpdate(String... text) {

        }
    }

    public static class SocketHandler {
        private static Socket socket = null;
        private static PrintWriter initializeQuery;
        private static BufferedReader out;
        private static BufferedReader keyboard;
        private static InputStreamReader input;
        private static ObjectInputStream inB;
        private static List<Info> brokerInfo = null;
        private static String consumerID;

        public static synchronized void setConsumerID(String consumerID){
            SocketHandler.consumerID = consumerID;
        }

        public static synchronized String getConsumerID(){
            return SocketHandler.consumerID;
        }

        public static synchronized void setBrokerInfo(List<Info> brokerInfo){
            SocketHandler.brokerInfo = brokerInfo;
        }

        public static synchronized List<Info> getBrokerInfo(){
            return SocketHandler.brokerInfo;
        }

        public static synchronized void disconnect() {//Disconnects from a Broker and closes sockets, readers/writers and I/O streams.
            try {
                out.close();
                input.close();
                initializeQuery.close();
                socket.close();
                inB.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static synchronized Socket getSocket(){
            return socket;
        }

        public static synchronized PrintWriter getPrintWriter(){
            return initializeQuery;
        }

        public static synchronized BufferedReader getBufferedReaderOut(){
            return out;
        }

        public static synchronized BufferedReader getBufferedReaderKeyboard(){
            return keyboard;
        }

        public static synchronized InputStreamReader getInputStreamReader(){
            return input;
        }

        public static synchronized ObjectInputStream getObjectInputStream(){
            return inB;
        }

        public static synchronized void refreshObjectIoStream() throws IOException {
            out.close();
            input.close();
            initializeQuery.close();
            socket.close();
            inB.close();
            SocketHandler.keyboard = new BufferedReader(new InputStreamReader(System.in));
            SocketHandler.input = new InputStreamReader(socket.getInputStream());
            SocketHandler.out = new BufferedReader(input);
            SocketHandler.initializeQuery = new PrintWriter(socket.getOutputStream(), true);
            SocketHandler.inB = new ObjectInputStream(socket.getInputStream());
            SocketHandler.inB = new ObjectInputStream(socket.getInputStream());
        }

        public static synchronized void setSocket(Socket requestSocket){
            SocketHandler.socket = requestSocket;

            try {
                SocketHandler.keyboard = new BufferedReader(new InputStreamReader(System.in));
                SocketHandler.input = new InputStreamReader(requestSocket.getInputStream());
                SocketHandler.out = new BufferedReader(input);
                SocketHandler.initializeQuery = new PrintWriter(requestSocket.getOutputStream(), true);
                SocketHandler.inB = new ObjectInputStream(requestSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}