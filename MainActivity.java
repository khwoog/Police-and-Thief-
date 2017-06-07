package kr.ac.kumoh.ce.s20120675.policebutton;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    LinearLayout linearLayout;
    Button btn;
    private static final int MILLISINFUTURE = 60*1000;
    private static final int COUNT_DOWN_INTERVAL = 1000;
    private int count = 59;
    private CountDownTimer countDownTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button)findViewById(R.id.btn);
        linearLayout = (LinearLayout)findViewById(R.id.lay);
        linearLayout.setVisibility(View.INVISIBLE);
        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                count = 59;
                linearLayout.setVisibility(View.VISIBLE);
                btn.setEnabled(false);
                btn.setBackgroundResource(R.drawable.thiefoff);
                countDownTimer();
                countDownTimer.start();

            }
        });


    }
    public void countDownTimer(){
        countDownTimer = new CountDownTimer(MILLISINFUTURE, COUNT_DOWN_INTERVAL) {
            public void onTick(long millisUntilFinished) {
                btn.setText(String.valueOf(count));
                count --;
            }

            public void onFinish() {
                btn.setEnabled(true);
                linearLayout.setVisibility(View.INVISIBLE);
                btn.setBackgroundResource(R.drawable.thief);
                btn.setText(String.valueOf(""));
            }
        };
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        try{
            countDownTimer.cancel();
        } catch (Exception e) {}
        countDownTimer=null;
    }
}
