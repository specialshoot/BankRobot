package com.example.han.bankrobot;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.han.bankrobot.adapter.VideoAdapter;
import com.example.han.bankrobot.model.FolderBean;
import com.example.han.bankrobot.utils.ToastUtils;
import com.example.han.bankrobot.view.ListImageDirPopupWindow;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoChooseActivity extends AppCompatActivity {

    @Bind(R.id.id_video_toolbar)
    Toolbar toolbar;

    @Bind(R.id.id_gridView)
    GridView mGridView;
    @Bind(R.id.id_bottom_ly)
    RelativeLayout mBottomLy;
    @Bind(R.id.id_dir_name)
    TextView mDirName;
    @Bind(R.id.id_dir_count)
    TextView mDirCount;

    private List<String> mImgs; //gridview数据集
    private File mCurrentDir;
    private int mMaxCount = 0;
    private ProgressDialog mProgressDialog;
    private VideoAdapter mImgAdapter;
    private ListImageDirPopupWindow mDirPopupWindow;

    private static final int DATA_LOADED = 0X110;

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {
                mProgressDialog.dismiss();
                data2View();    //绑定数据到View中
                initDirPopupWindow();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_choose);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        initDatas();
    }

    @OnClick(R.id.id_video_sure)
    void makesure() {
        ArrayList<String> list = new ArrayList<String>();
        for (Iterator it = mImgAdapter.mSeletedImg.iterator(); it.hasNext(); ) {
            list.add(it.next().toString());
        }

        if (mCurrentDir != null && !list.isEmpty()) {
            Intent intent = new Intent();
            intent.putExtra("dir", mCurrentDir.getAbsolutePath());
            intent.putStringArrayListExtra("video", list);
            setResult(MainActivity.VideoCode, intent);
        }
        finish();
    }

    @OnClick(R.id.id_bottom_ly)
    void bottom_ly_click() {
        mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
        mDirPopupWindow.showAsDropDown(mBottomLy, 0, 0);    //设置popupwindow显示的位置
        lightOff(); //popupWindow出现后背景变暗
    }

    protected void data2View() {
        if (mCurrentDir == null) {
            ToastUtils.showShort(this, "未扫描到任何视频");
            return;
        }

        mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.endsWith(".avi") || filename.endsWith(".mp4")) {
                    return true;
                }
                return false;
            }
        }));   //数组转化为List
        mImgAdapter = new VideoAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImgAdapter);

        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());
    }

    protected void initDirPopupWindow() {
        mDirPopupWindow = new ListImageDirPopupWindow(this, mFolderBeans);
        //做一个popupwindow消失后的一个效果
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });

        //更新popupWindow选中的文件夹
        mDirPopupWindow.setOnDirSelectedListener(new ListImageDirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelect(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".avi") || filename.endsWith(".mp4")) {
                            return true;
                        }
                        return false;
                    }
                }));

                mImgAdapter = new VideoAdapter(VideoChooseActivity.this, mImgs, mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(mImgAdapter);
                mDirCount.setText(mMaxCount + "");
                mDirName.setText(mCurrentDir.getName());
                mDirPopupWindow.dismiss();
            }
        });
    }

    protected void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    protected void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    /**
     * 利用ContentProvider扫描手机中的所有视频
     */
    private void initDatas() {
        mImgAdapter.mSeletedImg.clear();
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            ToastUtils.showShort(VideoChooseActivity.this, "存储卡不可用");
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");
        new Thread() {
            public void run() {
                Uri mVideoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = VideoChooseActivity.this.getContentResolver();
                Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);

                Set<String> mDirPaths = new HashSet<String>();    //用Set存储parent路径
                //由于很多文件有相同的上级目录,每次都要getParentFile后放到folderBean中造成重复,所以用一个HashSet

                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));  //视频路径
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) {
                        continue;
                    }
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;

                    if (mDirPaths.contains(dirPath)) {
                        continue;   //当前文件夹扫描过
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);
                        //到这里为止folderBean还缺少一个数量
                    }

                    //folderBean图片数量
                    if (parentFile.list() == null) {
                        continue;
                    }

                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if (filename.endsWith(".avi") || filename.endsWith(".mp4")) {
                                return true;
                            }
                            return false;
                        }
                    }).length;  //得到视频数量

                    folderBean.setCount(picSize);

                    mFolderBeans.add(folderBean);   //folderBean加入到列表中

                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }
                cursor.close(); //跑完while要关闭cursor
                mHandler.sendEmptyMessage(DATA_LOADED);   //通知handler扫描图片完成
            }
        }.start();
    }

}
