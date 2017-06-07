package kr.ac.kumoh.ce.s20120675.zonegage;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Animation grow;
    ProgressBar pgbar ;
    TextView tv;
    int progressBarValue = 0;
    Handler handler = new Handler();

    boolean isStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn1 = (Button)findViewById(R.id.btn);
        tv = (TextView)findViewById(R.id.tv);
        pgbar = (ProgressBar)findViewById(R.id.pgbar);
        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.circular);
        pgbar.setVisibility(View.INVISIBLE);
        handler = new Handler()
        {
            public void handleMessage(android.os.Message msg)
            {
                if(isStart)
                {
                    progressBarValue++;
                    if(progressBarValue==100)
                    {
                        progressBarValue=0;
                        pgbar.setVisibility(View.INVISIBLE);
                        handler.removeCallbacksAndMessages(null);
                        tv.setText("아이템 있어");
                    }
                }
                pgbar.setProgress(progressBarValue);


                handler.sendEmptyMessageDelayed(0, 30);
            }
        };


        grow = AnimationUtils.loadAnimation(this,R.anim.grow);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        btn1.setOnLongClickListener(new View.OnLongClickListener() { ///꾹 누를때
            @Override
            public boolean onLongClick(View v) {
               // ProgressBar pgbar = (ProgressBar)findViewById(R.id.pgbar);
               // pgbar = (ProgressBar)findViewById(R.id.pgbar);
                pgbar.setVisibility(View.VISIBLE);
               // pgbar.startAnimation(grow);
                isStart = true;
              //  pgbar.setProgress(0);

                handler.sendEmptyMessage(0);
               // pgbar.setProgress(100);

                return false;
            }
        });
        btn1.setOnTouchListener(new View.OnTouchListener(){ ///손가락 뗄때
            @Override
            public boolean onTouch(View v, MotionEvent event) {
          //      pgbar = (ProgressBar)findViewById(R.id.pgbar);
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP) {
                 //   Toast.makeText(MainActivity.this,"asdfafds",Toast.LENGTH_SHORT).show();
                    isStart = false;
                    progressBarValue=0;
                    pgbar.setVisibility(View.INVISIBLE);
                    handler.removeCallbacksAndMessages(null);

                }

                return false;
            }
        });
    }

}
