package com.demo.monitor;

import org.MediaPlayer.PlayM4.Player;

import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.RealPlayCallBack;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * @author 金建强(ptma@163.com)
 * @version 2016-11-09 19:06
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private final static String    TAG              = "HIK";

    private ImageView              mPlay;
    private ProgressBar            mBar;
    private SurfaceView            mSurfaceView     = null;
    private int                    hikPort          = -1;            // playPort
    private int                    hikLoginId       = -1;
    private NET_DVR_DEVICEINFO_V30 hikDeviceInfo = null;
    private int                    hikPlayID        = -1;            // return by
                                                                     // NET_DVR_RealPlay_V30
    private int                    hikPlaybackID    = -1;            // return by
                                                                     // NET_DVR_PlayBackByTime


    private String                 strIP            = "60.12.79.165";
    private int                    nPort            = 8000;
    private String                 strUser          = "admin";
    private String                 strPsd           = "1q2w3e4r";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!initeSdk()) {
            this.finish();
            return;
        }

        if (!initeActivity()) {
            this.finish();
            return;
        }
    }

    // GUI的初始化
    private boolean initeActivity() {
        findViews();
        mSurfaceView.getHolder().addCallback(this);
        setListeners();
        return true;
    }

    private void setListeners() {
        mPlay.setOnClickListener(My_onClick);
    }

    private void findViews() {
        mPlay = (ImageView) findViewById(R.id.play);
        mBar = (ProgressBar) findViewById(R.id.load);
        mSurfaceView = (SurfaceView) findViewById(R.id.s_view);
    }

    // [1]
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        Log.i("sur", "surface is created" + hikPort);
        if (-1 == hikPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (surface.isValid()) {
            if (!Player.getInstance().setVideoWindow(hikPort, 0, holder)) {
                Log.e("sur", "Player setVideoWindow failed!");
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("sur", "Player setVideoWindow release!" + hikPort);
        if (-1 == hikPort) {
            return;
        }
        if (holder.getSurface().isValid()) {
            if (!Player.getInstance().setVideoWindow(hikPort, 0, null)) {
                Log.e("sur", "Player setVideoWindow failed!");
            }
        }
    }

    // [1]
    class myAsync1 extends AsyncTask<Void, Void, Void> {
        @Override
        // 必须要重写的方法
        protected Void doInBackground(Void... params) {
            Log.e("sync", "doInBackground");
            publishProgress();// 手动调用onProgressUpdate方法，传入进度值
            // 登录设备操作
            try {
                if (hikLoginId < 0) {
                    // login on the device
                    hikLoginId = loginDevice();
                    if (hikLoginId < 0) {
                        Log.e("login", "This device logins failed!");
                        return null;
                    }
                    // get instance of exception callback and set
                    ExceptionCallBack oexceptionCbf = getExceptiongCbf();
                    if (oexceptionCbf == null) {
                        Log.e("login", "ExceptionCallBack object is failed!");
                        return null;
                    }

                    if (!HCNetSDK.getInstance().NET_DVR_SetExceptionCallBack(oexceptionCbf)) {
                        Log.e("login", "NET_DVR_SetExceptionCallBack is failed!");
                        return null;
                    }

                    Log.i("login",
                            "Login sucess ****************************1***************************");
                    Thread.sleep(3000);
                } else {
                    if (!HCNetSDK.getInstance().NET_DVR_Logout_V30(hikLoginId)) {
                        Log.e("login", " NET_DVR_Logout is failed!");
                        return null;
                    }
                    hikLoginId = -1;
                }
            } catch (Exception err) {
                Log.e("login", "error: " + err.toString());
            }
            return null;
        }

        @Override
        // 预处理
        protected void onPreExecute() {
            Log.e("sync", "onPreExecute");
            mBar.setVisibility(View.VISIBLE);
            mPlay.setVisibility(View.GONE);
            super.onPreExecute();
        }

        @Override
        // 输出异步处理后结果
        protected void onPostExecute(Void aVoid) {
            Log.e("sync", "onPostExecute");
            mBar.setVisibility(View.GONE);
            if (hikLoginId < 0) {
                Log.e("login", "please login on device first");
                return;
            } else {
                startSinglePreview();
            }
            super.onPostExecute(aVoid);
        }

        @Override
        // 获取进度，更新进度条
        protected void onProgressUpdate(Void... values) {
            Log.e("sync", "onProgressUpdate");
            super.onProgressUpdate(values);
        }
    }

    private View.OnClickListener My_onClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play:
                    myAsync1 task = new myAsync1();// 实例化异步任务
                    task.execute();// 执行
                    break;
            }
        }
    };

    // class drawThread implements Runnable {
    // @Override
    // public void run() {
    // holder = mSurfaceView.getHolder();
    // holder.setFormat(PixelFormat.TRANSLUCENT);//半透明
    // if (-1 == hikPort)
    // {
    // return;
    // }
    // Surface surface = holder.getSurface();
    // if (surface.isValid()) {
    // if (!Player.getInstance().setVideoWindow(hikPort, 0, holder)) {
    // Log.e("sur", "Player setVideoWindow failed!");
    // }
    // }
    //
    // }
    // }

    private boolean initeSdk() {
        // init net sdk
        if (!HCNetSDK.getInstance().NET_DVR_Init()) {
            Log.e("sur", "HCNetSDK init is failed!");
            return false;
        }
        HCNetSDK.getInstance().NET_DVR_SetLogToFile(3, "/mnt/sdcard/sdklog/", true);
        return true;
    }


    private int loginDevice() {
        // get instance
        hikDeviceInfo = new NET_DVR_DEVICEINFO_V30();
        if (null == hikDeviceInfo) {
            Log.e("login", "HKNetDvrDeviceInfoV30 new is failed!");
            return -1;
        }

        // call NET_DVR_Login_v30 to login on, port 8000 as default
        int loginId = HCNetSDK.getInstance().NET_DVR_Login_V30(strIP, nPort, strUser, strPsd,
            hikDeviceInfo);
        if (loginId < 0) {
            Log.e("login", HikVisionError.errorMsg(HCNetSDK.getInstance().NET_DVR_GetLastError()));
            return -1;
        }
        // if(hikDeviceInfo.byChanNum > 0)
        // {
        // m_iStartChan = hikDeviceInfo.byStartChan;
        // m_iChanNum = hikDeviceInfo.byChanNum;
        // }
        // else if(hikDeviceInfo.byIPChanNum > 0)
        // {
        // m_iStartChan = hikDeviceInfo.byStartDChan;
        // m_iChanNum = hikDeviceInfo.byIPChanNum + hikDeviceInfo.byHighDChanNum *
        // 256;
        // }
        Log.i("login", "NET_DVR_Login is Successful!");
        Log.i("login", "下面是设备信息************************");
        Log.i("login", "通道开始=" + hikDeviceInfo.byStartChan);
        Log.i("login", "通道个数=" + hikDeviceInfo.byChanNum);
        Log.i("login", "设备类型=" + hikDeviceInfo.byDVRType);
        Log.i("login", "ip通道个数=" + hikDeviceInfo.byIPChanNum);

        return loginId;
    }


    private void startSinglePreview() {
        // if(hikPlaybackID >= 0)
        // {
        // Log.i("play", "Please stop palyback first");
        // return ;
        // }
        RealPlayCallBack fRealDataCallBack = getRealPlayerCbf();
        if (fRealDataCallBack == null) {
            Log.e("play", "fRealDataCallBack object is failed!");
            return;
        }


        NET_DVR_PREVIEWINFO previewInfo = new NET_DVR_PREVIEWINFO();
        previewInfo.lChannel = 33;
        previewInfo.dwStreamType = 1; // substream
        previewInfo.bBlocked = 1;
        // HCNetSDK start preview
        hikPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(hikLoginId, previewInfo,
                fRealDataCallBack);
        if (hikPlayID < 0) {
            Log.e("play", HikVisionError.errorMsg(HCNetSDK.getInstance().NET_DVR_GetLastError()));
            return;
        }

        Log.i("play", "NetSdk Play sucess ***********************3***************************");
    }

    // 异常回调
    private ExceptionCallBack getExceptiongCbf() {
        ExceptionCallBack oExceptionCbf = new ExceptionCallBack() {
            public void fExceptionCallBack(int iType, int iUserID, int iHandle) {
                System.out.println("recv exception, type:" + iType);
            }
        };
        return oExceptionCbf;
    }

    private RealPlayCallBack getRealPlayerCbf() {
        RealPlayCallBack cbf = new RealPlayCallBack() {
            public void fRealDataCallBack(int iRealHandle, int iDataType, byte[] pDataBuffer,
                    int iDataSize) {
                // player channel 1
                MainActivity.this.processRealData(1, iDataType, pDataBuffer, iDataSize,
                        Player.STREAM_REALTIME);
            }
        };
        return cbf;
    }


    public void processRealData(int iPlayViewNo, int iDataType, byte[] pDataBuffer, int iDataSize,
            int iStreamMode) {

        if (HCNetSDK.NET_DVR_SYSHEAD == iDataType) {
            if (hikPort >= 0) {
                return;
            }
            hikPort = Player.getInstance().getPort();
            if (hikPort == -1) {
                Log.e("play", HikVisionError.errorMsg(Player.getInstance().getLastError(hikPort)));
                return;
            }
            Log.i("play", "getPort succ with: " + hikPort);
            if (iDataSize > 0) {
                if (!Player.getInstance().setStreamOpenMode(hikPort, iStreamMode)) // set stream
                                                                                   // mode
                {
                    Log.e("play", "setStreamOpenMode failed");
                    return;
                }
                if (!Player.getInstance().openStream(hikPort, pDataBuffer, iDataSize,
                        2 * 1024 * 1024)) // open stream
                {
                    Log.e("play", "openStream failed");
                    return;
                }
                if (!Player.getInstance().play(hikPort, mSurfaceView.getHolder())) {
                    Log.e("play", "play failed");
                    return;
                }
                if (!Player.getInstance().playSound(hikPort)) {
                    Log.e("play",
                            HikVisionError.errorMsg(Player.getInstance().getLastError(hikPort)));
                    return;
                }
            }
        } else {
            if (!Player.getInstance().inputData(hikPort, pDataBuffer, iDataSize)) {
                // Log.e(TAG, "inputData failed with: " +
                // Player.getInstance().getLastError(hikPort));
                for (int i = 0; i < 4000; i++) {
                    if (!Player.getInstance().inputData(hikPort, pDataBuffer, iDataSize))
                        Log.e("play", HikVisionError
                                .errorMsg(Player.getInstance().getLastError(hikPort)));
                    else
                        break;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }
    }

}
