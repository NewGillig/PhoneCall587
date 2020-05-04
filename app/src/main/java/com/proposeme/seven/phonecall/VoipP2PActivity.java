package com.proposeme.seven.phonecall;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.proposeme.seven.phonecall.audio.AudioDecoder;
import com.proposeme.seven.phonecall.audio.AudioRecorder;
import com.proposeme.seven.phonecall.net.CallSignal;
import com.proposeme.seven.phonecall.net.NettyReceiverHandler;
import com.proposeme.seven.phonecall.provider.EncodeProvider;
import com.proposeme.seven.phonecall.users.User;
import com.proposeme.seven.phonecall.users.UserAdapter;
import com.proposeme.seven.phonecall.utils.HttpUtil;
import com.proposeme.seven.phonecall.utils.MLOC;
import com.proposeme.seven.phonecall.utils.NetUtils;
import com.proposeme.seven.phonecall.utils.UserNameUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.proposeme.seven.phonecall.utils.NetUtils.getIPAddress;


public class VoipP2PActivity extends AppCompatActivity implements View.OnClickListener{

    private Chronometer timer;
    private String TargetIp = "127.0.0.1";

    private int page = 0;

    private boolean sendSucc = false;
    private boolean pickSucc = false;

    private EncodeProvider provider;
    private AudioRecorder audioRecorder;

    private boolean isAnswer = false;
    private boolean isBusy = false;
    private int port = 7777;
    private int localPort = 7777;
    private String newEndIp = "127.0.0.1";

    private EditText mEditText;
    //return code
    private  final int phone_make_call = 100;
    private  final int phone_answer_call = 200;
    private  final int phone_call_end = 300;
    private  final int send_OK = 500;
    private  final int pick_OK=501;



    private CountDownTimer mCountDownTimer; //Calling Time out counter

    ListView listView;
    UserAdapter adapter;
    List<User> userList = new ArrayList<>();


    @SuppressLint("HandlerLeak")
    Handler timeHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            updateData();
            timeHandler.sendEmptyMessageDelayed(0,2000);
        }
    };

    //return code listener
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {

            if (msg.what == phone_make_call) {
                if (!isBusy){
                    showRingView();
                    isBusy = true;

                }
            }else if (msg.what == phone_answer_call){
                showTalkingView();
                audioRecorder.startRecording();
                isAnswer = true;
            }else if (msg.what == phone_call_end){
                if (isBusy&&newEndIp.equals(TargetIp)){
                    showBeginView();
                    isAnswer = false;
                    isBusy = false;
                    audioRecorder.stopRecording();
                    //timer.stop();
                }
            }else if (msg.what == send_OK){
                sendSucc = true;
            }
        }
    };

    //跳转activity
    public static void newInstance(Context context) {
        context.startActivity(new Intent(context, VoipP2PActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);

        audioRecorder = new AudioRecorder();
        showBeginView();

        //init network

        netInit();

        /*
        findViewById(R.id.calling_view).setVisibility(View.GONE);
        findViewById(R.id.talking_view).setVisibility(View.GONE);
        findViewById(R.id.ring_view).setVisibility(View.GONE);
        findViewById(R.id.begin_view).setVisibility(View.GONE);
        findViewById(R.id.user_input_ip_view).setVisibility(View.GONE);


        timer = findViewById(R.id.timer);

        //设置挂断按钮
        findViewById(R.id.calling_hangup).setOnClickListener(this);
        findViewById(R.id.talking_hangup).setOnClickListener(this);
        findViewById(R.id.ring_pickup).setOnClickListener(this);
        findViewById(R.id.ring_hang_off).setOnClickListener(this);
        //设置手动输入ip
        findViewById(R.id.Create_button).setOnClickListener(this);
        findViewById(R.id.user_input_phoneCall).setOnClickListener(this);

        mEditText = findViewById(R.id.user_input_TargetIp);

        //
        //getServerUserList(); //获取服务器列表

        //拨打电话倒计时计时器。倒计时10s
        mCountDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                if (!isAnswer){ //如果没有人应答，则挂断
                    hangupOperation(TargetIp);
                    Toast.makeText(VoipP2PActivity.this,"打电话超时，请稍后再试！",Toast.LENGTH_SHORT).show();
                }

            }
        };*/
    }

    //init network
    private void netInit(){

        provider = new EncodeProvider(TargetIp, port, localPort, new NettyReceiverHandler.FrameResultedCallback() {

            @Override
            public void onTextMessage(String msg) {
                if (CallSignal.PHONE_MAKE_CALL.equals(msg)){

                    mHandler.sendEmptyMessage(phone_make_call);
                    Log.e("ccc", "receive make call ");
                    for(int i=0;i<2;i++){
                        SystemClock.sleep(1);
                        provider.UserIPSentTestData(TargetIp, CallSignal.SEND_OK);
                        Log.e("ccc","Send OK to "+TargetIp);
                    }
                }else if (CallSignal.PHONE_ANSWER_CALL.equals(msg)){

                    mHandler.sendEmptyMessage(phone_answer_call);
                    Log.e("ccc", "receive answer");
                    for(int i=0;i<2;i++){
                        SystemClock.sleep(1);
                        provider.UserIPSentTestData(TargetIp, CallSignal.PICK_OK);
                        Log.e("ccc","Send OK to "+TargetIp);
                    }
                }else if (isBusy&&CallSignal.PHONE_CALL_END.equals(msg)){

                    mHandler.sendEmptyMessage(phone_call_end);
                    Log.e("ccc", "receive phone_end");
                    provider.shutDownSocket();
                    netInit();
                }
                else if(CallSignal.SEND_OK.equals(msg))
                {
                    sendSucc = true;
                    mHandler.sendEmptyMessage(send_OK);

                    Log.e("ccc","Make Call Success");
                }
                else if(CallSignal.PICK_OK.equals(msg))
                {
                    pickSucc = true;
                    mHandler.sendEmptyMessage(pick_OK);

                    Log.e("ccc","Pickup Success");
                }
            }

            @Override
            public void onAudioData(byte[] data) {
                if (isAnswer){
                    AudioDecoder.getInstance().addData(data, data.length);
                }
            }

            @Override
            public void onGetRemoteIP(String ip) {
                newEndIp = ip;
                Log.e("ccc", "receive ip" + ip);
                if ((!ip.equals(""))){
                    MLOC.remoteIpAddress = ip;
                    TargetIp = MLOC.remoteIpAddress;
                }
            }
        });

    }


    //backpress
    @Override
    public void onBackPressed(){
        /*
        new AlertDialog.Builder(VoipP2PActivity.this).setCancelable(true)
                .setTitle("是否退出?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        timer.stop();
                        finish(); //确定以后调用退出方法
                    }
                }
        ).show();*/
        try {

            if (page == 0) {
                provider.shutDownSocket();
                final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                finish();
            } else if (page == 1){
                showBeginView();
                provider.shutDownSocket();
                netInit();
            }
        }catch(Exception e){}
    }


    private void showBeginView(){
        MLOC.localIpAddress = getIPAddress(this);
        setContentView(R.layout.activity_voip_p2p_init);
        page=0;
        findViewById(R.id.Create_button).setOnClickListener(this);
        ((TextView)findViewById(R.id.create_ip_addr)).setText("Your IP: "+MLOC.localIpAddress);
    }


    private void showUserInputIpView(){
        setContentView(R.layout.activity_voip_p2p_ip);
        findViewById(R.id.user_input_phoneCall).setOnClickListener(this);
        mEditText = findViewById(R.id.user_input_TargetIp);
        page=1;
    }

    private void showCallingView(){
        setContentView(R.layout.activity_voip_p2p_calling);
        page=2;
        findViewById(R.id.calling_hangup).setOnClickListener(this);
        SystemClock.sleep(3000);
        showTalkingView();
    }

    private void showTalkingView(){
        setContentView(R.layout.activity_voip_p2p_talking);
        findViewById(R.id.talking_hangup).setOnClickListener(this);
        page=3;
        ArrayList<String> a = new ArrayList<String>();
        a.add(TargetIp);
        MLOC.remoteList = a;
        timer = findViewById(R.id.timer);
        timer.stop();
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
    }

    //显示响铃界面
    private void showRingView(){
        page=4;
        setContentView(R.layout.activity_voip_p2p_ringing);
        ((TextView)findViewById(R.id.ring_targetid_text)).setText(TargetIp);
        findViewById(R.id.ring_pickup).setOnClickListener(this);
        findViewById(R.id.ring_hang_off).setOnClickListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        provider.shutDownSocket();
        //updateOfflineState(); //进行下线操作
    }

    //设置点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ring_pickup:
                pickSucc = false;
                for(int i=0;i<1000;i++){
                    if (pickSucc)
                        break;
                    SystemClock.sleep(2);
                    provider.UserIPSentTestData(TargetIp, CallSignal.PHONE_ANSWER_CALL);
                    Log.e("", "try call");
                }
                if(!pickSucc)
                {
                    new AlertDialog.Builder(VoipP2PActivity.this).setCancelable(true)
                            .setTitle("Cannot Call.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface arg0, int arg1) {
                                            provider.shutDownSocket();
                                            netInit();
                                            showBeginView();
                                        }
                                    }
                            ).show();

                }
                else {
                    showTalkingView();
                    isBusy = true;
                    audioRecorder.startRecording();
                    isAnswer = true;
                }
                break;
            case R.id.calling_hangup:
                provider.UserIPSentTestData(TargetIp,CallSignal.PHONE_CALL_END);
                isBusy = false;
                showBeginView();
                isAnswer = false;
                //audioRecorder.stopRecording(); //关闭录音和发送数据
                //timer.stop();
                break;
            case R.id.talking_hangup:
                provider.UserIPSentTestData(TargetIp,CallSignal.PHONE_CALL_END);
                isBusy = false;
                showBeginView();
                isAnswer = false;
                audioRecorder.stopRecording(); //关闭录音和发送数据
                timer.stop();
                break;
            case R.id.ring_hang_off:
                provider.UserIPSentTestData(TargetIp,CallSignal.PHONE_CALL_END);
                isBusy = false;
                showBeginView();
                isAnswer = false;
                //audioRecorder.stopRecording(); //关闭录音和发送数据
                //timer.stop();
                break;
            case R.id.Create_button:
                showUserInputIpView();
                break;
            case R.id.user_input_phoneCall:
                TargetIp = mEditText.getText().toString();
                MLOC.remoteIpAddress = TargetIp;

                //Send a calling message
                sendSucc = false;
                for(int i=0;i<400;i++){
                    if (sendSucc)
                        break;
                    SystemClock.sleep(5);
                    provider.UserIPSentTestData(TargetIp, CallSignal.PHONE_MAKE_CALL);
                    Log.e("", "try call");

                }
                if(!sendSucc)
                {
                    new AlertDialog.Builder(VoipP2PActivity.this).setCancelable(true)
                            .setTitle("Cannot Call.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    provider.shutDownSocket();
                                    netInit();
                                    showBeginView();
                                }
                            }
                    ).show();

                }
                else {
                    showCallingView();
                    isBusy = true;
                }


                break;
        }
    }

    //进行挂断电话时候的逻辑
    private void hangupOperation(String targetIp){
        provider.UserIPSentTestData(targetIp,CallSignal.PHONE_CALL_END);
        isBusy = false;
        showBeginView();
        isAnswer = false;
        //audioRecorder.stopRecording(); //关闭录音和发送数据
        //timer.stop();
        //mCountDownTimer.cancel();
    }

    //退出时候需要进行下线。
    public void updateOfflineState(){
        String username = UserNameUtil.getUsername(this);

        HttpUtil.sendOkHttpRequest(MLOC.baseURl+"server/stateServlet?username="+username, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }

    //获取用户列表
    /*private void getServerUserList() {
        //初始化网络ip信息显示
        listView = findViewById(R.id.list_view);
        adapter = new UserAdapter(this,R.layout.item_layout,userList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                User user = userList.get(i);
                 final String inputId = user.getIp();
                listMakeCall(inputId);
            }
        });
        timeHandler.sendEmptyMessageDelayed(0,2000);
    }*/

    //点击列表时候获得到的ip。并且进行拨打电话操作。最初的操作。
    private void listMakeCall(String inputId){
        showCallingView();
        //2 发送一条拨打电话的信息。
        TargetIp = inputId;
        MLOC.remoteIpAddress = inputId; //需要更新目标ip
        isBusy = true;
        provider.UserIPSentTestData(TargetIp,CallSignal.PHONE_MAKE_CALL);
        Log.e("ccc", "获取IP" + inputId);
    }

    //更新用户列表
    public void updateData(){
        HttpUtil.sendOkHttpRequest(MLOC.baseURl+ "server/dataServlet?ip="+ NetUtils.getIPAddress(this), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();

                if (responseData!=null){
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<User>>(){}.getType();
                    List<User> list = gson.fromJson(responseData, type);
                    userList.clear();
                    for (User user:list){
                        userList.add(user);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }
}
