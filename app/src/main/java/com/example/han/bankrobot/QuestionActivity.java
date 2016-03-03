package com.example.han.bankrobot;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.han.bankrobot.adapter.QaAdapter;
import com.example.han.bankrobot.model.QA_Model;
import com.litesuits.orm.db.assit.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class QuestionActivity extends AppCompatActivity {

    @Bind(R.id.id_question_toolbar)
    Toolbar mToolbar;
    @Bind(R.id.id_question_list)
    ListView mListView;
    @Bind(R.id.etSearch)
    TextView etSearch;
    @Bind(R.id.ivDeleteText)
    ImageView ivDeleteText;

    public static final int MSG_ALL = 0x1;  //输入框没有内容时
    public static final int MSG_PART = 0x2; //输入框有内容时

    private ArrayList<QA_Model> qa_models;
    private QaAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        ButterKnife.bind(this);
        initAction();
    }

    private void initAction() {
        setSupportActionBar(mToolbar);
        qa_models=new ArrayList<QA_Model>();
        QueryBuilder query = new QueryBuilder(QA_Model.class);
        qa_models = SpeechApp.sDb.query(query);
        mAdapter = new QaAdapter(QuestionActivity.this, qa_models);
        mListView.setAdapter(mAdapter);

        ivDeleteText.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                etSearch.setText("");
            }
        });
        etSearch.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    ivDeleteText.setVisibility(View.GONE);
                    handler.obtainMessage(MSG_ALL).sendToTarget();
                } else {
                    ivDeleteText.setVisibility(View.VISIBLE);
                    handler.obtainMessage(MSG_PART).sendToTarget();
                }
            }
        });

    }

    private void getAllItemFromOrm() {
        qa_models.clear();
        QueryBuilder query = new QueryBuilder(QA_Model.class);
        qa_models.addAll(SpeechApp.sDb.query(query));
    }

    private void getPartItemFromOrm() {
        qa_models.clear();
        QueryBuilder query = new QueryBuilder(QA_Model.class).where("question like ?",new String[]{"%"+etSearch.getText().toString()+"%"});
        qa_models.addAll(SpeechApp.sDb.query(query));
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ALL:
                    getAllItemFromOrm();
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("TAG", "adapter null");
                    }
                    break;
                case MSG_PART:
                    getPartItemFromOrm();
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("TAG", "adapter null");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @OnClick(R.id.qa_back)
    public void back() {
        this.finish();
    }

}
