package com.example.lottomatch;

import androidx.appcompat.app.AppCompatActivity;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.jsoup.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // logcat
    private static final String TAG_DB = "DB";

    SQLiteDatabase database;
    Button lotteryButton;

    int lottoHistory[] = new int[46];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createDatabase();
        fileRead();

        lotteryButton = findViewById(R.id.lotteryButton);
        lotteryButton.setOnClickListener(v -> {

        });
    }

    private void fileRead() {
        File f = new File(getFilesDir(), "history.txt");
        if(!f.isFile()) {
            Log.d(TAG_DB, "file not exists");
            try {
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(0);
                fos.close();
            }
            catch(IOException e) {
                Log.d(TAG_DB, e + " IOException");
            }
        }
        else {
            Log.d(TAG_DB, "file exists");
            try {
                FileInputStream fis = new FileInputStream(f);
                int idx = 0, t;
                while((t = fis.read()) != -1) {
                    lottoHistory[idx] = t;
                }
            }
            catch(IOException e) {
                Log.d(TAG_DB, e + " IOException");
            }
        }
    }
    private void createDatabase() {
        Log.d(TAG_DB, "createDatabase called");

        database = openOrCreateDatabase("lotto", MODE_PRIVATE, null);

        // 추첨번호 table
        database.execSQL("create table if not exists lottery" + "("
        + "first integer, "
        + "second integer, "
        + "third integer, "
        + "fourth integer, "
        + "fifth integer, "
        + "sixth integer" + ")");

        // 지금 까지 나온 번호, 회차 table
        database.execSQL("create table if not exists lottoHistory" + "("
        + "_id integer PRIMARY KEY autoincrement, "
        + "num integer)");
    }
}