package com.arcvideo.vrdemo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.arcvideo.MediaPlayer.ModuleManager.E_ARCH_TYPE;
import com.arcvideo.MediaPlayer.ModuleManager;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

public class MyApplication extends Application {

	private static String TAG = "ArcPlayerSampleApplication";

	private static String _strFileDir = null;

	private static String _strLibsDir = null;

	private static boolean _bOuputLog = true;

	private static String _strArm64LibsDir = null;

	private static boolean _isArm64 = false;

	private static void outputLog(String strTAG, String strInfo) {
		if (_bOuputLog)
			Log.i(strTAG, strInfo);
	}

	private String getCurAppDir() {
		ApplicationInfo applicationInfo = getApplicationInfo();
		outputLog(TAG, "cur data dir:" + this.getApplicationInfo().dataDir);
		outputLog(TAG, "cur file dir:"
				+ getBaseContext().getFilesDir().getAbsolutePath());

		_strLibsDir = applicationInfo.dataDir;
		_strFileDir = getBaseContext().getFilesDir().getAbsolutePath();

		_strArm64LibsDir = applicationInfo.nativeLibraryDir;
		_isArm64 = _strArm64LibsDir.endsWith("arm64");
		_strArm64LibsDir += "/";

		if (!_strLibsDir.endsWith("/")) {
			_strLibsDir = _strLibsDir + "/";
		}
		_strLibsDir = _strLibsDir + "lib/";
		outputLog(TAG, "cur libs dir:" + _strLibsDir);

		if (!_strFileDir.endsWith("/"))
			_strFileDir = _strFileDir + "/";

		if (!_strFileDir.endsWith("/"))
			_strFileDir = _strFileDir + "/";

		outputLog(TAG, "cur file dir:" + _strFileDir);
		return this.getApplicationInfo().dataDir;
	}

	private void copyPlayerIni() {
		getCurAppDir();

		ArrayList<Integer> codecList = new ArrayList<Integer>();
		codecList.add(ModuleManager.CODEC_SUBTYPE_H264);
		codecList.add(ModuleManager.CODEC_SUBTYPE_H265);
		codecList.add(ModuleManager.CODEC_SUBTYPE_MP3);
		codecList.add(ModuleManager.CODEC_SUBTYPE_AAC);

        ArrayList<Integer> parserList = new ArrayList<Integer>();
		parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_MP4);
		parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_FLV);
		parserList.add(ModuleManager.FILE_PARSER_SUBTYPE_TS);

		ModuleManager mgr = new ModuleManager(null, codecList, parserList);
		ArrayList<String> modList = mgr.QueryRequiredModules();
		outputLog(TAG, "module list(" + modList.size() + ": " + modList);
		
		File dirFile = new File(_strFileDir);
		if (!dirFile.exists()) {
			if (!dirFile.mkdirs()) {
				return;
			}
		}

		if(_isArm64)
		{
			mgr.GenerateConfigFile(_strArm64LibsDir, _strFileDir+"MV3Plugin.ini");
		}
		else
		{
			mgr.GenerateConfigFile(_strLibsDir, _strFileDir+"MV3Plugin.ini");
		}
	}

	private static void LoadLibrarayArm64() {
		try {
			outputLog(TAG, "LoadLibraray : load libmv3_platform.so");

			System.loadLibrary("mv3_platform");
		} catch (java.lang.UnsatisfiedLinkError ex) {
			Log.d(TAG, "load Arm64 libmv3_platform.so failed," + ex.getMessage());
		}

		try {
			outputLog(TAG, "LoadLibraray : load libmv3_common.so");
			//System.load(_strLibsDir + "libmv3_common.so");
			System.loadLibrary("mv3_common");
		} catch (java.lang.UnsatisfiedLinkError ex) {
			Log.d(TAG, "load libmv3_common.so failed," + ex.getMessage());
		}

		try {
			outputLog(TAG, "LoadLibraray : load libmv3_mpplat.so");
			//System.load(_strLibsDir + "libmv3_mpplat.so");
			System.loadLibrary("mv3_mpplat");
		} catch (java.lang.UnsatisfiedLinkError ex) {
			Log.d(TAG, "load libmv3_mpplat.so failed," + ex.getMessage());
		}

		try {
			outputLog(TAG, "LoadLibraray : load libmv3_playerbase.so");
			//System.load(_strLibsDir + "libmv3_playerbase.so");
			System.loadLibrary("mv3_playerbase");
		} catch (java.lang.UnsatisfiedLinkError ex) {
			Log.d(TAG, "load libmv3_playerbase.so failed," + ex.getMessage());
		}

		try {
			Log.d(TAG, "LoadLibraray : load libmv3_jni.so");
			//System.load(_strLibsDir + "libmv3_jni.so");
			System.loadLibrary("mv3_jni");
		} catch (java.lang.UnsatisfiedLinkError ex) {
			Log.d(TAG, "load libmv3_jni.so failed, " + ex.getMessage());
		}
	}

	private static void LoadLibraray() {
		Log.d(TAG, "load library  _strLibsDir = " + _strLibsDir);
		System.load(_strLibsDir + "libmv3_platform.so");
		System.load(_strLibsDir + "libmv3_common.so");
		System.load(_strLibsDir + "libmv3_mpplat.so");
		try {
			System.load(_strLibsDir + "libmv3_playerbase.so");
		} catch (java.lang.UnsatisfiedLinkError ex) {
			Log.d(TAG, "load libmv3_playerbase.so failed");
		}

		int apiVersion = android.os.Build.VERSION.SDK_INT;
		if (apiVersion >= 15) {
			try {
				System.load(_strLibsDir + "libmv3_jni_4.0.so");
			} catch (java.lang.UnsatisfiedLinkError ex) {
				Log.d(TAG, "load libmv3_jni_4.0.so failed");
			}
		}else {
			try {
				System.load(_strLibsDir + "libmv3_jni.so");
			} catch (java.lang.UnsatisfiedLinkError ex) {
				Log.d(TAG, "load libmv3_jni.so failed");
			}
		}
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		outputLog(TAG, "ArcPlayerApplication");
		copyPlayerIni();
		if(_isArm64)
		{
			LoadLibrarayArm64();
		}
		else
		{
			LoadLibraray();
		}
		super.onCreate();
	}

}
