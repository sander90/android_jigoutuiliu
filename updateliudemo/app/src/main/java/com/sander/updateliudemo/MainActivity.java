package com.sander.updateliudemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sander.updateliudemo.callBack.ZegoLivePublisherCallback;
import com.sander.updateliudemo.callBack.ZegoLivePublisherCallback2;
import com.sander.updateliudemo.callBack.ZegoRoomCallback;
import com.zego.zegoliveroom.ZegoLiveRoom;
import com.zego.zegoliveroom.callback.IZegoLoginCompletionCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.constants.ZegoVideoViewMode;
import com.zego.zegoliveroom.entity.ZegoStreamInfo;
import com.zego.zegoliveroom.entity.ZegoUser;

import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity implements IStateChangedListener,IRoomClient  {
    static final private String ROOM_ID_PREFIX = "sander";
    static final private String STREAM_ID_PREFIX = "sanderSteam";

    static final private int REQUEST_PERMISSION_CODE = 101;


    private String[] mResolutionText;

    private ZegoLiveRoom mZegoLiveRoom;

    private HandlerThread mWorkThread;

    private Handler mWorkHandler;

    private Handler mRetryHandler;

    private TextureView mMainPreviewView;
    private TextureView mSecondPreviewView;

    private Button setting_button;
    private EditText inputNameText;

    private EditText inputRoomID;

    boolean isStarting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mZegoLiveRoom = ((LiuApplication )getApplication()).getZegoLiveRoom();

        mResolutionText = getResources().getStringArray(R.array.zg_resolutions);
        mWorkThread = new HandlerThread("worker_thread", Thread.NORM_PRIORITY);
        mWorkThread.start();

        mWorkHandler = new Handler(mWorkThread.getLooper());

        mRetryHandler = new RetryHandler(Looper.getMainLooper());


        ((LiuApplication )getApplication()).setStream_header(new Streamhandler());

        isStarting = false;
        initRoomAndStreamInfo();

        initCtrls();




    }



    @SuppressLint("WrongViewCast")
    private void initCtrls()
    {
        mMainPreviewView = (TextureView) findViewById(R.id.main_preview_view);
        mSecondPreviewView = (TextureView) findViewById(R.id.second_preview_view);

        mMainPreviewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("sander","主流");

                if (PrefUtil.getInstance().getStreamRoot()){
                    mZegoLiveRoom.stopPublishing(ZegoConstants.PublishChannelIndex.MAIN);
                }else{
                    mZegoLiveRoom.stopPublishing(ZegoConstants.PublishChannelIndex.AUX);
                }
                PrefUtil.getInstance().setStreamRoot(true);
                mZegoLiveRoom.stopPreview(ZegoConstants.PublishChannelIndex.AUX);
                startPreview(ZegoConstants.PublishChannelIndex.MAIN);
//                publishStream(ZegoConstants.PublishChannelIndex.MAIN);
            }
        });

        mSecondPreviewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("sander","从流");
                if (PrefUtil.getInstance().getStreamRoot()){
                    mZegoLiveRoom.stopPublishing(ZegoConstants.PublishChannelIndex.MAIN);
                }else{
                    mZegoLiveRoom.stopPublishing(ZegoConstants.PublishChannelIndex.AUX);
                }
                PrefUtil.getInstance().setStreamRoot(false);

                mZegoLiveRoom.stopPreview(ZegoConstants.PublishChannelIndex.MAIN);
                startPreview(ZegoConstants.PublishChannelIndex.AUX);
//                publishStream(ZegoConstants.PublishChannelIndex.AUX);
            }
        });

        inputNameText = (EditText)findViewById(R.id.inputName);
        inputNameText.setText(PrefUtil.getInstance().getUserId());



        inputNameText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                PrefUtil.getInstance().setUserId(v.getText().toString());

                // 重新登录吗
                String mUserID = PrefUtil.getInstance().getUserId();
                String userName = PrefUtil.getInstance().getUserName();

                ZegoLiveRoom.setUser(mUserID,userName);



                return false;
            }
        });

        inputRoomID = (EditText)findViewById(R.id.inputRoomid);

        inputRoomID.setText(PrefUtil.getInstance().getRoomId());

        inputRoomID.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                PrefUtil.getInstance().setRoomId(v.getText().toString());

                return false;
            }
        });

        setting_button = (Button)findViewById(R.id.button_setting);
        setting_button.setText("开始推送");
        setting_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("sander","开始");
                isStarting = !isStarting;

                if (!isStarting){
                    setting_button.setText("stop");
                    if (PrefUtil.getInstance().getStreamRoot()){
                        mZegoLiveRoom.stopPublishing(ZegoConstants.PublishChannelIndex.MAIN);
                    }else{
                        mZegoLiveRoom.stopPublishing(ZegoConstants.PublishChannelIndex.AUX);
                    }
                }else{
                    setting_button.setText("start");
                    boolean permission = checkOrRequestPermission();
                    if (permission) {

                        loginRoomAndPublishStream();


                    }
                }



            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        Log.d("sander","这个页面被销毁");
    }

    private boolean checkOrRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkCallingPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || getApplicationContext().checkCallingPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                 this.requestPermissions(new String[]{
                       Manifest.permission.CAMERA},REQUEST_PERMISSION_CODE);
                return false;
            }
        }
        return true;
    }

    private void startPreview( int channelIndex) {


        mZegoLiveRoom.enableMic(false);
        if (channelIndex == 0){
            mZegoLiveRoom.enableCamera(true, 0);
            mZegoLiveRoom.enableCamera(false,1);
            mZegoLiveRoom.setFrontCam(true, channelIndex);
            mZegoLiveRoom.setPreviewView(mMainPreviewView, channelIndex);

        }else{
            mZegoLiveRoom.enableCamera(true, 1);
            mZegoLiveRoom.enableCamera(false,0);
            mZegoLiveRoom.setFrontCam(true,channelIndex);
            mZegoLiveRoom.setPreviewView(mSecondPreviewView, channelIndex);

        }
        mZegoLiveRoom.enablePreviewMirror(false);
        mZegoLiveRoom.setPreviewViewMode(ZegoVideoViewMode.ScaleAspectFill, channelIndex);
        mZegoLiveRoom.startPreview(channelIndex);

        Log.d("sander","start preview ...  " + channelIndex);


    }

    private void loginRoomAndPublishStream() {


        boolean success = loginRoom(false);
        if (!success) { // 登录失败， 1 分钟后重试
            Log.d("sander","log in failed");

            mRetryHandler.removeMessages(RetryHandler.MSG_REPUBLISH_STREAM);
            mRetryHandler.sendEmptyMessageDelayed(RetryHandler.MSG_RELOGIN_ROOM, 60 * 1000);
        }

    }

    private void initRoomAndStreamInfo() {
        PrefUtil prefUtil = PrefUtil.getInstance();
        if (TextUtils.isEmpty(prefUtil.getRoomId())
                || TextUtils.isEmpty(prefUtil.getRoomName())
                || TextUtils.isEmpty(prefUtil.getStreamId())
                || TextUtils.isEmpty(prefUtil.getStreamId2())) {
            String deviceId = DeviceIdUtil.generateDeviceId(this);

            deviceId = "21312312312";

            String roomId = String.format("%s_%s", ROOM_ID_PREFIX, deviceId);
            PrefUtil.getInstance().setRoomId(roomId);

            // 对娃娃机名做特殊处理以区分是即构的还是开发者的
            if (deviceId.startsWith("12345_5432")) {
                int deviceNo = Integer.valueOf(deviceId.substring(deviceId.length() - 1));
                String roomName = getString(R.string.zg_text_wawaji_name_template, deviceNo);
                PrefUtil.getInstance().setRoomName(roomName);
            } else {
                PrefUtil.getInstance().setRoomName(roomId);
            }

            String streamId = String.format("%s_%s", STREAM_ID_PREFIX, deviceId);
            PrefUtil.getInstance().setStreamId(streamId);

            String streamId2 = String.format("%s_%s_2", STREAM_ID_PREFIX, deviceId);
            PrefUtil.getInstance().setStreamId2(streamId2);
        }


    }


    private boolean loginRoom(boolean retry){
        if (!retry){
            setupCallbacks();
        }

        mZegoLiveRoom.setRoomConfig(false, true);
        String roomId = PrefUtil.getInstance().getRoomId();
        String roomName = PrefUtil.getInstance().getRoomName();

        Log.d("sander", " roomid = " + roomId + "   " + roomName);
        return mZegoLiveRoom.loginRoom(roomId, roomName, ZegoConstants.RoomRole.Anchor, new IZegoLoginCompletionCallback() {
            @Override
            public void onLoginCompletion(int errorCode, ZegoStreamInfo[] streamList) {
                if (errorCode == 0) {
                    Log.d("sander","login success!!!");

                    if (PrefUtil.getInstance().getStreamRoot()){
                        publishStream(ZegoConstants.PublishChannelIndex.MAIN);
                    }else{
                        publishStream(ZegoConstants.PublishChannelIndex.AUX);
                    }
                } else {
                    Log.d("sander","log in errr");
                    mRetryHandler.removeMessages(RetryHandler.MSG_REPUBLISH_STREAM);
                    mRetryHandler.sendEmptyMessageDelayed(RetryHandler.MSG_RELOGIN_ROOM, 60 * 1000);
                }
            }
        });
    }
    private void publishStream(int channelIndex) {

        Log.d("sander","push stream...");
        mZegoLiveRoom.enableDTX(true);
        mZegoLiveRoom.setLatencyMode(ZegoConstants.LatencyMode.Low2);

        // 开启自动流量监控
        int properties = ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_FPS
                | ZegoConstants.ZegoTrafficControlProperty.ZEGOAPI_TRAFFIC_RESOLUTION;
        mZegoLiveRoom.enableTrafficControl(properties, true);

        String extraInfo = generateStreamExtraInfo();   // 为第一条流添加附加信息

        if (channelIndex == -1 || channelIndex == ZegoConstants.PublishChannelIndex.MAIN) {
            String streamId = PrefUtil.getInstance().getStreamId();

            boolean success = mZegoLiveRoom.startPublishing(streamId,"",ZegoConstants.PublishFlag.JoinPublish);
            if (!success) {
                Log.d("sander","publish 1 error");
                republishStreamDelay(ZegoConstants.PublishChannelIndex.MAIN);
            }else{
                Log.d("sander","publish 1 success!!!!");
            }
        }

        if (channelIndex == -1 || channelIndex == ZegoConstants.PublishChannelIndex.AUX) {
            String streamId2 = PrefUtil.getInstance().getStreamId2();
            boolean success = mZegoLiveRoom.startPublishing2(streamId2, "", ZegoConstants.PublishFlag.JoinPublish, ZegoConstants.PublishChannelIndex.AUX);
            if (!success) {
                Log.d("sander","publish 2 error");
                republishStreamDelay(ZegoConstants.PublishChannelIndex.AUX);
            }else{
                Log.d("sander","publish 2 success !!!!");
            }
        }
    }

    private void republishStreamDelay(int channelIndex) {
        Message msg = Message.obtain();
        msg.what = RetryHandler.MSG_REPUBLISH_STREAM;
        msg.arg1 = channelIndex;
        mRetryHandler.sendMessageDelayed(msg, 60 * 1000);
    }
    private String generateStreamExtraInfo() {
        JSONObject extraInfo = new JSONObject();
//        try {
//            extraInfo.put(Constants.JsonKey.KEY_USER_TOTAL, mTotalUsers.size());
//            extraInfo.put(Constants.JsonKey.KEY_QUEUE_NUMBER, mQueueUsers.size());
//
//            JSONObject player = new JSONObject();
//            player.put(Constants.JsonKey.KEY_USER_ID, mCurrentPlayer.userID);
//            player.put(Constants.JsonKey.KEY_USER_NAME, mCurrentPlayer.userName);
//            extraInfo.put(Constants.JsonKey.KEY_PLAYER, player);
//        } catch (JSONException e) {
//            AppLogger.getInstance().writeLog("create stream extra info failed. " + e);
//        }
        return extraInfo.toString();
    }

    private void setupCallbacks() {
        mZegoLiveRoom.setZegoLivePublisherCallback(new ZegoLivePublisherCallback(this));
        mZegoLiveRoom.setZegoLivePublisherCallback2(new ZegoLivePublisherCallback2(this));
        mZegoLiveRoom.setZegoRoomCallback(new ZegoRoomCallback(this, this));
//        mZegoLiveRoom.setZegoIMCallback(new ZegoIMCallack(this, this));
    }

    @Override
    public void onRoomStateUpdate() {

    }

    @Override
    public void onVideoCaptureSizeChanged(int width, int height, int channelIndex) {

    }

    @Override
    public void onPublishStateUpdate(int stateCode, String streamId) {
        Log.d("sander","publish state update " + streamId  + "  and " + stateCode);

        if (stateCode == 0){
            Log.d("sander",streamId + " publish success");
        }else{
            Log.d("sander",streamId + "publish error " + stateCode);
        }
    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public List<ZegoUser> getTotalUser() {
        return null;
    }

    @Override
    public List<ZegoUser> getQueueUser() {
        return null;
    }

    @Override
    public void updateCurrentPlayerInfo(String userId, String userName) {

    }

    @Override
    public ZegoLiveRoom getZegoLiveRoom() {
        return null;
    }

    @Override
    public void runOnWorkThread(Runnable task) {

    }

    private class RetryHandler extends Handler{
        static final int MSG_RELOGIN_ROOM = 1;
        static final int MSG_REPUBLISH_STREAM = 2;

        public RetryHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RELOGIN_ROOM:
//                    AppLogger.getInstance().writeLog("relogin room");
//                    loginRoom(true);
                    break;

                case MSG_REPUBLISH_STREAM:
                    int channelIndex = msg.arg1;
//                    AppLogger.getInstance().writeLog("republish stream: %d", channelIndex);
//                    publishStream(channelIndex);
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    }


    public class Streamhandler extends Handler{
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == 1){
                // 设置主流
                Log.d("sander","主流");

                mZegoLiveRoom.stopPreview(ZegoConstants.PublishChannelIndex.AUX);
                startPreview(ZegoConstants.PublishChannelIndex.MAIN);
                publishStream(ZegoConstants.PublishChannelIndex.MAIN);
            }else if (msg.what == 2){
                // 设置成从流
                Log.d("sander","从流");
                mZegoLiveRoom.stopPreview(ZegoConstants.PublishChannelIndex.MAIN);
                startPreview(ZegoConstants.PublishChannelIndex.AUX);
                publishStream(ZegoConstants.PublishChannelIndex.AUX);

            }else{
                super.handleMessage(msg);
            }
        }
    }


    /**
     * api 23
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSION_CODE: {
                boolean allPermissionGranted = true;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        allPermissionGranted = false;
                        Toast.makeText(this, "failed", Toast.LENGTH_LONG).show();
                    }
                }
                if (allPermissionGranted) {
                    if (PrefUtil.getInstance().getStreamRoot()){
                        startPreview(ZegoConstants.PublishChannelIndex.MAIN);
                    }else{
                        startPreview(ZegoConstants.PublishChannelIndex.AUX);
                    }

                    loginRoomAndPublishStream();
                } else {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
            break;
        }
    }
}

