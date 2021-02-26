package com.example.lottomatch;

import androidx.appcompat.app.AppCompatActivity;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    Button lotteryButton;
    SQLiteDatabase database;

    // logcat
    private static final String TAG_DB = "DB";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DBThread thread = new DBThread();
        thread.start();

        lotteryButton = findViewById(R.id.lotteryButton);
        lotteryButton.setOnClickListener(v -> {

        });
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

class DBThread extends Thread {

    int lottoHistory[] = new int[46];
    Button lotteryButton;

    // logcat
    private static final String TAG_FILE = "FILE";

    @Override
    public void run() {
        try {
            fileRead();
        }
        catch(IOException e) {
            Log.d(TAG_FILE, e + " IOException");
        }
    }

    // 바이트스트림으로 저장하려 했으나 실패함.. ㅠ
    private void fileRead() throws IOException{
        File f = new File("/data/data/com.example.lottomatch/files/", "history.txt");
        // 파일이 존재하지 않을경우
        if(!f.isFile()) {
            Log.d(TAG_FILE, "file not exists");
        }
        // 파일이 존재 할 경우
        else {
            BufferedReader br = new BufferedReader(new FileReader(f));
            int n;
            String tmp = br.readLine();
            StringTokenizer st = new StringTokenizer(tmp);
            n = 0;
            while(st.hasMoreTokens())
                lottoHistory[n++] = Integer.parseInt(st.nextToken());
            br.close();
        }
        fileUpdate();
    }

    private void fileUpdate() throws IOException {
        // 현재 까지 당첨번호 통계 불러오기
        int n = lottoHistory[0] + 1;
        while(true) {
            // 불러올 url 주소
            String url = "https://dhlottery.co.kr/gameResult.do?method=byWin&drwNo=" + n;
            // doc에 해당 url의 html 정보를 가져옴
            // mainthread 에서 network를 사용하면 에러가 발생함.
            Document doc = Jsoup.connect(url).get();

            // 로또 당점 번호가 있는 부분을 추출함
            Elements elem = doc.select(".num.win");
            StringTokenizer st = new StringTokenizer(elem.text());
            Log.d(TAG_FILE, n + "회차 당첨번호 : " + elem.text());

            // "당첨번호" 토큰 제거
            st.nextToken();

            if(!st.hasMoreTokens()) {
                Log.d(TAG_FILE, n + "회차는 아직 나오지 않았습니다.");
                break;
            }

            // 해당 당첨 번호 idx의 값을 1증가
            for(int j = 0; j < 6; j++) {
                int tmp = Integer.parseInt(st.nextToken());
                lottoHistory[tmp]++;
            }
            n++;
        }
        // 나온 회차까지
        lottoHistory[0] = n-1;

        // lottoHistory의 값들을 파일에 저장한다.
        File f = new File("/data/data/com.example.lottomatch/files/", "history.txt");
        FileWriter fos = new FileWriter(f);
        for(int i = 0; i < lottoHistory.length; i++) {
            fos.write(lottoHistory[i] + " ");
        }
        fos.close();
    }
}