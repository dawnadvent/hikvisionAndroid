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

    private ImageView              mPlay;
    private ProgressBar            mBar;
    private SurfaceView            mSurfaceView           = null;
    private int                    m_iPort                = -1;            // playPort
    private int                    m_iLogID               = -1;            // return by
                                                                           // NET_DVR_Login_v30
    private NET_DVR_DEVICEINFO_V30 m_oNetDvrDeviceInfoV30 = null;
    private int                    m_iPlayID              = -1;            // return by
                                                                           // NET_DVR_RealPlay_V30
    private int                    m_iPlaybackID          = -1;            // return by
                                                                           // NET_DVR_PlayBackByTime


    private String                 strIP                  = "60.12.79.165";
    private int                    nPort                  = 8000;
    private String                 strUser                = "admin";
    private String                 strPsd                 = "1q2w3e4r";


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
        Log.i("sur", "surface is created" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (surface.isValid()) {
            if (!Player.getInstance().setVideoWindow(m_iPort, 0, holder)) {
                Log.e("sur", "Player setVideoWindow failed!");
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("sur", "Player setVideoWindow release!" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        if (holder.getSurface().isValid()) {
            if (!Player.getInstance().setVideoWindow(m_iPort, 0, null)) {
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
                if (m_iLogID < 0) {
                    // login on the device
                    m_iLogID = loginDevice();
                    if (m_iLogID < 0) {
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
                    // whether we have logout
                    if (!HCNetSDK.getInstance().NET_DVR_Logout_V30(m_iLogID)) {
                        Log.e("login", " NET_DVR_Logout is failed!");
                        return null;
                    }
                    m_iLogID = -1;
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
            if (m_iLogID < 0) {
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
    // if (-1 == m_iPort)
    // {
    // return;
    // }
    // Surface surface = holder.getSurface();
    // if (surface.isValid()) {
    // if (!Player.getInstance().setVideoWindow(m_iPort, 0, holder)) {
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
        m_oNetDvrDeviceInfoV30 = new NET_DVR_DEVICEINFO_V30();
        if (null == m_oNetDvrDeviceInfoV30) {
            Log.e("login", "HKNetDvrDeviceInfoV30 new is failed!");
            return -1;
        }

        // call NET_DVR_Login_v30 to login on, port 8000 as default
        int iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(strIP, nPort, strUser, strPsd,
                m_oNetDvrDeviceInfoV30);
        if (iLogID < 0) {
            Log.e("login",
                    "NET_DVR_Login is failed!Err:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return -1;
        }
        // if(m_oNetDvrDeviceInfoV30.byChanNum > 0)
        // {
        // m_iStartChan = m_oNetDvrDeviceInfoV30.byStartChan;
        // m_iChanNum = m_oNetDvrDeviceInfoV30.byChanNum;
        // }
        // else if(m_oNetDvrDeviceInfoV30.byIPChanNum > 0)
        // {
        // m_iStartChan = m_oNetDvrDeviceInfoV30.byStartDChan;
        // m_iChanNum = m_oNetDvrDeviceInfoV30.byIPChanNum + m_oNetDvrDeviceInfoV30.byHighDChanNum *
        // 256;
        // }
        Log.i("login", "NET_DVR_Login is Successful!");

        return iLogID;
    }


    private void startSinglePreview() {
        // if(m_iPlaybackID >= 0)
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
        m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(m_iLogID, previewInfo,
                fRealDataCallBack);
        if (m_iPlayID < 0) {
            Log.e("play", "NET_DVR_RealPlay is failed!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
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
            if (m_iPort >= 0) {
                return;
            }
            m_iPort = Player.getInstance().getPort();
            if (m_iPort == -1) {
                Log.e("play",
                        "getPort is failed with: " + Player.getInstance().getLastError(m_iPort));
                return;
            }
            Log.i("play", "getPort succ with: " + m_iPort);
            if (iDataSize > 0) {
                if (!Player.getInstance().setStreamOpenMode(m_iPort, iStreamMode)) // set stream
                                                                                   // mode
                {
                    Log.e("play", "setStreamOpenMode failed");
                    return;
                }
                if (!Player.getInstance().openStream(m_iPort, pDataBuffer, iDataSize,
                        2 * 1024 * 1024)) // open stream
                {
                    Log.e("play", "openStream failed");
                    return;
                }
                if (!Player.getInstance().play(m_iPort, mSurfaceView.getHolder())) {
                    Log.e("play", "play failed");
                    return;
                }
                if (!Player.getInstance().playSound(m_iPort)) {
                    Log.e("play", "playSound failed with error code:"
                            + Player.getInstance().getLastError(m_iPort));
                    return;
                }
            }
        } else {
            if (!Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize)) {
                // Log.e(TAG, "inputData failed with: " +
                // Player.getInstance().getLastError(m_iPort));
                for (int i = 0; i < 4000; i++) {
                    if (!Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize))
                        Log.e("play", "inputData failed with: "
                                + Player.getInstance().getLastError(m_iPort));
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
