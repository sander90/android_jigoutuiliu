package com.sander.updateliudemo;

import android.app.Application;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.constants.ZegoAvConfig;
import com.zego.zegoliveroom.constants.ZegoConstants;

/**
 * Created by sander on 2017/11/29.
 */

public class LiuApplication extends Application {

    static final private long APP_ID =  3177435262L;

    static final private byte[] APP_SIGN_KEY = new byte[] {
            (byte)0x16, (byte)0x6c, (byte)0x57, (byte)0x8b, (byte)0xb0, (byte)0xb5, (byte)0x51, (byte)0xfd,
            (byte)0xc4, (byte)0xd9, (byte)0xb7, (byte)0xaf, (byte)0x96, (byte)0x1f, (byte)0x13, (byte)0x82,
            (byte)0xc9, (byte)0xb6, (byte)0x2b, (byte)0x0f, (byte)0x99, (byte)0x75, (byte)0x3a, (byte)0xb3,
            (byte)0xc1, (byte)0x7e, (byte)0xc4, (byte)0x54, (byte)0x30, (byte)0x93, (byte)0x28, (byte)0xfa
    };

    static final private  String BUGLY_APP_KEY = "1e4b3a1ac0";

    static private LiuApplication sInstance;

    private ZegoLiveRoom mZegoLiveRoom;




    private MainActivity.Streamhandler stream_header;

    static public LiuApplication getAppContext() {
        return sInstance;
    }

    public ZegoLiveRoom getZegoLiveRoom() {
        return mZegoLiveRoom;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        sd_configUser();

        sd_configStreamRoot();

        sd_configSDK();

    }

    private void sd_configUser()
    {
        String userId = PrefUtil.getInstance().getUserId();
        String userName = PrefUtil.getInstance().getUserName();
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(userName)) {
            userId = "sander1";
            userName = String.format("WWJS_%s_%s", Build.MODEL.replaceAll(",", "."), userId);

            PrefUtil.getInstance().setUserId(userId);
            PrefUtil.getInstance().setUserName(userName);
        }
    }

    public void sd_configStreamRoot()
    {
        // 这个是主流 默认的
        PrefUtil.getInstance().setStreamRoot(true);
        // 这个是从流
//        PrefUtil.getInstance().setStreamRoot(false);
    }

    private void sd_configSDK(){

        long ms = System.currentTimeMillis();
        String mUserID = PrefUtil.getInstance().getUserId();
        String userName = PrefUtil.getInstance().getUserName();

        // 测试环境开关
//        ZegoLiveRoom.setTestEnv(true);

        ZegoLiveRoom.requireHardwareEncoder(true);
        ZegoLiveRoom.requireHardwareDecoder(true);

        // 根据当前运行模式是否打开调试信息，仅供参考
        ZegoLiveRoom.setVerbose(BuildConfig.DEBUG);

        Log.d("sander","userid = " + mUserID);
        ZegoLiveRoom.setUser(mUserID,userName);

        mZegoLiveRoom = new ZegoLiveRoom();

        mZegoLiveRoom.setSDKContext(new ZegoLiveRoom.SDKContext() {
            @Override
            public String getSoFullPath() {
                return null;
            }

            @Override
            public String getLogPath() {
                return null;
            }

            @Override
            public Application getAppContext() {
                return sInstance;
            }
        });


        sd_initSDK(mZegoLiveRoom);

    }

    private void sd_initSDK(ZegoLiveRoom room)
    {
        try {
            boolean result = room.initSDK(APP_ID,APP_SIGN_KEY);

            if (!result){
                Log.d("sander","init sdk failed");
            }else{
                Log.d("sander","success!!!");

                // 推荐使用如下参数配置推流以达到最佳均衡效果
                // 采集分辨率：720 * 1280
                // 编码分辨率：480 * 640
                // 推流码率：600 * 1000 bps （在合适范围内，码率对视频效果基本无影响）
                int resolutionLevel;
                ZegoAvConfig config;
                int level = PrefUtil.getInstance().getLiveQuality();
                if (level < 0) {
                    // 默认设置级别为"标准"
                    resolutionLevel = ZegoAvConfig.Level.Generic;

                    config = new ZegoAvConfig(resolutionLevel);

                    // 保存默认设置
                    PrefUtil.getInstance().setLiveQuality(resolutionLevel);
                    PrefUtil.getInstance().setLiveQualityResolution(resolutionLevel);
                    PrefUtil.getInstance().setLiveQualityFps(15);
                    PrefUtil.getInstance().setLiveQualityBitrate(600 * 1000);
                } else if (level > ZegoAvConfig.Level.SuperHigh) {
                    resolutionLevel = PrefUtil.getInstance().getLiveQualityResolution();

                    config = new ZegoAvConfig(ZegoAvConfig.Level.High);
                    config.setVideoBitrate(PrefUtil.getInstance().getLiveQualityBitrate());
                    config.setVideoFPS(PrefUtil.getInstance().getLiveQualityFps());
                } else {
                    resolutionLevel = level;
                    config = new ZegoAvConfig(level);
                }

                String resolutionText = getResources().getStringArray(R.array.zg_resolutions)[resolutionLevel];
                String[] strWidthHeight = resolutionText.split("x");

                int height = Integer.parseInt(strWidthHeight[0].trim());
                int width = Integer.parseInt(strWidthHeight[1].trim());
                config.setVideoEncodeResolution(width, height);

                if (width <= 720 && height <= 1280) {
                    // 默认使用 720 * 1280 采集分辨率以达到最佳推流质量
                    config.setVideoCaptureResolution(720, 1280);
                } else {
                    config.setVideoCaptureResolution(width, height);
                }

                room.setAVConfig(config);
                room.setAVConfig(config, ZegoConstants.PublishChannelIndex.AUX);
            }
        }catch (Exception ex){
            Log.d("sander",ex.toString());
        }




    }


    public MainActivity.Streamhandler getStream_header() {
        return stream_header;
    }

    public void setStream_header(MainActivity.Streamhandler stream_header) {
        this.stream_header = stream_header;
    }
}
