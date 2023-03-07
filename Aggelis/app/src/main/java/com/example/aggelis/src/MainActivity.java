package com.example.aggelis.src;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.aggelis.R;

import java.io.File;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onStart() {
        super.onStart();
        EditText usernameText = (EditText) findViewById(R.id.username);
        Button loginButton = (Button) findViewById(R.id.login);
        Button offlineButton = (Button) findViewById(R.id.offlinePlay);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(usernameText.getText().toString().trim().equalsIgnoreCase("")){
                    Toast.makeText(getApplicationContext(),"Please enter a username!",Toast.LENGTH_SHORT).show();
                }else {
                    Intent s = new Intent(view.getContext(), AsyncTaskActivity.class);
                    s.putExtra("Username", usernameText.getText().toString().trim());
                    startActivityForResult(s, 0);
                }
            }
        });

        offlineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(usernameText.getText().toString().trim().equalsIgnoreCase("")) {
                    Toast.makeText(getApplicationContext(),"Please enter a username!",Toast.LENGTH_SHORT).show();
                }else {
                    File externalFilesDir = getExternalFilesDir("");
                    File file = new File(externalFilesDir, usernameText.getText().toString().trim());
                    boolean result = file.mkdir(); //Creating directory for user.
                    if (!result) {
                        Intent s = new Intent(view.getContext(), OfflineModeActivity.class);
                        String user = usernameText.getText().toString().trim();

                        s.putExtra("Username", user);
                        startActivityForResult(s, 0);
                    } else {
                        file.delete();
                        Toast.makeText(getApplicationContext(), "You don't have any downloaded songs yet!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
