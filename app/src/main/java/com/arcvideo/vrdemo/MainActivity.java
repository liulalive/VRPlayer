package com.arcvideo.vrdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.arcvideo.MediaPlayer.ArcMediaPlayer;
import com.arcvideo.vrkit.VRConst;
import com.arcvideo.vrkit.VRVideoView;

import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends Activity {
    VRVideoView id_vr_videoview;
    protected ArcMediaPlayer mMediaPlayer;

    String mUrlRTSP = "rtsp://admin:bsoft123@10.0.4.74/h264/ch1/main/av_stream";
    //鉴权参数:需要app去获取和提供
    private String accessKey = "6c621b52-f1a";
    private String secretKey = "y0LF9gjTQ4atdmGGcsDk";
    private String appKey = "f358d95832534f7e84f4fae7c75bb62d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        id_vr_videoview = (VRVideoView) findViewById(R.id.id_vr_videoview);
        id_vr_videoview.setVideo3DMode(/*Video3DMode.TopBottom:*/VRConst.Video3DMode.LeftRight);

        mMediaPlayer = new ArcMediaPlayer();
        mMediaPlayer.setConfigFile(this, getFilesDir().getAbsolutePath()
                + "/MV3Plugin.ini");
        mMediaPlayer.validate(this, accessKey, secretKey, appKey);
        mMediaPlayer.reset();
        openFileStr();
    }

    @SuppressLint("NewApi")
    private void openFileStr() {
        boolean bValid = isInputTextValid(mUrlRTSP);
        if (!bValid) {
            Toast.makeText(MainActivity.this, "非法地址！", Toast.LENGTH_SHORT).show();
            return;
        }

        mMediaPlayer.reset();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Arcvideo Player/3.5");
        headers.put("Referer", "Arcvideo Sample Player");

        try {
            mMediaPlayer.setDataSource(mUrlRTSP, headers);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("VRDemo", "setOnCompletionListener");

        Log.e("VRDemo", "setOnPreparedListener");
        Log.e("VRDemo", "setOnVideoSizeChangedListener");

        //mMediaPlayer.setLooping(true);

        Log.e("VRDemo", "setDisplay");

        id_vr_videoview.setMediaPlayer(mMediaPlayer);

        int mReconnectCount = 2;
        mMediaPlayer.setConfig(ArcMediaPlayer.CONFIG_NETWORK_RECONNECT_COUNT, mReconnectCount);

        mMediaPlayer.prepareAsync();

    }


    // open url dialog
    public static boolean isInputTextValid(String strText) {
        if (null == strText || 0 == strText.length())
            return false;

        String strForbidden = "\\*<>|\n";
        int nStrForbidLen = 0;
        boolean bValid = true;

        nStrForbidLen = strForbidden.length();
        for (int i = 0; i < strText.length(); i++) {
            for (int j = 0; j < nStrForbidLen; j++) {
                if (strText.charAt(i) == strForbidden.charAt(j)) {
                    bValid = false;
                    break;
                }
            }
        }

        return bValid;
    }
}
