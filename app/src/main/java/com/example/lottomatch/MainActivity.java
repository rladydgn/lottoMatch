package com.example.lottomatch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

    Button lotteryButton, resetButton;
    TextView lotteryNumbers;
    SQLiteDatabase database;

    // logcat
    private static final String TAG_DB = "DB";
    private static final String TAG_EVENT = "EVENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lotteryButton = findViewById(R.id.lotteryButton);
        lotteryNumbers = findViewById(R.id.lotteryNumbers);
        resetButton = findViewById(R.id.resetButton);

        DBThread thread = new DBThread();
        thread.start();
        createDatabase();
        getDatabase();

        // lambda
        lotteryButton.setOnClickListener(v -> {
            if(!thread.isReady()) {
                Toast.makeText(getApplicationContext(), "데이터 업데이트중! 잠시후 다시시도 하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG_EVENT, "lotteryButton clicked");
            Toast.makeText(getApplicationContext(), "번호 추출중 ! 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show();
            int lottoHistory[] = thread.getLottoHistory();
            int avg = 0;
            for(int i : lottoHistory) {
                if(i == lottoHistory[0])
                    continue;
                avg += i;
            }
            avg /= 45;
            int sample[] = new int[(avg+1)*45];
            int idx = 0;
            // sample에 지금 까지 나온 번호 통계를 이용해 숫자를 적절한 양만큼 넣어준다.
            for(int i = 1; i < 46; i++) {
                int n;
                if(avg > lottoHistory[i])
                    n = avg + avg - lottoHistory[i];
                else
                    n = avg - (lottoHistory[i] - avg);
                while(true) {
                    sample[idx++] = i;
                    if(--n == 0)
                        break;
                }
            }
            int pickedNumbers[] = new int[6];
            int k = 0;
            while(true) {
                boolean TF = false;
                int tmp = sample[(int)(Math.random()*idx)];
                for(int i : pickedNumbers) {
                    if(tmp == i) {
                        TF = true;
                        break;
                    }
                }
                if(TF)
                    continue;
                pickedNumbers[k++] = tmp;
                if(k == 6)
                    break;
            }
            Log.d(TAG_EVENT, "추출한 당첨번호 : " + pickedNumbers[0] + " " + pickedNumbers[1] + " "
                    + pickedNumbers[2] + " " + pickedNumbers[3] + " " + pickedNumbers[4] + " " + pickedNumbers[5]);
            updateDatabase(pickedNumbers);
        });

        resetButton.setOnClickListener(v -> {
            deleteDatabase();
        });
    }

    // database는 추첨한 번호들을 저장하는 용도이다.
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
    }

    private void updateDatabase(int[] pickedNumbers) {
        database.execSQL("insert into lottery"
        + "(first, second, third, fourth, fifth, sixth) "
        + " values "
        + "(" + pickedNumbers[0] + ", " + pickedNumbers[1] + ", " + pickedNumbers[2] + ", "
        + pickedNumbers[3] + ", " + pickedNumbers[4] + ", " + pickedNumbers[5] + ")");
        Log.d(TAG_DB, "DB에 레코드 저장 완료.");
        getDatabase();
    }

    private void deleteDatabase() {
        database.execSQL("delete from lottery");
        Log.d(TAG_DB, "table 초기화 완료.");
        getDatabase();
    }

    private void getDatabase() {
        Cursor cursor = database.rawQuery("select first, second, third, fourth, fifth, sixth from lottery", null);
        int recordCount = cursor.getCount();
        lotteryNumbers.setText("");
        for(int i = 0; i < recordCount; i++) {
            cursor.moveToNext();
            lotteryNumbers.append(cursor.getInt(0) + " " + cursor.getInt(1) + " "
                    + cursor.getInt(2) + " " + cursor.getInt(3) + " "
                    + cursor.getInt(4) + " " + cursor.getInt(5) + " " + "\n");
        }
    }
}

// 당첨번호별 통계를 가져오는 스레드
class DBThread extends Thread {

    int lottoHistory[] = new int[46];
    boolean TF;

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
        File f = new File("/data/data/com.example.lottomatch", "history.txt");
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
        TF = true;
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
        File f = new File("/data/data/com.example.lottomatch", "history.txt");
        FileWriter fos = new FileWriter(f);
        for(int i = 0; i < lottoHistory.length; i++) {
            fos.write(lottoHistory[i] + " ");
        }
        fos.close();
    }

    public int[] getLottoHistory() {
        return lottoHistory;
    }

    public boolean isReady() {
        return TF;
    }
}