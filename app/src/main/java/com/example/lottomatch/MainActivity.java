package com.example.lottomatch;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    //logcat
    private static final String SQL_TAG = "SQL";
    
    Button lotteryButton;
    TextView lottoText;

    SQLiteDatabase database;
    String DB = "lottoNumber";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lotteryButton = findViewById(R.id.lotteryButton);
        lottoText = findViewById(R.id.lottoText);

        database = openOrCreateDatabase(DB, MODE_PRIVATE, null);

        createDB();
        upDateDB();
    }

    public void onLotteryButtonClicked(View v) {
        int lotto[] = new int[6];
        int i = 0;
        while(true) {
            boolean duplicate = false;
            // 0~45
            int ranNum = (int)(Math.random()*45) + 1;
            // 중복체크
            for(int k : lotto) {
                if(k == ranNum)
                    duplicate = true;
            }
            Log.d(SQL_TAG, ranNum + " ");
            if(duplicate == true)
                continue;
            lotto[i] = ranNum;
            i += 1;
            // 6개 모두 뽑았을 경우 반복 종료
            if(i == lotto.length)
                break;
        }

        database.execSQL("insert into " + DB
        + "(first, second, third, fourth, fifth, sixth)"
        + " values "
        + "(" + lotto[0] + ", " + lotto[1] + ", " + lotto[2] + ", "
        + lotto[3] + ", " + lotto[4] + ", " + lotto[5] + ")");

        upDateDB();
    }

    public void onResetButtonClicked(View v) {
        database.execSQL("DROP TABLE IF EXISTS " + DB);
        lottoText.setText("");
        createDB();
    }

    void upDateDB() {
        Cursor cursor = database.rawQuery("select _id, first, second, third, fourth, fifth, sixth from " + DB, null);
        int recordCount = cursor.getCount();
        lottoText.setText("");
        for(int n = 0; n < recordCount; n++) {
            cursor.moveToNext();
            for(int k = 1; k <= 6; k++)
                lottoText.append(cursor.getInt(k) + " ");
            lottoText.append("\n");
        }
    }

    void createDB() {
        // create SQLite database
        database.execSQL("create table if not exists " + DB + "("
                + "_id integer PRIMARY KEY autoincrement, "
                + " first integer, "
                + " second integer, "
                + " third integer, "
                + " fourth integer, "
                + " fifth integer, "
                + " sixth integer)");
    }
}