package com.abstack.plugins;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;


/**
 * Created by lagel on 16/3/21.
 *
 * 这里是一个 cordova 插件，但是和 Activity 的模式类似。
 */
public class Demo extends CordovaPlugin implements TextToSpeech.OnInitListener{

    private static final String IFLYAPK = "http://www.coolapk.com/apk/com.iflytek.tts";
    private static final String IFLYTEK = "com.iflytek.tts";
    private static final String CHECK = "check";
    private static final String SPEAK = "speak";
    private static final String STOP = "stop";

    private static final int REQUEST = 9980;
    private static final int DIALOG = 1000;

    private static final String TAG = "Demo";

    private TextToSpeech tts = null;

    private boolean ready = false;
    private Context context = null;
    private int version = 0;
    private Handler handler;

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        version = Build.VERSION.SDK_INT;
        Log.i(TAG, action);

        context = this.cordova.getActivity().getApplicationContext();
        if (tts == null) {
            if (version > 14) {
                tts = new TextToSpeech(context, this, IFLYTEK);
            } else {
                tts = new TextToSpeech(context, this);
            }
        }
        /*
            因为 AlterDialog 只能在 UI 线程中运行
            但是代码是在 cordova 的线程池调用的
            所以，需要使用 Handler 来进行通信
        */
        handler = new Handler() {
            @Override
            public void dispatchMessage(Message msg) {
                if (msg.what == DIALOG) {
                    dialog();
                }
            }
        };

        // cordova 的线程池
        this.cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                if (CHECK.equals(action)) {
                    check(callbackContext);
                } else if (SPEAK.equals(action)) {
                    try {
                        String msg = args.getString(0);
                        speak(callbackContext, msg);
                    } catch (JSONException e) {
                        callbackContext.error("数据格式错误");
                    }
                } else if (STOP.equals(action)) {
                    stop(callbackContext);
                }
            }
        });

        return true;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true;
            Log.i(TAG, "初始化成功");
        } else {
            show("暂无语音引擎");
        }
    }

    private void show(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    // 跳出安装提示框
    private void dialog() {
        new AlertDialog.Builder(this.cordova.getActivity())
                .setTitle("安装讯飞语音")
                .setMessage("将要跳转到下载地址，是否同意？")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        installApk();
                    }
                })
                .show();
    }

    // 朗读一段文字
    private void speak(CallbackContext callbackContext, String msg) {
        if (!ready && (msg == null || "".equals(msg.trim()))) {
            callbackContext.error("语音信息不能为空!");
            return;
        } else {
            if (version < 14) {
                tts.setEngineByPackageName(IFLYTEK);
            }
            tts.speak(msg, TextToSpeech.QUEUE_ADD, null);
            callbackContext.success();
        }
    }

    // 停止语音文件
    private void stop(CallbackContext callbackContext) {
        if (ready && tts.isSpeaking()) {
            tts.stop();
        }
        callbackContext.success();
    }

    // 检测讯飞语音引擎是否安装
    private void check(CallbackContext callbackContext) {
        Log.i(TAG, "check...");
        if (!checkApkExist(IFLYTEK)) {
            Message msg = new Message();
            msg.what = DIALOG;
            handler.sendMessage(msg);
            callbackContext.error("没有安装讯飞语音！");
        } else {
            callbackContext.success("成功了！");
        }
    }

    // 根据包名来检验手机里是否安装了某个应用
    private boolean checkApkExist(String packageName) {
        if (packageName == null || "".equals(packageName.trim())) {
            return false;
        }
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    // 使用浏览器打开某个网址
    private void installApk() {
        Uri uri = Uri.parse(IFLYAPK);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        this.cordova.startActivityForResult(this, intent, REQUEST);
    }

    @Override
    public void onDestroy() {
        if (context != null) {
            context = null;
        }
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
    }
}
