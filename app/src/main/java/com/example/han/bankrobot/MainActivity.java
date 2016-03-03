package com.example.han.bankrobot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.han.bankrobot.service.MediaScannerService;
import com.example.han.bankrobot.speech.setting.IatSettings;
import com.example.han.bankrobot.speech.setting.TtsSettings;
import com.example.han.bankrobot.speech.util.JsonParser;
import com.example.han.bankrobot.utils.ToastUtils;
import com.example.han.bankrobot.view.FaceMask;
import com.example.han.bankrobot.view.MyTextView;
import com.example.han.bankrobot.vitamio.LibsChecker;
import com.faceplusplus.api.FaceDetecter;
import com.facepp.http.HttpRequests;
import com.friendlyarm.AndroidSDK.HardwareControler;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "MainActivity";
    public static final int VideoCode = 1;
    private static final String SEEK_POSITION_KEY = "SEEK_POSITION_KEY";
    private static final String VIDEO_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
    private Toast mToast;
    @Bind(R.id.id_main_toolbar)
    Toolbar id_main_toolbar;
    @Bind(R.id.id_main_scrolltext)
    MyTextView mText;
    //视频播放
    @Bind(R.id.surface_view)
    VideoView mVideoView; //这里的VideoView是io.vov.vitamio.widget里面的VideoView
    @Bind(R.id.operation_volume_brightness)
    View mVolumeBrightnessLayout;   //调节明亮度
    @Bind(R.id.operation_bg)
    ImageView mOperationBg; //音量
    @Bind(R.id.operation_percent)
    ImageView mOperationPercent;    //进度条
    @Bind(R.id.video_loading)
    View mLoadingView;
    @Bind(R.id.id_main_voice_fab)
    FloatingActionButton mFab;
    @Bind(R.id.haveface)
    ImageView haveface;
    //    @Bind(R.id.camera_preview)
//    SurfaceView camerasurface;
//    @Bind(R.id.mask)
//    FaceMask mask;
    private boolean needResume;//是否需要自动恢复播放，用于自动暂停，恢复播放
    private AudioManager mAudioManager; //声音管理器
    private int mMaxVolume; //最大声音
    private int mVolume = -1;   //当前声音
    private float mBrightness = -1f;    //当前亮度
    private int mLayout = VideoView.VIDEO_LAYOUT_ZOOM;  //当前缩放模式
    //private GestureDetector mGestureDetector;   //手势
    //private MediaController mMediaController;
    private String mCurrentURI = "";

    //科大语音
    private String speechResult = "";
    //科大讯飞相应变量
    private String mEngineType = SpeechConstant.TYPE_CLOUD; //语音类型,云端(用于语音听写、语音合成)
    //语音听写相关参数Iat
    SpeechRecognizer mIat;    //语音听写对象
    RecognizerDialog mIatDialog;  //语音听写对话框
    private SharedPreferences mIatSharedPreferences;//语音听写sharedpreferences
    //语音合成相关参数Tts
    private SpeechSynthesizer mTts;//语音合成对象
    public static String voicer = "xiaoyan";//默认发言人
    private int mPercentForBuffering = 0;//缓冲进度
    private int mPercentForPlaying = 0;//播放速度
    private SharedPreferences mTtsSharedPreferences;//语音合成sharedpreferences
    private Handler handlervoice = new Handler() {  //语音播放handler
        @Override
        public void handleMessage(Message msg) {
            String detail = (String) msg.obj;
            setTtsParam();
            int code = mTts.startSpeaking(detail, mTtsListener);
            if (code != ErrorCode.SUCCESS) {
                showTip("语音合成失败,错误码: " + code);
                if (mVideoView != null && !mVideoView.isPlaying()) {
                    mVideoView.start();
                }
            }
        }
    };
    //语音唤醒相关参数
    private VoiceWakeuper mIvw;
    private String resultString;//语音唤醒结果内容
    public static int curThresh = -20;    //唤醒门限值

    //人脸检测
    private FaceDetecter facedetecter = null;
    private HandlerThread handleThread = null;
    private Handler detectHandler = null;
    private Runnable detectRunnalbe = null;
    private Camera camera = null;
    @Bind(R.id.camera_preview)
    SurfaceView camerasurface = null;
    //    @Bind(R.id.mask)
//    FaceMask mask = null;
    HttpRequests request = null;// 在线api
    private int width = 0;
    private int height = 0;

    //串口
    private final int MAXLINES = 200;
    private StringBuilder remoteData = new StringBuilder(256 * MAXLINES);
    private String devName = "/dev/s3c2410_serial3";    //串口名
    private int speed = 115200;
    private int dataBits = 8;
    private int stopBits = 1;
    private int devfd = -1;
    private final int BUFSIZE = 512;
    private byte[] buf = new byte[BUFSIZE];
    private Timer timer = new Timer();
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (HardwareControler.select(devfd, 0, 0) == 1) {
                        int retSize = HardwareControler.read(devfd, buf, BUFSIZE);
                        if (retSize > 0) {
                            String str = new String(buf, 0, retSize);
                            remoteData.append(str);
                            //收到的信息从这里处理

                        }
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };
    private TimerTask task = new TimerTask() {
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(id_main_toolbar);

        initAction();
    }

    private void initAction() {
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mText.setFocusable(true);
        mText.setFocusableInTouchMode(true);
        if (!LibsChecker.checkVitamioLibs(this, R.string.init_decoders))
            return;
        OPreference pref = new OPreference(this);
        //	首次运行，扫描SD卡
        if (pref.getBoolean(SpeechApp.PREF_KEY_FIRST, true)) {
            getApplicationContext().startService(new Intent(getApplicationContext(), MediaScannerService.class).putExtra(MediaScannerService.EXTRA_DIRECTORY, Environment.getExternalStorageDirectory().getAbsolutePath()));
        }
        mVideoView.setOnCompletionListener(new io.vov.vitamio.MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(io.vov.vitamio.MediaPlayer mediaPlayer) {
                ToastUtils.showShort(getApplicationContext(), "循环播放");
                if (!mCurrentURI.equals("")) {
                    mVideoView.seekTo(0);
                    mVideoView.start();
                }
            }
        });
        mVideoView.setOnInfoListener(new io.vov.vitamio.MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(io.vov.vitamio.MediaPlayer mediaPlayer, int i, int download_rate) {
                switch (i) {
                    case io.vov.vitamio.MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        //开始缓存，暂停播放
                        if (isPlaying()) {
                            stopPlayer();
                            needResume = true;
                        }
                        mLoadingView.setVisibility(View.VISIBLE);
                        break;
                    case io.vov.vitamio.MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        //缓存完成，继续播放
                        if (needResume)
                            startPlayer();
                        mLoadingView.setVisibility(View.GONE);
                        break;
                    case io.vov.vitamio.MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                        //显示 下载速度
                        Log.v(TAG, "Download Rate:" + download_rate);
                        //mListener.onDownloadRateChanged(arg2);
                        break;
                }
                return true;
            }
        });
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        //设置显示名称
//        mMediaController = new MediaController(this);
//        mMediaController.setFileName(mVideoName);
//        mVideoView.setMediaController(mMediaController);
        mVideoView.requestFocus();

//        mGestureDetector = new GestureDetector(this, new MyGestureListener());//手势监听器
        /*****************************语音听写初始化*****************************/
        //初始化语音听写
        mIat = com.iflytek.cloud.SpeechRecognizer.createRecognizer(this, initListener);
        //初始化语音听写对话框
        mIatDialog = new RecognizerDialog(this, initListener);
        mIatSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        /*****************************语音合成初始化*****************************/
        mTts = SpeechSynthesizer.createSynthesizer(this, initListener);
        mTtsSharedPreferences = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        /*****************************语音唤醒初始化*****************************/
        DoWake();   //唤醒打开

        /********************************face++********************************/
        //RelativeLayout.LayoutParams para = new RelativeLayout.LayoutParams(480, 800);
        handleThread = new HandlerThread("dt");
        handleThread.start();
        detectHandler = new Handler(handleThread.getLooper());
        //para.addRule(RelativeLayout.CENTER_IN_PARENT);
        //camerasurface.setLayoutParams(para);
        //mask.setLayoutParams(para);
        camerasurface.getHolder().addCallback(this);
        camerasurface.setKeepScreenOn(true);

        facedetecter = new FaceDetecter();
        if (!facedetecter.init(this, "b38751bb5fecf7277d23c8528f4867d6")) {
            Log.e("diff", "有错误 ");
        }
        request = new HttpRequests("b38751bb5fecf7277d23c8528f4867d6",
                "ykbkr_XW2n6GR5dAbEpopZHnvUatls6A");
        facedetecter.setTrackingMode(true);
        //OpenPort();
    }

    /**
     * 视频是否在播放
     *
     * @return
     */
    private boolean isPlaying() {
        return mVideoView != null && mVideoView.isPlaying();
    }

    /**
     * 暂停视频播放
     */
    private void stopPlayer() {
        if (mVideoView != null)
            mVideoView.pause();
    }

    /**
     * 开始播放
     */
    private void startPlayer() {
        if (mVideoView != null)
            mVideoView.start();
    }

//    /**
//     * 手势检测双击及滑动
//     */
//    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
//
//        /**
//         * 双击
//         */
//        @Override
//        public boolean onDoubleTap(MotionEvent e) {
//            if (mLayout == VideoView.VIDEO_LAYOUT_ZOOM)
//                mLayout = VideoView.VIDEO_LAYOUT_ORIGIN;
//            else
//                mLayout++;
//            if (mVideoView != null)
//                mVideoView.setVideoLayout(mLayout, 0);
//            return true;
//        }
//
//        /**
//         * 滑动
//         */
//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//            float mOldX = e1.getX(), mOldY = e1.getY();
//            int y = (int) e2.getRawY();
//            Display disp = getWindowManager().getDefaultDisplay();
//            int windowWidth = disp.getWidth();
//            int windowHeight = disp.getHeight();
//
//            if (mOldX > windowWidth * 4.0 / 5)// 右边滑动
//                onVolumeSlide((mOldY - y) / windowHeight);
//            else if (mOldX < windowWidth / 5.0)// 左边滑动
//                onBrightnessSlide((mOldY - y) / windowHeight);
//
//            return super.onScroll(e1, e2, distanceX, distanceY);
//        }
//    }

    /**
     * 滑动改变声音大小
     *
     * @param percent
     */
    private void onVolumeSlide(float percent) {
        if (mVolume == -1) {
            mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (mVolume < 0)
                mVolume = 0;

            // 显示
            mOperationBg.setImageResource(R.drawable.video_volumn_bg);
            mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
        }

        int index = (int) (percent * mMaxVolume) + mVolume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;

        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

        // 变更进度条
        ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
        lp.width = findViewById(R.id.operation_full).getLayoutParams().width * index / mMaxVolume;
        mOperationPercent.setLayoutParams(lp);
    }

    /**
     * 滑动改变亮度
     *
     * @param percent
     */
    private void onBrightnessSlide(float percent) {
        if (mBrightness < 0) {
            mBrightness = getWindow().getAttributes().screenBrightness;
            if (mBrightness <= 0.00f)
                mBrightness = 0.50f;
            if (mBrightness < 0.01f)
                mBrightness = 0.01f;
            // 显示
            mOperationBg.setImageResource(R.drawable.video_brightness_bg);
            mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
        }
        WindowManager.LayoutParams lpa = getWindow().getAttributes();
        lpa.screenBrightness = mBrightness + percent;
        if (lpa.screenBrightness > 1.0f)
            lpa.screenBrightness = 1.0f;
        else if (lpa.screenBrightness < 0.01f)
            lpa.screenBrightness = 0.01f;
        getWindow().setAttributes(lpa);

        ViewGroup.LayoutParams lp = mOperationPercent.getLayoutParams();
        lp.width = (int) (findViewById(R.id.operation_full).getLayoutParams().width * lpa.screenBrightness);
        mOperationPercent.setLayoutParams(lp);
    }

    /**
     * 手势结束
     */
    private void endGesture() {
        mVolume = -1;
        mBrightness = -1f;

        // 隐藏
        mDismissHandler.removeMessages(0);
        mDismissHandler.sendEmptyMessageDelayed(0, 500);
    }

    /**
     * 定时隐藏图标
     */
    private Handler mDismissHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mVolumeBrightnessLayout.setVisibility(View.GONE);
        }
    };

    /**
     * 初始化语音的监听
     */
    private InitListener initListener = new InitListener() {
        @Override
        public void onInit(int code) {

            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.v(TAG, "初始化失败，错误码：" + code);
            } else {
                // 如果有语音合成,初始化成功,之后可以调用startSpeaking方法
                // 注:有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 设置语音听写参数
     */
    private void setIatParams() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        String lag = mIatSharedPreferences.getString("iat_language_preference", "mandarin");
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        // 设置语音后端点
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
        // 设置标点符号
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
        // 设置音频保存路径
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/iflytek/wavaudio.pcm");
    }

    /**
     * 设置语音合成参数
     */
    private void setTtsParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            //设置使用云端引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
        } else {
            //设置使用本地引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            //设置发音人资源路径
            mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicer);
        }
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, mTtsSharedPreferences.getString("speed_preference", "50"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, mTtsSharedPreferences.getString("pitch_preference", "50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, mTtsSharedPreferences.getString("volume_preference", "50"));
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mTtsSharedPreferences.getString("stream_preference", "3"));

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

    /**
     * 获取发音人资源路径
     *
     * @return
     */
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/" + MainActivity.voicer + ".jet"));
        return tempBuffer.toString();
    }

    /**
     * 带UI听写监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        @Override
        public void onResult(RecognizerResult recognizerResult, boolean isLast) {
            Log.d(TAG, "recognizer result：" + recognizerResult.getResultString());
            String text = JsonParser.parseIatResult(recognizerResult.getResultString());
            speechResult += text;
            if (isLast) {
                showTip(speechResult);
                DoTuring(speechResult);
                setFabClickable();
                DoWake();
            }
        }

        @Override
        public void onError(SpeechError speechError) {
            DoWake();
            if (mVideoView != null && !mVideoView.isPlaying()) {
                mVideoView.start();
                setFabClickable();
            }
        }
    };

    /**
     * 不带UI听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        /**
         * 开始说话
         */
        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Log.v(TAG, "开始说话");
        }

        /**
         * 返回错误
         * @param error
         */
        @Override
        public void onError(SpeechError error) {
            Log.v(TAG, error.getPlainDescription(true));
            showTip(error.getPlainDescription(true));
        }

        /**
         * 结束说话
         */
        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        /**
         * 语音识别结果.过程是累加的,当last为true的时候代表真正说话完成
         * @param results
         * @param isLast
         */
        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            String text = JsonParser.parseIatResult(results.getResultString());
            speechResult += text;
            if (isLast) {
                //TODO 最后的结果
                Log.v(TAG, speechResult);
            }
        }

        /**
         * 音量改变
         * @param volume
         * @param data
         */
        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                Log.d(TAG, "session id =" + sid);
            }
        }
    };

    /**
     * 语音合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            showTip("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成");
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
            if (mVideoView != null && !mVideoView.isPlaying()) {
                mVideoView.start();
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    /**
     * 设置语音唤醒监听
     */
    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            try {
                String text = result.getResultString();
                JSONObject object;
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 " + text);
                buffer.append("\n");
                buffer.append("【操作类型】" + object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】" + object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】" + object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】" + object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】" + object.optString("eos"));
                resultString = buffer.toString();
                //destroy是为了下面的说话，语音识别时必须禁用唤醒功能
                mIvw = VoiceWakeuper.getWakeuper();
                if (mIvw != null) {
                    mIvw.destroy();
                }
                try {
                    //先停止说话
                    mTts.stopSpeaking();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mVideoView.pause();
                DoListener();
            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            Log.v(TAG, resultString);
        }

        @Override
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

        @Override
        public void onBeginOfSpeech() {
            showTip("开始说话");
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {

        }

        @Override
        public void onVolumeChanged(int volume) {
            // TODO Auto-generated method stub

        }
    };

    /**
     * 语音识别
     */
    private void DoListener() {
        speechResult = "";
        setIatParams();
        mIatDialog.setListener(mRecognizerDialogListener);
        mIatDialog.show();
        showTip(getString(R.string.text_begin));
    }

    /**
     * 唤醒操作
     */
    private void DoWake() {
        // 加载识唤醒地资源，resPath为本地识别资源路径
        StringBuffer param = new StringBuffer();
        String resPath = ResourceUtil.generateResourcePath(MainActivity.this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + getString(R.string.iflytek_id) + ".jet");
        param.append(SpeechConstant.IVW_RES_PATH + "=" + resPath);
        param.append("," + ResourceUtil.ENGINE_START + "=" + SpeechConstant.ENG_IVW);
        boolean ret = SpeechUtility.getUtility().setParameter(ResourceUtil.ENGINE_START, param.toString());
        if (!ret) {
            Log.d(TAG, "启动本地引擎失败！");
        }
        try {
            // 初始化唤醒对象
            mIvw = VoiceWakeuper.createWakeuper(this, null);
            //非空判断，防止因空指针使程序崩溃
            mIvw = VoiceWakeuper.getWakeuper();
            if (mIvw != null) {
                resultString = "";
                // 清空参数
                mIvw.setParameter(SpeechConstant.PARAMS, null);
                /**
                 * 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
                 * 示例demo默认设置第一个唤醒词，建议开发者根据定制资源中唤醒词个数进行设置
                 */
                mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:"
                        + curThresh);
                // 设置唤醒模式
                mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
                // 设置持续进行唤醒
                mIvw.setParameter(SpeechConstant.KEEP_ALIVE, "1");
                mIvw.startListening(mWakeuperListener);
            } else {
                showTip("唤醒未初始化");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showTip("唤醒未初始化");
        }
    }

    /**
     * 语音播报
     *
     * @param msg 语音播报的内容
     */
    private void DoSpeak(String msg) {
        Message message = handlervoice.obtainMessage();
        message.obj = msg;
        handlervoice.sendMessage(message);
    }

    /**
     * 图灵机器人
     *
     * @param msg 传入的字符串
     */
    private void DoTuring(String msg) {
        try {
            String INFO = URLEncoder.encode(msg, "utf-8");
            String url = getString(R.string.turing_api_address) + "?key=" + getString(R.string.turing_api_key) + "&info=" + INFO;
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(new StringRequest(url, new Response.Listener<String>() {
                @Override
                public void onResponse(String s) {
                    Log.v(TAG, getTuringText(s));
                    DoSpeak(getTuringText(s));
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    DoSpeak("您的问题好深奥,我无法回答");
                }
            }));
        } catch (UnsupportedEncodingException e) {
            DoSpeak("您的问题好深奥,我无法回答");
        }
    }

    /**
     * 得到图灵返回的text字段
     * 图灵返回json字符串信息见http://www.tuling123.com/html/doc/api.html#jiekoudizhi
     *
     * @param msg
     * @return
     */
    private String getTuringText(String msg) {
        try {
            com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(msg);
            String text = jsonObject.getString("text");
            return text;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 打开文件夹选择视频文件
     */
    @OnClick(R.id.id_main_videoChoose)
    public void chooseVideo() {
        Intent intent = new Intent(MainActivity.this, VideoChooseActivity.class);
        startActivityForResult(intent, VideoCode);
    }

    /**
     * 处理询问业务
     */
    @OnClick(R.id.id_main_businessAsk)
    public void businessAsk() {
        Intent intent=new Intent(MainActivity.this,QuestionActivity.class);
        startActivity(intent);
    }

    /**
     * 处理业务办理
     */
    @OnClick(R.id.id_main_businessHandle)
    public void businessHandle() {

    }

    @OnClick(R.id.id_main_voice_fab)
    public void getVoice() {
        try {
            setFabUnClickable();
            mVideoView.pause();
            if (mTts.isSpeaking()) {
                mTts.stopSpeaking();
            }
            if (mIat.isListening()) {
                mIat.cancel();
            }
            if (mIvw.isListening()) {
                mIvw.stopListening();
            }
            DoListener();   //语音识别
        } catch (Exception e) {
            e.printStackTrace();
            setFabClickable();
        }
    }

    @OnClick(R.id.id_main_preview_camera)
    public void preview_camera() {
        try {
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.id_main_close_camera)
    public void close_camera() {
        try {
            camera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setFabClickable() {
        mFab.setBackgroundTintList(getResources().getColorStateList(R.color.main_blue_dark_arrow));
        mFab.setClickable(true);
    }

    private void setFabUnClickable() {
        mFab.setBackgroundTintList(getResources().getColorStateList(R.color.md_grey_800));
        mFab.setClickable(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState Position=" + mVideoView.getCurrentPosition());
    }

    @Override
    protected void onRestoreInstanceState(Bundle outState) {
        super.onRestoreInstanceState(outState);
    }

    private void switchTitleBar(boolean show) {
        android.support.v7.app.ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            if (show) {
                supportActionBar.show();
            } else {
                supportActionBar.hide();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ");
        if (mVideoView != null) {
            mVideoView.pause();
        }
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.resume();
        }

        camera = Camera.open(0);
        Camera.Parameters para = camera.getParameters();
        para.setPreviewSize(width, height);
        camera.setParameters(para);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放语音合成
        if (mIat != null) {
            mIat.cancel();
            mIat.destroy();
        }
        if (mTts != null) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {
            mIvw.cancel();
            mIvw.destroy();
        }
        //释放mVideoView
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }

        if (facedetecter != null) {
            facedetecter.release(this);
            handleThread.quit();
        }
    }

//    /**
//     * 手势调节视频亮度及音量
//     * @param event
//     * @return
//     */
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (mGestureDetector.onTouchEvent(event)) {
//            return true;
//        }
//
//        // 处理手势结束
//        switch (event.getAction() & MotionEvent.ACTION_MASK) {
//            case MotionEvent.ACTION_UP:
//                endGesture();
//                break;
//        }
//        return super.onTouchEvent(event);
//    }

    /************************
     * 人脸识别
     ***********************/
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
//        camera = Camera.open(0);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        camera.setDisplayOrientation(90);
        camera.startPreview();
        camera.setPreviewCallback(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        camera.setPreviewCallback(null);
        detectHandler.post(new Runnable() {

            @Override
            public void run() {
                Camera.Size size = camera.getParameters().getPreviewSize(); //获取预览大小
                width = size.width;  //宽度
                height = size.height;   //高度
                Log.v("facesize", "width -> " + width + " ; height -> " + height);
                byte[] ori = new byte[width * height];
                int is = 0;
//                for (int x = width - 1; x >= 0; x--) {
//                    for (int y = height - 1; y >= 0; y--) {
//                        ori[is] = data[x * height + y];
//                        is++;
//                    }
//                }
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        ori[is] = data[x * height + y];
                        is++;
                    }
                }
                final FaceDetecter.Face[] faceinfo = facedetecter.findFaces(ori, width,
                        height);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //mask.setFaceInfo(faceinfo);
                        if (faceinfo != null && faceinfo.length != 0) {
                            Log.v("facenum", faceinfo.length + "个脸");
                            haveface.setVisibility(View.VISIBLE);
                        } else {
                            haveface.setVisibility(View.GONE);
                        }
                    }
                });
                try {
                    MainActivity.this.camera.setPreviewCallback(MainActivity.this);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 友善之臂串口通信
     */
    private void OpenPort() {
        devfd = HardwareControler.openSerialPort(devName, speed, dataBits, stopBits);
        if (devfd >= 0) {
            timer.schedule(task, 0, 500);
        } else {
            devfd = -1;
            ToastUtils.showShort(MainActivity.this, "Fail to open " + devName + "!");
        }
    }

    /**
     * 串口发送数据
     *
     * @param str 要发送的数据
     */
    private void SendMessageFromPort(String str) {
        if (str.length() > 0) {
            if (str.charAt(str.length() - 1) != '\n') {
                str = str + "\n";
            }
            int ret = HardwareControler.write(devfd, str.getBytes());
            if (ret > 0) {
                ToastUtils.showShort(this, "Success To Send");
            } else {
                ToastUtils.showShort(this, "Fail To Send!");
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mVideoView != null)
            mVideoView.setVideoLayout(mLayout, 0);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case VideoCode:
                String mCurrentDir = data.getExtras().getString("dir");
                ArrayList mImgs = data.getStringArrayListExtra("video");
                System.out.println("当前目录:" + mCurrentDir);
                for (int i = 0; i < mImgs.size(); i++) {
                    System.out.println(mImgs.get(i));
                }

                if (mImgs != null && !mCurrentDir.equals("")) {
                    try {
                        if (mVideoView != null) {
                            if (mImgs.isEmpty()) {
                                mVideoView.pause();
                            } else {
                                String url = mImgs.get(0).toString();
                                int position = url.lastIndexOf("/");  //记录最后一次/出现的位置,方便提取文件名
                                mCurrentURI = url;
                                mVideoView.setVideoPath(mCurrentURI);
                                //mMediaController.setFileName(url.substring(position + 1));
                                mVideoView.start();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.main_voice) {
//
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
