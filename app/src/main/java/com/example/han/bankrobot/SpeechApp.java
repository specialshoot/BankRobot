package com.example.han.bankrobot;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.example.han.bankrobot.utils.FileUtils;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.litesuits.orm.LiteOrm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SpeechApp extends Application {

    private static SpeechApp mApplication;

    /**
     * OPlayer SD卡缓存路径
     */
    public static final String OPLAYER_CACHE_BASE = Environment.getExternalStorageDirectory() + "/oplayer";
    /**
     * 视频截图缓冲路径
     */
    public static final String OPLAYER_VIDEO_THUMB = OPLAYER_CACHE_BASE + "/thumb/";
    /**
     * 首次扫描
     */
    public static final String PREF_KEY_FIRST = "application_first";
    /**
     * 数据库
     */
    private static final String PACKAGE_NAME = "com.example.han.bankrobot";
    public static final String DB_PATH = "/data"
            + Environment.getDataDirectory().getAbsolutePath() + "/"
            + PACKAGE_NAME;  //在手机里存放数据库的位置
    private static final String DB_NAME = "bankqa.db";
    private String dbfile = DB_PATH + "/databases/" + DB_NAME;
    private String databasesDir = DB_PATH + "/databases/";
    private final int BUFFER_SIZE = 400000;
    public static Context sContext;
    public static LiteOrm sDb;

    @Override
    public void onCreate() {
        // 应用程序入口处调用,避免手机内存过小,杀死后台进程后通过历史intent进入Activity造成SpeechUtility对象为null
        // 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true"
        // 参数间使用“,”分隔。
        // 设置你申请的应用appid

        // 注意： appid 必须和下载的SDK保持一致，否则会出现10407错误

        mApplication = this;

        init();
        super.onCreate();
    }

    private void init() {
        //语音设置
        StringBuffer param = new StringBuffer();
        param.append("appid=" + getString(R.string.iflytek_id));
        param.append(",");
        // 设置使用v5+
        param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
        System.out.println("param -> " + param.toString());
        SpeechUtility.createUtility(SpeechApp.this, param.toString());

        System.out.println("dbfile -> " + dbfile);
        try {
            if (!(new File(dbfile).exists())) {//判断数据库文件是否存在，若不存在则执行导入，否则直接打开数据库
                System.out.println("不存在数据库,新建");
                InputStream is = this.getResources().openRawResource(
                        R.raw.bankqa); //欲导入的数据库
                File Dir = new File(databasesDir);
                if (!Dir.exists()) {
                    Dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(dbfile);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            }
            if (new File(dbfile).exists()) {
                if (sDb == null) {
                    sDb = LiteOrm.newSingleInstance(this, DB_NAME);
                }
                if (BuildConfig.DEBUG) {
                    sDb.setDebugged(true);
                }
                System.out.println("数据库创建成功");
            } else {
                System.out.println("数据库创建失败,写入错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("数据库创建失败");
        }
        //创建缓存目录
        FileUtils.createIfNoExists(OPLAYER_CACHE_BASE);
        FileUtils.createIfNoExists(OPLAYER_VIDEO_THUMB);
    }

    public static SpeechApp getApplication() {
        return mApplication;
    }

    public static Context getContext() {
        return mApplication;
    }

    /**
     * 销毁
     */
    public void destory() {
        mApplication = null;
    }
}
