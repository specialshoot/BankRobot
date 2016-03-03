package com.example.han.bankrobot.vitamio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

import com.example.han.bankrobot.R;

import io.vov.vitamio.Vitamio;

public class InitActivity extends Activity {
    public static final String FROM_ME = "fromVitamioInitActivity";
    public static final String EXTRA_MSG = "EXTRA_MSG";
    public static final String EXTRA_FILE = "EXTRA_FILE";
    private ProgressDialog mPD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        new AsyncTask<Object, Object, Object>() {
            @Override
            protected void onPreExecute() {
                mPD = new ProgressDialog(InitActivity.this);
                mPD.setCancelable(false);
                mPD.setMessage(getText(R.string.init_decoders));
                mPD.show();
            }

            @Override
            protected Object doInBackground(Object... params) {

                Vitamio.initialize(getApplicationContext());
                uiHandler.sendEmptyMessage(0);
                return null;
            }
        }.execute();
    }

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mPD.dismiss();
            Intent src = getIntent();
            Intent i = new Intent();
            i.setClassName(src.getStringExtra("package"), src.getStringExtra("className"));
            i.setData(src.getData());
            i.putExtras(src);
            i.putExtra(FROM_ME, true);
            startActivity(i);

            finish();
        }
    };
}
