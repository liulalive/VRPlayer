/*----------------------------------------------------------------------------------------------
 *
 * This file is Arcvideo's property. It contains Arcvideo's trade secret, proprietary and
 * confidential information.
 *
 * The information and code contained in this file is only for authorized Arcvideo employees
 * to design, create, modify, or review.
 *
 * DO NOT DISTRIBUTE, DO NOT DUPLICATE OR TRANSMIT IN ANY FORM WITHOUT PROPER AUTHORIZATION.
 *
 * If you are not an intended recipient of this file, you must not copy, distribute, modify,
 * or take any action in reliance on it.
 *
 * If you have received this file in error, please immediately notify Arcvideo and
 * permanently delete the original and any copy of any file and any printout thereof.
 *
 *-------------------------------------------------------------------------------------------------*/
package com.arcvideo.vrdemo;

///////////////////

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.arcvideo.MediaPlayer.ArcMediaPlayer;
import com.arcvideo.MediaPlayer.ArcMediaPlayer.*;
import com.arcvideo.MediaPlayer.MV2Config.VALIDATE;
import com.arcvideo.MediaPlayer.MV2Config;
import com.arcvideo.vrkit.VRConst.*;
import com.arcvideo.vrkit.VRVideoView;

@SuppressLint("NewApi")
public class Player1Activity extends Activity implements
        OnCompletionListener, OnPreparedListener,
        OnVideoSizeChangedListener, OnErrorListener{
    private float squareCoords[] = {
            -0.52f, 0.29f, -0.85f,
            -0.52f, -0.29f, -0.85f,
            0.52f, -0.29f, -0.85f,
            0.52f, 0.29f, -0.85f
    };

    enum AMMF_STATE {
        IDLE, INITIALIZED, PREPARING, PREPARED, STARTED, PAUSED, STOPPED, PLAYCOMPLETED
    }

    ;

    private static final String TAG = "ArcVRDemo";
    AMMF_STATE m_PlayerState = AMMF_STATE.IDLE;

    public String m_strURL = "rtsp://admin:bsoft123@10.0.4.74/h264/ch1/main/av_stream";

    protected ArcMediaPlayer mMediaPlayer;
    private int m_iCurOrientation = 0;
    VRVideoView mVideoView = null;

    int m_frameWidth = 0;
    int m_frameHeight = 0;
    private int m_lLoadType = 0;


    //鉴权参数:需要app去获取和提供
    private String accessKey = "6c621b52-f1a";
    private String secretKey = "y0LF9gjTQ4atdmGGcsDk";
    private String appKey = "f358d95832534f7e84f4fae7c75bb62d";

    /**
     * Called with the activity is first created.
     */
    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.streamingplayer1);
        m_iCurOrientation = this.getResources().getConfiguration().orientation;
        mVideoView = (VRVideoView) findViewById(R.id.textureView);

        try {
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open("360.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                Log.d(TAG, "bitmap:width=" + bitmap.getWidth() + " ,height=" + bitmap.getHeight());
            } else {
                Log.d(TAG, "bitmap == null");
            }
            mVideoView.setBackgroundImage(bitmap);
            mVideoView.setVideoCoords(squareCoords);

            DisplayMetrics dm = getResources().getDisplayMetrics();
            Point point = new Point();
            getWindowManager().getDefaultDisplay().getRealSize(point);
            mVideoView.setScreenSize(point.x / dm.xdpi * 0.0254f, point.y / dm.ydpi * 0.0254f);

        } catch (Exception e) {
            Log.d(TAG, "exception:" + e.toString());
        }
//        mVideoView.setRenderMode(mRenderMode);

        Log.e(TAG, "new ArcMediaPlayer " + getFilesDir().getAbsolutePath() + "/MV3Plugin.ini");
        mMediaPlayer = new ArcMediaPlayer();
        mMediaPlayer.setConfigFile(this, getFilesDir().getAbsolutePath()
                + "/MV3Plugin.ini");
        mMediaPlayer.validate(this, accessKey, secretKey, appKey);
        mMediaPlayer.reset();
        openFileStr();
    }


    protected void onDestroy() {
        Log.d(TAG, "Destory");

        if (mVideoView != null)
            mVideoView.setMediaPlayer(null);

        if (mMediaPlayer != null) {
            if (mVideoView != null)
                mVideoView.setMediaPlayer(null);
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        super.onDestroy();
    }


    @SuppressWarnings("deprecation")
    public void SetSurfaceRect(int x, int y, int w, int h) {
        RelativeLayout.LayoutParams lp;
        {
            if (w <= 1 && h <= 1) {
                lp = (RelativeLayout.LayoutParams) (mVideoView
                        .getLayoutParams());
                lp.leftMargin = getWindow().getWindowManager().getDefaultDisplay()
                        .getWidth();
                lp.topMargin = getWindow().getWindowManager().getDefaultDisplay()
                        .getHeight();
                lp.width = 0;
                lp.height = 0;
                mVideoView.setLayoutParams(lp);
                Log.e("VRDemo", "[0]x=" + lp.leftMargin + "y=" + lp.topMargin + "w=" + lp.width + "h="
                        + lp.height);
            } else {
                lp = (RelativeLayout.LayoutParams) (mVideoView
                        .getLayoutParams());
                lp.leftMargin = x;
                lp.topMargin = y;
                lp.width = w;
                lp.height = h;
                mVideoView.setLayoutParams(lp);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation != m_iCurOrientation) {
            m_iCurOrientation = newConfig.orientation;
            onVideoSizeChanged(mMediaPlayer, m_frameWidth, m_frameHeight);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            Point point = new Point();
            getWindowManager().getDefaultDisplay().getRealSize(point);
            mVideoView.setScreenSize(point.x / dm.xdpi * 0.0254f, point.y / dm.ydpi * 0.0254f);
        }
        super.onConfigurationChanged(newConfig);
    }

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

    public void showToast(String str, boolean bShowLong) {
        Toast toast = null;
        int nDuration = bShowLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        toast = Toast.makeText(this, str, nDuration);
        toast.show();
    }


    @SuppressLint("DefaultLocale")
    protected void stopPlayback() {
        if (m_PlayerState != AMMF_STATE.IDLE) {
            m_PlayerState = AMMF_STATE.STOPPED;
            if (null != mMediaPlayer) {
                mMediaPlayer.stop();
            }

            if (mVideoView != null)
                mVideoView.setMediaPlayer(null);

            Log.e("VRDemo", "stopPlayback");
            controlBackLight(this, false);
        }

        showSystemNavigationBar();
    }


    public void onCompletion(ArcMediaPlayer arg0) {
        Log.d(TAG, "onCompletion called");
        mMediaPlayer.stop();
        m_PlayerState = AMMF_STATE.STOPPED;
        m_bVideoSizeChanged = false;
        if (m_lLoadType == 1) {
            Log.d("VRDemo", "Intent Click stop out");
            finish();
        }

        controlBackLight(this, false);
    }

    @SuppressWarnings("deprecation")
    public void onVideoSizeChanged(ArcMediaPlayer mp, int width, int height) {
        Log.v(TAG, "onVideoSizeChanged called: " + width + "x" + height);
/*

        boolean needFit = (mRenderMode == RenderMode.NORMALVIEW || mRenderMode == RenderMode.NORMALVIEW3DHALF);

        m_frameWidth = width;
        m_frameHeight = height;

        needFit = needFit && m_frameWidth != 0 && m_frameHeight != 0;
*/

        if (width != 0 && height != 0 && m_PlayerState == AMMF_STATE.STARTED)
            m_bVideoSizeChanged = true;
        Log.v(TAG, "onVideoSizeChanged m_bVideoSizeChanged: "
                + m_bVideoSizeChanged);
        //int nScreenWidth = getWindow().getWindowManager().getDefaultDisplay().getWidth();
        //int nScreenHeight = getWindow().getWindowManager().getDefaultDisplay().getHeight();
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(outMetrics);
        int nScreenWidth = outMetrics.widthPixels;
        int nScreenHeight = outMetrics.heightPixels;

        float aspect_ratio = mMediaPlayer.getAspectRatio();
        Log.v(TAG, "aspect_ratio=" + aspect_ratio);
        Log.v(TAG, "before adjuct aspect, w=" + m_frameWidth + ",h="
                + m_frameHeight);

        if (aspect_ratio != 0.0) {
            m_frameWidth = Float.floatToIntBits((Float.intBitsToFloat(m_frameHeight) * aspect_ratio));

            Log.v(TAG, "after adjuct aspect, w=" + m_frameWidth + ",h="
                    + m_frameHeight);
        }
        /* start */
      /*  if (needFit) {
            int estimateW, estimateH;
            if (nScreenWidth * m_frameHeight > nScreenHeight * m_frameWidth) {
                estimateW = nScreenHeight * m_frameWidth / m_frameHeight;
                estimateH = nScreenHeight;
                if (estimateW % 4 != 0)
                    estimateW -= estimateW % 4;
            } else {
                estimateW = nScreenWidth;
                estimateH = nScreenWidth * m_frameHeight / m_frameWidth;
                if (estimateH % 4 != 0)
                    estimateH -= estimateH % 4;
            }
            int xOffset = (nScreenWidth - estimateW) / 2;
            int yOffset = (nScreenHeight - estimateH) / 2;
            if (xOffset % 4 != 0)
                xOffset -= xOffset % 4;
            if (yOffset % 4 != 0)
                yOffset -= yOffset % 4;
            Log.d(TAG, xOffset + ", " + yOffset + ", " + estimateW + "x"
                    + estimateH);
            SetSurfaceRect(xOffset, yOffset, estimateW, estimateH);
        } else {
            SetSurfaceRect(0, 0, nScreenWidth, nScreenHeight);
        }*/

    }

    boolean m_bVideoSizeChanged = false;

    public void onPrepared(ArcMediaPlayer mediaplayer) {
        Log.d(TAG, "onPrepared called");
        m_PlayerState = AMMF_STATE.PREPARED;
        if (!m_bVideoSizeChanged)
            onVideoSizeChanged(mMediaPlayer, mMediaPlayer.getVideoWidth(),
                    mMediaPlayer.getVideoHeight());

        m_PlayerState = AMMF_STATE.STARTED;
        mMediaPlayer.start();
    }

    private int mRetryCount = 0;
    private int mRetryLimit = 5;

    public boolean onError(ArcMediaPlayer mp, int what, int extra) {
        boolean bUpdate = false;
        boolean bNeedRetry = false;
        int lastPlayedPosition = 0;
        String codecTypeString = "";
        switch (what) {
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_UNSUPPORTED_SCHEME:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_SOURCE_UNSUPPORTED_SCHEME, value = " + what);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_STREAM_SEEK:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_SOURCE_STREAM_SEEK, value = " + what);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_DNS_RESOLVE:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_SOURCE_DNS_RESOLVE, value = " + what);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_PLAYER_DISPLAY_INIT_FAILED:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_PLAYER_DISPLAY_INIT_FAILED, value = " + what);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_PLAYER_NOAUDIO_VIDEOUNSUPPORT:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_PLAYER_NOAUDIO_VIDEOUNSUPPORT, value = " + what);
                codecTypeString = mMediaPlayer.getMediaMetadata(MV2Config.METADATA.METADATA_KEY_VIDEO_TYPE, bUpdate);
                Log.e(TAG, "onError: unsupproted video codec is " + codecTypeString);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_PLAYER_NOVIDEO_AUDIOUNSUPPORT:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_PLAYER_NOVIDEO_AUDIOUNSUPPORT, value = " + what);
                codecTypeString = mMediaPlayer.getMediaMetadata(MV2Config.METADATA.METADATA_KEY_AUDIO_TYPE, bUpdate);
                Log.e(TAG, "onError: unsupproted audio codec is " + codecTypeString);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_PLAYER_AVCODEC_UNSUPPORT:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_PLAYER_AVCODEC_UNSUPPORT, value = " + what);
                codecTypeString = mMediaPlayer.getMediaMetadata(MV2Config.METADATA.METADATA_KEY_VIDEO_TYPE, bUpdate);
                Log.e(TAG, "onError: unsupproted video codec is " + codecTypeString);
                codecTypeString = mMediaPlayer.getMediaMetadata(MV2Config.METADATA.METADATA_KEY_AUDIO_TYPE, bUpdate);
                Log.e(TAG, "onError: unsupproted audio codec is " + codecTypeString);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_PLAYER_AVCODEC_AUDIOUNSUPPORT:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_PLAYER_AVCODEC_AUDIOUNSUPPORT, value = " + what);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_PLAYER_AVCODEC_VIDEOUNSUPPORT:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_PLAYER_AVCODEC_VIDEOUNSUPPORT, value = " + what);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_PLAYER_OPERATION_CANNOTEXECUTE:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_PLAYER_OPERATION_CANNOTEXECUTE, value = " + what);
                break;
            case android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_UNKNOWN, value = " + what);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_SEEK_BEYONDFILESIZE:
                Log.e(TAG, "onError: error type is MEDIA_ERROR_SOURCE_SEEK_BEYONDFILESIZE, value = " + what);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_NETWORK_CONNECTFAIL:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_STREAM_OPEN:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATARECEIVE_TIMEOUT:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_FORMAT_UNSUPPORTED:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_FORMAT_MALFORMED:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_NETWORK_CONNECTIMEOUT:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATARECEIVE_FAIL:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATASEND_TIMEOUT:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATAERROR_HTML:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATASEND_FAIL:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_DATARECEIVE_NOBODY:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_DNS_RESOLVE_TIMEOUT:
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_ERROR_SOURCE_BUFFER_TIMEOUT:
                lastPlayedPosition = mp.getCurrentPosition();
                bNeedRetry = true;
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.LICENSE_ERR:
                Log.e(TAG, "===license onError");
                showLicenseErr(extra);
                break;
            default:
                if (what >= 100400 && what <= 100599) {
                    Log.e(TAG, "onError: error type is one of the http critical status code, value = " + what);
                } else {
                    Log.e(TAG, "onError: what the Fxxxx!! error is " + what + "? code is " + extra);
                }
                break;
        }
        stopPlayback();
        showToast("Error code = " + what + ", extra = " + extra, false);

        if (bNeedRetry)
            retryPlayback(lastPlayedPosition);

        return true;
    }

    private void retryPlayback(int lastPos) {
        if (mRetryCount++ < mRetryLimit) {
            if (mMediaPlayer != null) {
                if (mVideoView != null)
                    mVideoView.setMediaPlayer(null);
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            mMediaPlayer = new ArcMediaPlayer();
            mMediaPlayer.setConfigFile(this, getFilesDir().getAbsolutePath()
                    + "/MV3Plugin.ini");
            mMediaPlayer.validate(this, accessKey, secretKey, appKey);
            try {
                openFileStr();
            } catch (Exception e) {
                e.printStackTrace();
                showToast("重试出错", false);
            }
        } else {
            showToast("达到重试次数上限", false);
        }
    }

    @SuppressLint("NewApi")
    private void openFileStr() {
        Log.d(TAG, "AA=====openFileStr: m_strURL=" + m_strURL);
        boolean bValid = isInputTextValid(m_strURL);
        if (!bValid) {
            showToast(getString(R.string.str_invalid_url), false);
        } else {
            mMediaPlayer.reset();
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", "Arcvideo Player/3.5");
            headers.put("Referer", "Arcvideo Sample Player");

            try {
                mMediaPlayer.setDataSource(m_strURL, headers);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            m_bVideoSizeChanged = false;
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mVideoView.setMediaPlayer(mMediaPlayer);
            int mReconnectCount = 2;
            mMediaPlayer.setConfig(ArcMediaPlayer.CONFIG_NETWORK_RECONNECT_COUNT, mReconnectCount);
            mMediaPlayer.prepareAsync();
            controlBackLight(this, true);
            m_PlayerState = AMMF_STATE.PREPARING;
            hideSystemNavigationBar();
        }
    }

    public void controlBackLight(Activity activity, boolean flag) {
        if (null == activity)
            return;

        Window win = activity.getWindow();
        if (flag) {
            mVideoView.setKeepScreenOn(true);
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            mVideoView.setKeepScreenOn(false);
            win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        Log.e("VRDemo", "onPause");
        super.onPause();
        if (mMediaPlayer != null) {
            mVideoView.setMediaPlayer(null);
        }
    }

    @Override
    protected void onResume() {
        Log.e("VRDemo", "onResume");
        super.onResume();

        if (mMediaPlayer != null/* && m_PlayerState == AMMF_STATE.STOPPED*/) {
            mVideoView.setMediaPlayer(mMediaPlayer);
            //mMediaPlayer.start();
            if (mMediaPlayer.isPlaying()) {
                hideSystemNavigationBar();
            }
        }
    }

    class ModeCombination {
        RenderMode renderMode;
        ControlMode ctrlMode;
        int txtLabel;

        public ModeCombination(RenderMode m, ControlMode c, int l) {
            renderMode = m;
            ctrlMode = c;
            txtLabel = l;
        }
    }

  /*  ModeCombination mModes[] = {
            new ModeCombination(RenderMode.FULLVIEW, ControlMode.TOUCH, R.string.mode_touch_vr),
            new ModeCombination(RenderMode.FULLVIEW3D, ControlMode.GYROSCOPE, R.string.mode_gyro_3d),
            new ModeCombination(RenderMode.VR3DVIEW, ControlMode.GYROSCOPE, R.string.mode_gyro_3dvr),
            new ModeCombination(RenderMode.VR3DVIEW180, ControlMode.TOUCH, R.string.mode_touch_3dvr2),
            new ModeCombination(RenderMode.NORMALVIEW, ControlMode.TOUCH, R.string.mode_normal),
            new ModeCombination(RenderMode.CINEMAVIEW, ControlMode.TOUCH, R.string.mode_cinema),
            new ModeCombination(RenderMode.CINEMAVIEW3D, ControlMode.GYROSCOPE, R.string.mode_cinema_3d),
            new ModeCombination(RenderMode.CINEMAVIEW3DREAL, ControlMode.GYROSCOPE, R.string.mode_cinema_3dreal),
            new ModeCombination(RenderMode.FISHEYEVIEW, ControlMode.TOUCH, R.string.mode_fisheye)
    };
    int miCurrentMode = 0;
    RenderMode mRenderMode = mModes[miCurrentMode].renderMode;
*/

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mVideoView.onTouchEvent(event);
        return super.onTouchEvent(event);
    }



    private void showLicenseErr(int res) {
        String errorInfo = "";
        switch (res) {
            case VALIDATE.LICENSE_ERR_DISABLE_APP_NAME:
                errorInfo = "appname不一致，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_AUTHENTICATE_FAIL:
                errorInfo = "验证不通过，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_INVALID_PARAM:
                errorInfo = "账号无设置，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_MEM_NOT_ENOUGH:
                errorInfo = "内存不足，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_PARAMETER_DISACCORD:
                errorInfo = "多SDK账号信息不一致，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_DIRECTORY_ERR:
                errorInfo = "目录错误，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_SDK_NO_EXITS:
                errorInfo = "sdk信息不存在，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_SDK_PLANTFORM_NO_SUPPORT:
                errorInfo = "平台不支持，禁止";
                break;

            case VALIDATE.LICENSE_ERR_DISABLE_NO_UPDATE:
                errorInfo = "无更新，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_NETWORK:
                errorInfo = "网络错误，禁止";
                break;
            case VALIDATE.LICENSE_ERR_DISABLE_DATA_FORMAT:
                errorInfo = "license格式错误，禁止";
                break;
            default:
                errorInfo = "未知错误!";
                break;
        }

        if (0 != res) {
            Toast.makeText(this, errorInfo, Toast.LENGTH_SHORT).show();
        }

    }


    private void hideSystemNavigationBar() {
        if (android.os.Build.VERSION.SDK_INT > 11 && android.os.Build.VERSION.SDK_INT < 19) {
            View view = this.getWindow().getDecorView();
            view.setSystemUiVisibility(View.GONE);
        } else if (android.os.Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void showSystemNavigationBar() {
        if (android.os.Build.VERSION.SDK_INT > 11 && android.os.Build.VERSION.SDK_INT < 19) {
            View view = this.getWindow().getDecorView();
            view.setSystemUiVisibility(View.VISIBLE);
        } else if (android.os.Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

}
