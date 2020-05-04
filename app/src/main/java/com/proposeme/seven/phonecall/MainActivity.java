package com.proposeme.seven.phonecall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.gyz.voipdemo_speex.util.Speex;
import com.proposeme.seven.phonecall.utils.HttpUtil;
import com.proposeme.seven.phonecall.utils.MLOC;
import com.proposeme.seven.phonecall.utils.PermissionManager;
import com.proposeme.seven.phonecall.utils.UserNameUtil;
import com.yanzhenjie.permission.Permission;

import java.io.IOException;

import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.proposeme.seven.phonecall.utils.NetUtils.getIPAddress;


public class MainActivity extends AppCompatActivity {

    private void initPermission() {
        //检查权限
        PermissionManager.requestPermission(MainActivity.this, new PermissionManager.Callback() {
            @Override
            public void permissionSuccess() {
                PermissionManager.requestPermission(MainActivity.this, new PermissionManager.Callback() {
                    @Override
                    public void permissionSuccess() {
                        PermissionManager.requestPermission(MainActivity.this, new PermissionManager.Callback() {
                            @Override
                            public void permissionSuccess() {

                            }
                            @Override
                            public void permissionFailed() {
                            }
                        }, Permission.Group.STORAGE);
                    }

                    @Override
                    public void permissionFailed() {

                    }
                }, Permission.Group.MICROPHONE);
            }

            @Override
            public void permissionFailed() {

            }
        }, Permission.Group.CAMERA);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
        ButterKnife.bind(this);

        MLOC.localIpAddress = getIPAddress(this);
        Speex.getInstance().init();
        /*if(checkIsFirstRun()){
            Intent intent = new Intent(this,RegisterActivity.class);
            startActivity(intent);
        }*/
        findViewById(R.id.phoneCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //updateOnlineState();
                VoipP2PActivity.newInstance(MainActivity.this);
            }
        });


        findViewById(R.id.Multi_phoneCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                updateOnlineState();

                MultiVoIpActivity.newInstance(MainActivity.this);
            }
        });

        Log.e("ccc", "path" + getFilesDir());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public boolean checkIsFirstRun(){
        SharedPreferences preferences = getSharedPreferences("isFirstRun",MODE_PRIVATE);
        boolean isFirstRun = preferences.getBoolean("key",true);
        if(isFirstRun){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("key",false);
            editor.apply();
        }
        return isFirstRun;
    }

    public void updateOnlineState(){
        String username = UserNameUtil.getUsername(this);
        HttpUtil.sendOkHttpRequest( MLOC.baseURl+"server/stateServlet1?username="+username+"&ip="+ getIPAddress(MainActivity.this), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"Online",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
