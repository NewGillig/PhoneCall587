package com.proposeme.seven.phonecall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.R.layout;
import android.widget.TextView;

import com.proposeme.seven.phonecall.audio.AudioDecoder;
import com.proposeme.seven.phonecall.audio.AudioRecorder;
import com.proposeme.seven.phonecall.net.CallSignal;
import com.proposeme.seven.phonecall.net.NettyReceiverHandler;
import com.proposeme.seven.phonecall.provider.EncodeProvider;
import com.proposeme.seven.phonecall.utils.MLOC;
import com.proposeme.seven.phonecall.utils.mixAduioUtils.AudioUtil;
import com.proposeme.seven.phonecall.utils.mixAduioUtils.FileUtils;
import com.proposeme.seven.phonecall.utils.mixAduioUtils.MixAudioUtil;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.util.NetUtil;


//实现多人语音通话的活动，现在只是实现两个音轨的合成。
public class MultiVoIpActivity extends AppCompatActivity implements View.OnClickListener {

    private AudioUtil mAudioUtil;
    private static final int BUFFER_SIZE = 1024 * 2;
    private byte[] mBuffer;

    private int page = 0;

    private EncodeProvider provider;
    private AudioRecorder audioRecorder;

    private boolean isBusy = false;
    private boolean isAnswer = false;

    private int port = 7777;
    private int localPort = 7777;
    private String TargetIp = "127.0.0.1";
    private String newEndIp = "127.0.0.1";
    private ArrayAdapter adaptor;

    private TextView mEditText = null;

    private AudioDecoder[] audioDecoders = null;


    private boolean pickSucc = false;

    private MultiVoIpActivity self= this;

    private ExecutorService mExecutorService;
    private static final String TAG = "MainActivity";


    private ArrayList<String> hosts;
    private ArrayList<byte[]> bMulRoadAudioes;

    //跳转activity
    public static void newInstance(Context context) {
        context.startActivity(new Intent(context, MultiVoIpActivity.class));
    }

    Handler timeHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            timeHandler.sendEmptyMessageDelayed(0,2000);
        }
    };

    //return code listener
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {

            if (msg.what == 100) {
                    showList();
                    MLOC.remoteList=hosts;
            }
        }
    };


    private void netInit(){


        provider = new EncodeProvider(TargetIp, port, localPort, new NettyReceiverHandler.FrameResultedCallback() {

            @Override
            public void onTextMessage(String msg) {

                for (int i = 0; i < hosts.size(); i++) {
                    for (int j = 0; j < hosts.size(); j++)
                        provider.UserIPSentTestData(hosts.get(i), hosts.get(j));
                }
                mHandler.sendEmptyMessage(100);
                if(page==1&&msg.contains(".")&&!hosts.contains(msg)&&!msg.equals(MLOC.localIpAddress)&&!msg.equals("127.0.0.1")) {
                    hosts.add(msg);
                    Log.e("ccc","GETIPIPIPIPIP"+msg);
                    mHandler.sendEmptyMessage(100);
                    for(int k = 0;k<2000;k++) {
                        for (int i = 0; i < hosts.size(); i++) {
                            for (int j = 0; j < hosts.size(); j++)
                                provider.UserIPSentTestData(hosts.get(i), hosts.get(j));
                        }
                    }
                }
                if (CallSignal.PHONE_MAKE_CALL.equals(msg)){

                    //mHandler.sendEmptyMessage(phone_make_call);
                    if(page==1) {
                        int count=0;
                        Log.e("ccc","MSG: "+msg);

                        if(!hosts.contains(TargetIp)&&TargetIp!="127.0.0.1") {
                            hosts.add(TargetIp);
                            for(int k = 0;k<2000;k++) {
                                for (int i = 0; i < hosts.size(); i++) {
                                    for (int j = 0; j < hosts.size(); j++)
                                        provider.UserIPSentTestData(hosts.get(i), hosts.get(j));
                                }
                            }
                            Log.e("ccc","HOSTSIZE========================"+hosts.size());
                            //Log.e("ccc","BSIZE========================"+bMulRoadAudioes.size());
                            mHandler.sendEmptyMessage(100);
                            Log.e("ccc", "Connected by: " + TargetIp);
                            //ArrayAdapter adaptor = new ArrayAdapter<String>(self,layout.simple_list_item_1,hosts);
                            //ListView listView = (ListView) findViewById(R.id.list_client);
                            //listView.setAdapter(adaptor);
                            //setContentView(R.layout.activity_multi_voip_host);
                            /*Log.e("ccc", "receive make call ");
                            for (int i = 0; i < 2; i++) {
                                SystemClock.sleep(1);
                                provider.UserIPSentTestData(TargetIp, CallSignal.SEND_OK);
                                Log.e("ccc", "Send OK to " + TargetIp);
                            }
                            pickSucc = false;
                            for (int i = 0; i < 50; i++) {
                                if (pickSucc)
                                    break;
                                SystemClock.sleep(2);
                                provider.UserIPSentTestData(TargetIp, CallSignal.PHONE_ANSWER_CALL);
                                Log.e("", "try call");
                            }*/
                        }
                    }
                }else if (CallSignal.PHONE_ANSWER_CALL.equals(msg)){

                    //mHandler.sendEmptyMessage(phone_answer_call);
                    Log.e("ccc", "receive answer");
                    for(int i=0;i<2;i++){
                        SystemClock.sleep(1);
                        provider.UserIPSentTestData(TargetIp, CallSignal.PICK_OK);
                        Log.e("ccc","Send OK to "+TargetIp);
                    }
                }else if (isBusy&&CallSignal.PHONE_CALL_END.equals(msg)){

                    //mHandler.sendEmptyMessage(phone_call_end);
                    Log.e("ccc", "receive phone_end");
                    provider.shutDownSocket();
                    netInit();
                }
                else if(CallSignal.SEND_OK.equals(msg))
                {
                    //sendSucc = true;
                    //mHandler.sendEmptyMessage(send_OK);

                    Log.e("ccc","Make Call Success");
                }
                else if(CallSignal.PICK_OK.equals(msg))
                {
                    pickSucc = true;
                    //mHandler.sendEmptyMessage(pick_OK);

                    Log.e("ccc","Pickup Success");
                }

            }

            @Override
            public void onAudioData(byte[] data) {
                audioDecoders[hosts.indexOf(TargetIp)].addData(data,data.length);
                //AudioDecoder.getInstance().addData(data, data.length);
            }

            @Override
            public void onGetRemoteIP(String ip) {
                newEndIp = ip;
                Log.e("ccc", "receive ipx" + ip);
                if ((!ip.equals(""))){
                    MLOC.remoteIpAddress = ip;
                    TargetIp = MLOC.remoteIpAddress;
                }
            }
        });

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        netInit();

        audioRecorder = new AudioRecorder();

        audioDecoders = new AudioDecoder[10];
        for(int i=0;i<audioDecoders.length;i++)
        {
            audioDecoders[i] = new AudioDecoder();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_voip);

        findViewById(R.id.join_a_room).setOnClickListener(this);
        findViewById(R.id.open_a_room).setOnClickListener(this);




        /*findViewById(R.id.first_stop_record_button).setOnClickListener(this);
        findViewById(R.id.first_play).setOnClickListener(this);

        findViewById(R.id.second_start_record_button).setOnClickListener(this);
        findViewById(R.id.second_stop_record_button).setOnClickListener(this);
        findViewById(R.id.second_play).setOnClickListener(this);

        findViewById(R.id.beginMix).setOnClickListener(this);
        findViewById(R.id.MixaudioPlayer).setOnClickListener(this);*/
        mBuffer = new byte[BUFFER_SIZE];

        mExecutorService = Executors.newSingleThreadExecutor();
        mAudioUtil = AudioUtil.getInstance();
    }

    public void showList()
    {
        this.adaptor = new ArrayAdapter<String>(self,layout.simple_list_item_1,hosts);
        ListView listView = (ListView) findViewById(R.id.list_client);
        listView.setAdapter(this.adaptor);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            //第一个录音操作
            case R.id.open_a_room:
                hosts = new ArrayList<>();
                setContentView(R.layout.activity_multi_voip_host);
                showList();
                isAnswer = true;
                page=1;
                MLOC.localIpAddress = com.proposeme.seven.phonecall.utils.NetUtils.getIPAddress(this);
                ((TextView)findViewById(R.id.create_ip_addr)).setText("Your IP: "+ MLOC.localIpAddress);
                audioRecorder.startRecording();;






                /*
                mAudioUtil.createFile("firstPcm.pcm");
                mAudioUtil.startRecord();
                mAudioUtil.recordData();*/
                break;
            case R.id.join_a_room:
                setContentView(R.layout.activity_multi_voip_ip);
                findViewById(R.id.user_input_phoneCall).setOnClickListener(this);
                mEditText = findViewById(R.id.user_input_TargetIp);
                break;
            case R.id.user_input_phoneCall:
                audioRecorder.startRecording();
                TargetIp = mEditText.getText().toString();
                MLOC.remoteIpAddress = TargetIp;

                //Send a calling message
                for(int i=0;i<400;i++){
                    SystemClock.sleep(5);
                    provider.UserIPSentTestData(TargetIp, CallSignal.PHONE_MAKE_CALL);
                    Log.e("", "try call");

                }

                isBusy = true;


                hosts = new ArrayList<>();
                setContentView(R.layout.activity_multi_voip_host);
                hosts.add(TargetIp);
                showList();
                isAnswer = true;
                page = 1;
                MLOC.localIpAddress = com.proposeme.seven.phonecall.utils.NetUtils.getIPAddress(this);
                ((TextView) findViewById(R.id.create_ip_addr)).setText("Your IP: " + MLOC.localIpAddress);

                break;
            /*case R.id.first_play: // 录音播放
                audioPlayer("firstPcm.pcm");
                break;

            //第二个录音操作
            case R.id.second_start_record_button:
                mAudioUtil.createFile("secondPcm.pcm");
                mAudioUtil.startRecord();
                mAudioUtil.recordData();
                break;
            case R.id.second_stop_record_button:
                mAudioUtil.stopRecord();
                break;
            case R.id.second_play:
                audioPlayer("secondPcm.pcm");
                break;

            //混音操作
            case R.id.beginMix:  //混音开始
                try {
                    byte[] realMixAudio = MixAudioUtil.averageMix(FileUtils.getFileBasePath()+"firstPcm.pcm",FileUtils.getFileBasePath()+"secondPcm.pcm");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.MixaudioPlayer:  //混音播放
                audioPlayer("averageMix.pcm");
                break;*/
        }
    }
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
        try{

        if(page==0) {
            provider.shutDownSocket();
            final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        }
        else if(page==1)
        {
            setContentView(R.layout.activity_multi_voip);
            provider.shutDownSocket();
            netInit();
            findViewById(R.id.join_a_room).setOnClickListener(this);
            findViewById(R.id.open_a_room).setOnClickListener(this);
            page=0;
        }}catch(Exception e){}

    }
    private void audioPlayer(String fileName) {
        //在播放的时候需要提前设置好录音文件。
        final File mAudioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/record/"+ fileName);
        mExecutorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                playAudio(mAudioFile); //读入传入的文件。
            }
        });
    }

    //将pcm文件读入并且进行播放。
    private void playAudio(File audioFile) //读入的是pcm文件。
    {
        Log.d("MainActivity" , "播放开始");
        int streamType = AudioManager.STREAM_MUSIC;   //按照音乐流进行播放
        int simpleRate = 44100;   //播放的赫兹
        int channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int mode = AudioTrack.MODE_STREAM;

        int minBufferSize = AudioTrack.getMinBufferSize(simpleRate , channelConfig , audioFormat);
        AudioTrack audioTrack = new AudioTrack(streamType , simpleRate , channelConfig , audioFormat ,
                Math.max(minBufferSize , BUFFER_SIZE) , mode);
        audioTrack.play();
        Log.d(TAG , minBufferSize + " is the min buffer size , " + BUFFER_SIZE + " is the read buffer size");

        FileInputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream(audioFile); //读入pcm文件。
            int read;
            while ((read = inputStream.read(mBuffer)) > 0)
            {
                Log.d("MainActivity" , "录音开始 kaishi11111");

                audioTrack.write(mBuffer , 0 , read); //将文件流添加播放
            }
        }
        catch (RuntimeException | IOException e)
        {
            e.printStackTrace();
        }
    }
}
