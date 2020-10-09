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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.arcvideo.MediaPlayer.ArcMediaPlayer;
import com.arcvideo.MediaPlayer.ArcMediaPlayer.*;
import com.arcvideo.MediaPlayer.MV2Config.VALIDATE;
import com.arcvideo.MediaPlayer.MV2Config;
import com.arcvideo.vrkit.VRConst.*;
import com.arcvideo.vrkit.VRVideoView;

@SuppressLint("NewApi")
public class Player1Activity extends Activity implements
        View.OnClickListener,
        OnCompletionListener, OnPreparedListener,
        OnVideoSizeChangedListener, OnInfoListener, OnErrorListener, VRVideoView.OnScaleChangedListener {

    public static final int MSG_GET_POSITION = 1;
    public static final int MSG_GET_BUFFERINGPERCENT = 2;
    public static final int MSG_DELAYED_OPEN_EXT = 3;
    public static final int MSG_HIDE_SCALE_DISPLAY = 10;
    public static final int MSG_HIDE_ROTATION_DISPLAY = 11;

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

    public ListView m_listView = null;
    public String m_strURL;
    long m_lDuration = 0;

    private RelativeLayout m_selLayout;
    protected ArcMediaPlayer mMediaPlayer;
    private int m_iCurOrientation = 0;
    private ImageButton m_imgBtnPlayPause = null;
    private ImageButton m_imgBtnAdd = null;
    VRVideoView mVideoView = null;


    int m_frameWidth = 0;
    int m_frameHeight = 0;
    int mLastSelected = 0;
    private int m_lLoadType = 0;


    //鉴权参数:需要app去获取和提供
    private String accessKey = "6c621b52-f1a";
    private String secretKey = "y0LF9gjTQ4atdmGGcsDk";
    private String appKey = "f358d95832534f7e84f4fae7c75bb62d";

    @SuppressLint("HandlerLeak")
    protected Handler mRefreshHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DELAYED_OPEN_EXT:
                    try {
                        openFileStr();
                    } catch (IllegalArgumentException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IllegalStateException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    break;
                case MSG_GET_POSITION: {
                    mRefreshHandler.removeMessages(MSG_GET_POSITION);

                    int nCurPos = 0;
                    if (null != mMediaPlayer/* && mMediaPlayer.isPlaying()*/) {
                        try {
                            nCurPos = mMediaPlayer.getCurrentPosition();
                        } catch (IllegalStateException e) {
                            // stop getting position
                            break;
                        }
                        mRefreshHandler.sendEmptyMessageDelayed(MSG_GET_POSITION,
                                500);
                    }
                }
                break;
                case MSG_GET_BUFFERINGPERCENT: {
                    mRefreshHandler.removeMessages(MSG_GET_BUFFERINGPERCENT);

                    int nCurPercent = 0;
                    if (null != mMediaPlayer) {
                        try {
                            nCurPercent = mMediaPlayer.getCurrentBufferingPercent();
                        } catch (IllegalStateException e) {
                            // stop getting position
                            break;
                        }
//						m_tvStatus.setText("Buf:" + nCurPercent);
                        mRefreshHandler.sendEmptyMessageDelayed(
                                MSG_GET_BUFFERINGPERCENT, 300);
                    }
                }
                break;

            }
        }
    };

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
        m_imgBtnPlayPause = (ImageButton) findViewById(R.id.but_play_pausel);
        m_imgBtnPlayPause.setOnClickListener(this);

        m_imgBtnAdd = (ImageButton) findViewById(R.id.btn_add);
        m_imgBtnAdd.setOnClickListener(this);
        mVideoView = (VRVideoView) findViewById(R.id.textureView);
        mVideoView.setOnScaleChangedListener(this);
        mVideoView.setVideo3DMode(/*Video3DMode.TopBottom:*/Video3DMode.LeftRight);

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
        mVideoView.setRenderMode(mRenderMode);

        m_selLayout = (RelativeLayout) findViewById(R.id.SelectLayout);

        m_listView = (ListView) findViewById(R.id.listitem);

        getUrlList();
        m_listView.setAdapter(new SimpleAdapter(this, m_aryUrl, R.layout.footer_selectview, new String[]{"url"}, new int[]{R.id.url}));
        m_listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            View lastView = null;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // TODO Auto-generated method stub
                Log.e("VRDemo", "selected " + position + "," + id);
                if (lastView != null)
                    lastView.setBackgroundColor(Color.WHITE);
                view.setBackgroundColor(Color.DKGRAY);
                lastView = view;
                mLastSelected = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
                Log.e("VRDemo", "nothing selected");
            }

        });
        m_listView.requestFocus();
        m_listView.setSelection(mLastSelected);
        m_listView.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub
                int newPos = 0;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        newPos = m_listView.getSelectedItemPosition() - 1;
                        Log.d(TAG, "newPos: " + newPos);
                        if (newPos < 0) {
                            newPos += m_listView.getCount();
                        }
                        if (event.getAction() == KeyEvent.ACTION_UP)
                            m_listView.setSelection(newPos);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        newPos = m_listView.getSelectedItemPosition() + 1;
                        Log.d(TAG, "newPos: " + newPos);
                        if (newPos >= m_listView.getCount()) {
                            newPos -= m_listView.getCount();
                        }
                        if (event.getAction() == KeyEvent.ACTION_UP)
                            m_listView.setSelection(newPos);
                        return true;
                }
                return false;
            }
        });
        m_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView tv = (TextView) view.findViewById(R.id.url);
                m_strURL = tv.getText().toString().trim();
                Log.d(TAG, "onItemClick" + position);
                try {
//					m_mainLayout.setVisibility(View.VISIBLE);
                    m_listView.setVisibility(View.INVISIBLE);
                    m_selLayout.setVisibility(View.GONE);
                    setVideoVisible(true);
                    openFileStr();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        m_listView.setVisibility(View.INVISIBLE);
        m_selLayout.setVisibility(View.GONE);
        setVideoVisible(true);

        Log.e(TAG, "new ArcMediaPlayer " + getFilesDir().getAbsolutePath() + "/MV3Plugin.ini");
        mMediaPlayer = new ArcMediaPlayer();
        mMediaPlayer.setConfigFile(this, getFilesDir().getAbsolutePath()
                + "/MV3Plugin.ini");
        mMediaPlayer.validate(this, accessKey, secretKey, appKey);
        mMediaPlayer.reset();

        Intent intent = getIntent();
        if (intent != null) {
            m_strURL = uriToFilePath(this, (Uri) intent.getData());
            Log.d(TAG, "URL: " + m_strURL);
            if (m_strURL != null) {
                // Loaded from intent, delayed open
                mRefreshHandler.removeMessages(MSG_DELAYED_OPEN_EXT);
                mRefreshHandler.sendEmptyMessageDelayed(MSG_DELAYED_OPEN_EXT,
                        200);

                m_lLoadType = 1;
            }
        }

    }

    private static String uriToFilePath(Context activity, Uri uri) {
        if (null == uri || null == activity) {
            return null;
        }
        Log.d(TAG, "uri=" + uri.toString());
        String tempString = null;
        Cursor cur = null;
        tempString = uri.toString();
        if (tempString.startsWith("http://") || tempString.startsWith("rtmp://")
                || tempString.startsWith("rtsp://")) {
            tempString = tempString.replaceAll("%20", " ");
        } else if (uri.toString().startsWith("file://")) {
            tempString = tempString.replaceAll("%20", " ");
            tempString = tempString.replaceAll("%25", "%");
        } else {
            ContentResolver cr = activity.getContentResolver();

            cur = cr.query(uri, null, null, null, null);
            if (null != cur) {
                if (cur.moveToFirst()) {
                    int idIndex = cur
                            .getColumnIndex(MediaStore.Audio.Media.DATA);
                    tempString = cur.getString(idIndex);
                }
            }
        }

        if (cur != null) {
            cur.close();
        }

        return tempString;
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
                Log.e("VRDemo", "[1]x=" + x + "y=" + y + "w=" + w + "h=" + h);
                lp = (RelativeLayout.LayoutParams) (mVideoView
                        .getLayoutParams());
                Log.e("VRDemo", "[2]x=" + lp.leftMargin + "y=" + lp.topMargin + "w=" + lp.width + "h="
                        + lp.height);
                lp.leftMargin = x;
                lp.topMargin = y;
                lp.width = w;
                lp.height = h;
                mVideoView.setLayoutParams(lp);
                Log.e("VRDemo", "[3]x=" + lp.leftMargin + "y=" + lp.topMargin + "w=" + lp.width + "h="
                        + lp.height);

            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e("VRDemo", "onConfigurationChanged, " + newConfig.orientation + ","
                + newConfig.keyboardHidden + ","
                + newConfig.screenWidthDp + "x" + newConfig.screenHeightDp);

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

    private AlertDialog m_currDlg = null;
    private Button m_btnConfirm;
    private Button m_btnCancel;

    private ArrayList<Map<String, Object>> m_aryUrl = new ArrayList<Map<String, Object>>();
    private File m_fileUrl = new File("/data/local/tmp/url.txt");
    private final String SPLIT_CHAR = "\r\n";

    private void getUrlList() {
        Log.e("getUrlList", "getUrlList 0 ,m_fileUrl:" + m_fileUrl);
        try {
            Log.e("getUrlList", "getUrlList 1 ");
            if (null != m_fileUrl) {
                Log.e("getUrlList", "getUrlList 2 ");
                if (!m_fileUrl.exists() || !m_fileUrl.canRead()) {
                    m_fileUrl = new File(Environment.getExternalStorageDirectory()
                            .getPath() + "/url.txt");
                }
                if (m_fileUrl.exists() && m_fileUrl.canRead()) {
                    Log.e("getUrlList", "getUrlList 3 ");
                    FileInputStream is = new FileInputStream(m_fileUrl);
                    Log.e("getUrlList", "getUrlList 4, is:" + is);
                    int iSize = is.available();
                    StringBuffer sb = new StringBuffer();
                    if (0 != iSize) {
                        byte[] buffer = new byte[iSize];
                        Log.e("getUrlList", "getUrlList 5, buffer:" + iSize
                                + "  " + buffer);
                        Log.e("getUrlList", "getUrlList 6, sb:" + sb);
                        while (is.read(buffer) != -1) {
                            Log.e("getUrlList", "getUrlList 7:" + iSize + "  "
                                    + buffer);
                            sb.append(new String(buffer));
                        }
                        Log.e("getUrlList", "getUrlList 8");
                        System.out.println(sb.toString());
                    }
                    is.close();
                    is = null;

                    String[] ary = sb.toString().split(SPLIT_CHAR);
                    if (ary != null) {
                        for (int i = 0; i < ary.length; i++) {
                            if (ary[i].compareToIgnoreCase("") != 0) {
                                Map<String, Object> item = new HashMap<String, Object>();
                                item.put("url", ary[i]);
                                m_aryUrl.add(item);
                            }
                        }
                    }
                    ary = null;
                    sb = null;
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Log.e("getUrlList", "getUrlList 61");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e("getUrlList", "getUrlList 62");
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void saveUrlList() {
        Log.e("saveUrlList", "saveUrlList 0 ,m_fileUrl:" + m_fileUrl);
        if (null != m_fileUrl) {
            if (m_fileUrl.exists())
                m_fileUrl.delete();

            try {
                m_fileUrl.createNewFile();

                StringBuffer sb = new StringBuffer();
                Log.e("saveUrlList", "saveUrlList 1 ,sb:" + sb);
                for (int i = 0; i < m_aryUrl.size(); i++) {
                    sb.append(m_aryUrl.get(i).get("url"));
                    sb.append(SPLIT_CHAR);
                }

                FileOutputStream os = new FileOutputStream(m_fileUrl);
                Log.e("saveUrlList", "saveUrlList 2 ,os:" + os);
                System.out.println(sb.toString());
                os.write(sb.toString().getBytes());
                os.flush();

                os.close();
                os = null;

                // m_fileUrl = null;
            } catch (IOException e) {
                Log.e("saveUrlList", "saveUrlList 71:");
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void addNewUrl(String strNewUrl) {
        boolean bHaveSame = false;

        for (int i = 0; i < m_aryUrl.size(); i++) {
            if (strNewUrl.compareToIgnoreCase((String) m_aryUrl.get(i).get("url")) == 0) {
                bHaveSame = true;

                break;
            }
        }

        if (!bHaveSame) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("url", strNewUrl);
            m_aryUrl.add(0, item);
        }
    }

    public void showToast(String str, boolean bShowLong) {
        Toast toast = null;
        int nDuration = bShowLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        toast = Toast.makeText(this, str, nDuration);
        toast.show();
    }


    public void showOpenURLDlg(String strTitle, String strContent) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.dialog_edit_text,
                null);

        EditText etCon = (EditText) textEntryView
                .findViewById(R.id.edittext_edit);
        etCon.getEditableText().insert(0, strContent);
        m_btnConfirm = (Button) textEntryView.findViewById(R.id.plsBtnConfirm);
        m_btnCancel = (Button) textEntryView.findViewById(R.id.plsBtnCancel);
        m_btnConfirm.requestFocus();

        m_currDlg = new AlertDialog.Builder(this).create();
        m_currDlg.setView(textEntryView, 0, 0, 0, 0);
        m_currDlg.show();
        //make max width to display more text
        WindowManager.LayoutParams params = m_currDlg.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.FILL_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        m_currDlg.getWindow().setAttributes(params);

        View.OnClickListener clickListener = new View.OnClickListener() {
            public void onClick(View v) {
                long id = v.getId();
                if (id == R.id.plsBtnConfirm) {
                    Log.e("VRDemo", "showOpenURLDlg 5");
                    EditText etPl = (EditText) textEntryView
                            .findViewById(R.id.edittext_edit);
                    Log.e("VRDemo", "showOpenURLDlg 6");
                    m_strURL = etPl.getText().toString().trim();
                    try {
//						m_mainLayout.setVisibility(View.VISIBLE);
                        m_listView.setVisibility(View.INVISIBLE);
                        m_selLayout.setVisibility(View.GONE);
                        setVideoVisible(true);
                        openFileStr();
                    } catch (IllegalArgumentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (id == R.id.plsBtnCancel) {
                    Log.e("VRDemo", "showOpenURLDlg 15");
                    m_currDlg.cancel();
                }

            }
        };

        m_btnConfirm.setOnClickListener(clickListener);
        m_btnCancel.setOnClickListener(clickListener);
    }

    private void setVideoVisible(boolean visible) {
        mVideoView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private void onClickPlayButton() {
        switch (m_PlayerState) {
            case IDLE:
                Log.e("VRDemo", "onClick,but_open:begin");
//				m_mainLayout.setVisibility(View.GONE);
                m_selLayout.setVisibility(View.VISIBLE);
                m_listView.setVisibility(View.VISIBLE);
                m_listView.requestFocus();

                setVideoVisible(false);

                Log.e("VRDemo", "onClick,but_open:End");
                break;
            case STARTED:
                controlBackLight(this, true);
                Log.e("VRDemo", "onClick,but_pause:begin");
//				m_tvStatus.setText("Pause");
                m_imgBtnPlayPause.setBackgroundResource(R.drawable.ic_videoview_play);
                m_PlayerState = AMMF_STATE.PAUSED;
                mMediaPlayer.pause();
                Log.e("VRDemo", "onClick,but_pause:end");

                break;
            case STOPPED:
                controlBackLight(this, false);
                Log.e("VRDemo", "onClick,but_play:begin");
                m_lDuration = 0;

                m_PlayerState = AMMF_STATE.IDLE;
                if (mMediaPlayer != null)
                    mMediaPlayer.reset(); // reset should be idle status,not stop
//				m_tvStatus.setText("Idle");

//				m_mainLayout.setVisibility(View.GONE);
                m_selLayout.setVisibility(View.VISIBLE);
                m_listView.setVisibility(View.VISIBLE);
                m_listView.requestFocus();

                setVideoVisible(false);

                Log.e("VRDemo", "onClick,but_play:end");
                break;
            case PAUSED:
                Log.e("VRDemo", "onClick,but_play:begin");

                mMediaPlayer.start();
                m_PlayerState = AMMF_STATE.STARTED;
                m_imgBtnPlayPause.setBackgroundResource(R.drawable.ic_videoview_pause);
//				m_tvStatus.setText("Playing");
                Log.e("VRDemo", "onClick,but_play:end");

                break;
            default:
                break;
        }

    }

    private void onClickBackButton() {
        if (m_selLayout.getVisibility() == View.VISIBLE) {
//			m_mainLayout.setVisibility(View.VISIBLE);
            m_listView.setVisibility(View.INVISIBLE);
            m_selLayout.setVisibility(View.GONE);
            setVideoVisible(true);
        } else if (mMediaPlayer.isPlaying()) {
            onClickStopButton();
        } else {
//			m_mainLayout.setVisibility(View.GONE);
            m_selLayout.setVisibility(View.VISIBLE);
            m_listView.setVisibility(View.VISIBLE);
            m_listView.requestFocus();

            setVideoVisible(false);
            finish();
        }
    }

    private void onClickStopButton() {
        int res = 0;
        Log.e("VRDemo", "onClick,but_stop:Begin");
        {
            m_imgBtnPlayPause.setBackgroundResource(R.drawable.ic_videoview_play);

        }
        stopPlayback();

        mMediaPlayer.reset();

        controlBackLight(this, false);

        saveUrlList();

        Log.e("VRDemo", "onClick,but_stop:end," + res);
        if (m_lLoadType == 1) {
            Log.d("VRDemo", "Intent Click stop out");
            finish();
        }
    }

    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        switch (arg0.getId()) {
            case R.id.but_play_pausel:
                onClickPlayButton();
                break;

            case R.id.btn_add:
                showOpenURLDlg("", "");
                break;

            default:
                break;
        }
    }

    @SuppressLint("DefaultLocale")
    protected void stopPlayback() {
        if (m_PlayerState != AMMF_STATE.IDLE) {
            m_PlayerState = AMMF_STATE.STOPPED;
            m_imgBtnPlayPause.setBackgroundResource(R.drawable.ic_videoview_play);
            if (null != mMediaPlayer) {
                mMediaPlayer.stop();
            }

            if (mVideoView != null)
                mVideoView.setMediaPlayer(null);

            Log.e("VRDemo", "stopPlayback");
            controlBackLight(this, false);

            mRefreshHandler.removeMessages(MSG_GET_POSITION);
            mRefreshHandler.removeMessages(MSG_GET_BUFFERINGPERCENT);
        }

        showSystemNavigationBar();
    }


    public void onCompletion(ArcMediaPlayer arg0) {
        Log.d(TAG, "onCompletion called");

        mMediaPlayer.stop();
        m_PlayerState = AMMF_STATE.STOPPED;
        m_imgBtnPlayPause.setBackgroundResource(R.drawable.ic_videoview_play);
        mRefreshHandler.removeMessages(MSG_GET_POSITION);
        mRefreshHandler.removeMessages(MSG_GET_BUFFERINGPERCENT);
        m_bVideoSizeChanged = false;

        saveUrlList();
        if (m_lLoadType == 1) {
            Log.d("VRDemo", "Intent Click stop out");
            finish();
        }

        controlBackLight(this, false);
    }

    @SuppressWarnings("deprecation")
    public void onVideoSizeChanged(ArcMediaPlayer mp, int width, int height) {
        Log.v(TAG, "onVideoSizeChanged called: " + width + "x" + height);

        boolean needFit = (mRenderMode == RenderMode.NORMALVIEW || mRenderMode == RenderMode.NORMALVIEW3DHALF);

        m_frameWidth = width;
        m_frameHeight = height;

        needFit = needFit && m_frameWidth != 0 && m_frameHeight != 0;

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
        if (needFit) {
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
        }

    }

    boolean m_bVideoSizeChanged = false;

    public void onPrepared(ArcMediaPlayer mediaplayer) {
        Log.d(TAG, "onPrepared called");
        m_PlayerState = AMMF_STATE.PREPARED;

        int nDuration = mMediaPlayer.getDuration();
        m_lDuration = nDuration;
        mRefreshHandler.sendEmptyMessage(MSG_GET_POSITION); //

        // if player doesn't notify video size message before prepared
        if (!m_bVideoSizeChanged)
            onVideoSizeChanged(mMediaPlayer, mMediaPlayer.getVideoWidth(),
                    mMediaPlayer.getVideoHeight());

        m_PlayerState = AMMF_STATE.STARTED;
        mMediaPlayer.start();
        m_imgBtnPlayPause.setBackgroundResource(R.drawable.ic_videoview_pause);
        addNewUrl(m_strURL);

    }

    public boolean onInfo(ArcMediaPlayer mp, int what, int extra) {
        switch (what) {
            case android.media.MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                Log.e(TAG,
                        "Bad interleaving of media file, audio/video are not well-formed, extra is "
                                + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_BUFFERING_END:
                mRefreshHandler.removeMessages(MSG_GET_BUFFERINGPERCENT);
                Log.w(TAG,
                        "Player is resuming playback after filling buffer, extra is "
                                + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_BUFFERING_START:
                Log.w(TAG,
                        "Player is temporarily pausing playback internally to buffer more data, extra is "
                                + extra);
                mRefreshHandler.sendEmptyMessage(MSG_GET_BUFFERINGPERCENT);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                Log.w(TAG, "A new set of metadata is available, extra is " + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                Log.e(TAG, "The stream cannot be seeked, extra is " + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                Log.w(TAG, "It's too complex for the decoder, extra is " + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_UNKNOWN:
                Log.w(TAG, "Unknown info, extra is " + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_RENDERING_START:
                Log.w(TAG, "video decode succeeded, start rendering");
                break;
            //warnings definition below
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_SPLITTER_NOAUDIO:
                Log.e(TAG, "MEDIA_INFO_SPLITTER_NOAUDIO ,Info type is "
                        + what + ", level is " + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_SPLITTER_NOVIDEO:
                Log.e(TAG, "MEDIA_INFO_SPLITTER_NOVIDEO, Info type is "
                        + what + ", level is " + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_VCODEC_DECODE_ERROR:
                Log.e(TAG, "MEDIA_INFO_VCODEC_DECODE_ERROR, Info type is "
                        + what + ", level is " + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.MEDIA_INFO_ACODEC_DECODE_ERROR:
                Log.e(TAG, "MEDIA_INFO_ACODEC_DECODE_ERROR, Info type is "
                        + what + ", level is " + extra);
                break;
            case com.arcvideo.MediaPlayer.ArcMediaPlayer.LICENSE_INFO:
                showLicenseInfo(extra);
                Log.e(TAG, "===license onInfo");
                break;
            default:
                Log.i(TAG, "Unknown info code: " + what + ", extra is " + extra);
                break;
        }
        return true;
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
    private void openFileStr() throws IllegalArgumentException,
            IllegalStateException, IOException {
        boolean bValid = isInputTextValid(m_strURL);
        if (!bValid) {
            showToast(getString(R.string.str_invalid_url), false);
        } else {
            if (m_lLoadType == 0 && m_currDlg != null)
                m_currDlg.cancel();

            m_lDuration = 0;

            mMediaPlayer.reset();

            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", "Arcvideo Player/3.5");
            headers.put("Referer", "Arcvideo Sample Player");

            mMediaPlayer.setDataSource(m_strURL, headers);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnPreparedListener(this);
            m_bVideoSizeChanged = false;
            mMediaPlayer.setOnVideoSizeChangedListener(this);

            mMediaPlayer.setOnInfoListener(this);

            mMediaPlayer.setOnErrorListener(this);
            mVideoView.setMediaPlayer(mMediaPlayer);
            int mReconnectCount = 2;
            mMediaPlayer.setConfig(ArcMediaPlayer.CONFIG_NETWORK_RECONNECT_COUNT, mReconnectCount);

            mMediaPlayer.prepareAsync();

            controlBackLight(this, true);

            m_PlayerState = AMMF_STATE.PREPARING;
            m_lDuration = 0;
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

    ;
    ModeCombination mModes[] = {
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
    ControlMode mControlMode = mModes[miCurrentMode].ctrlMode;


    @Override
    public void onScaleChanged(float newScale) {
        // TODO Auto-generated method stub
        mRefreshHandler.removeMessages(MSG_HIDE_SCALE_DISPLAY);
        mRefreshHandler.sendEmptyMessageDelayed(MSG_HIDE_SCALE_DISPLAY, 2000);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mVideoView.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    /**
     * 设置control mode, 如果GYROSCOPE不支持则自动转为TOUCH
     *
     * @return 0---------设置成功
     * 1---------模式设置changed
     * -1--------设置失败
     */
    private int setControlMode() {
        int res = 0;
        if (mControlMode == ControlMode.GYROSCOPE
                && !mVideoView.isMotionSensorPresent()) {
            String textString = "该设备不支持陀螺仪，将自动转为触屏模式";
            Log.e(TAG, textString);
            showToast(textString, true);
            mControlMode = ControlMode.TOUCH;
            FindModeIndex();
            res = 1;
        }
        mVideoView.setControlMode(mControlMode);

        if ((mRenderMode == RenderMode.VR3DVIEW || mRenderMode == RenderMode.FULLVIEW3D || mRenderMode == RenderMode.FULLVIEW3DCLONE
                || mRenderMode == RenderMode.VR3DVIEW180 || mRenderMode == RenderMode.CINEMAVIEW3D || mRenderMode == RenderMode.CINEMAVIEW3DCLONE
                || mRenderMode == RenderMode.CINEMAVIEW3DREAL)
                && mControlMode == ControlMode.GYROSCOPE)
            mVideoView.setDistortionCoefficients(0.2f, 0.2f);
        else
            mVideoView.setDistortionCoefficients(0f, 0f);

        return res;
    }

    private int FindModeIndex() {
        int size = mModes.length;
        for (int i = 0; i < size; i++) {
            if (mModes[i].ctrlMode == mControlMode
                    && mModes[i].renderMode == mRenderMode) {
                miCurrentMode = i;
                break;
            }
        }

        return miCurrentMode;
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


    private void showLicenseInfo(int res) {
        String errorInfo = "";
        switch (res) {
            case VALIDATE.LICENSE_INFO_ENABLE_SDK_EXPIREDATE:
                errorInfo = "sdk过期";
                break;
            case VALIDATE.LICENSE_INFO_ENABLE_NO_UPDATE:
                errorInfo = "license没更新";
                break;
            case VALIDATE.LICENSE_INFO_ENABLE_NETWORK:
                errorInfo = "网络错误";
                break;
            case VALIDATE.LICENSE_INFO_ENABLE_DATA_FORMAT:
                errorInfo = "license格式错误";
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
