package com.example.nyg.running;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    Button mEnterBtn;
    EditText mNicName;
    InputMethodManager imm;
    LinearLayout ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        ll = (LinearLayout)findViewById(R.id.ll);
        mEnterBtn = (Button)findViewById(R.id.enterBtn);
        mNicName = (EditText) findViewById(R.id.nicName);

        mEnterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RoomList.class);
                intent.putExtra("nicName", mNicName.getText().toString());
                startActivity(intent);
            }
        });// *************** 입장시 닉네임을 RoomList클래스에 전달 ***************
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

                case R.id.enterBtn :
                    break;
            }
        }
    };

    private void hideKeyboard()
    {
        imm.hideSoftInputFromWindow(mNicName.getWindowToken(), 0);
    }

}
