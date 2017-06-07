package kr.ac.kumoh.s20121133.ce.beforegame;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by jang on 2017-05-31.
 */

public class RoomCreate extends AppCompatActivity{

    String mTitle;
    String mPassword;
    String mRadius;
    InputMethodManager imm;
    LinearLayout ll;
    EditText createTitle;
    EditText createPwd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_create);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        ll = (LinearLayout)findViewById(R.id.createRoom);
        createTitle = (EditText)findViewById(R.id.createTitle);
        createPwd = (EditText)findViewById(R.id.createPwd);


        final TextView tv = (TextView)findViewById(R.id.abc);

        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mRadius = parent.getItemAtPosition(position).toString();
                tv.setText("제한구역 : " + parent.getItemAtPosition(position) + "m");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        }); //*************** 스피너를 이용한 제한구역 설정 리스너 ***************

        RadioGroup rg = (RadioGroup)findViewById(R.id.radioGroup1);
        final EditText editPwd = (EditText)findViewById(R.id.createPwd);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if(checkedId == R.id.radio0){
                    editPwd.setText("");
                    editPwd.setEnabled(false);
                }else if(checkedId == R.id.radio1){
                    editPwd.setEnabled(true);
                }
            }
        }); //*************** 비밀번호 공개/비공개 클릭에 대한 리스터 ***************

        Button Btn = (Button)findViewById(R.id.createBtn);
        Btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                TextView getTitle = (TextView)findViewById(R.id.createTitle);
                TextView getPassword = (TextView)findViewById(R.id.createPwd);
                mTitle = getTitle.getText().toString();
                mPassword = getPassword.getText().toString();

                Intent intent = new Intent(getApplicationContext(), RoomIn.class);
                intent.putExtra("create", "yes");
                intent.putExtra("createTitle", mTitle);
                intent.putExtra("createPwd", mPassword);
                intent.putExtra("createRadius",mRadius);
                startActivity(intent);
            }
        }); // *************** 방만들기 생성시 서버에 데이터 전달 ***************
        ll.setOnClickListener(myClickListener);// *******************EditText 자판 숨기기***********************
        createTitle.setOnKeyListener(new View.OnKeyListener() {
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
        createPwd.setOnKeyListener(new View.OnKeyListener() {
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

                case R.id.createBtn :
                    break;
            }
        }
    };

    private void hideKeyboard()
    {
        imm.hideSoftInputFromWindow(createTitle.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(createPwd.getWindowToken(), 0);
    }
}
