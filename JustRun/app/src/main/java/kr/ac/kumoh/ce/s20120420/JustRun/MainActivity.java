package kr.ac.kumoh.ce.s20120420.JustRun;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    final Context context = this;
    Button mEnterBtn;
    EditText mNicName;
    InputMethodManager imm;
    LinearLayout ll;

Intent a;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*a=new Intent(this,Randomac.class);
        startActivity(a);*/


        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        ll = (LinearLayout)findViewById(R.id.ll);
        mEnterBtn = (Button)findViewById(R.id.enterBtn);
        mNicName = (EditText) findViewById(R.id.nicName);


//        mEnterBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(getApplicationContext(), RoomList.class);
//                intent.putExtra("nicName", mNicName.getText().toString());
//                startActivity(intent);
//            }
//        });// *************** 입장시 닉네임을 RoomList클래스에 전달 ***************
        mEnterBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP) {
                    mEnterBtn.setBackgroundResource(R.drawable.button_enter);
                }
                if (action == MotionEvent.ACTION_DOWN) {
                    mEnterBtn.setBackgroundResource(R.drawable.button_enter2);
                }
                return false;
            }
        });
        mEnterBtn.setOnClickListener(myClickListener);
        ll.setOnClickListener(myClickListener);

        mNicName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //Enter key Action
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });
    }

    View.OnClickListener myClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            hideKeyboard();
            switch (v.getId())
            {
                case R.id.ll :
                    break;

                case R.id.enterBtn:
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            context);

                    // 제목셋팅
                    alertDialogBuilder.setTitle("게임 입장");

                    // AlertDialog 셋팅

                    alertDialogBuilder
                            .setMessage("처음 플레이하시는 분이시면 오른쪽 상단의 '도움말' 버튼을 클릭하여 주세요")
                            .setCancelable(false)
                            .setPositiveButton("확인",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                            Intent intent = new Intent(getApplicationContext(), RoomList.class);
                                            intent.putExtra("nicName", mNicName.getText().toString());
                                            startActivity(intent);
                                            dialog.cancel();
                                        }
                                    });

                    // 다이얼로그 생성
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // 다이얼로그 보여주기
                    alertDialog.show();
                    break;

                default:
                    break;
            }
        }

    };

    private void hideKeyboard()
    {
        imm.hideSoftInputFromWindow(mNicName.getWindowToken(), 0);
    }

}

