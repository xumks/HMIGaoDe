package com.example.cvm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class CarActivity extends AppCompatActivity {

    private Button back;
    private TextView car_info;

    private Timer mytimer;
    private TimerTask checkStatus;
    private Integer interval = -1;

    DecimalFormat format = new DecimalFormat("0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car);
        getWindow().setLayout(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);


        initUI(savedInstanceState);

        // timer
        mytimer = new Timer();
        checkStatus = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if( interval>0) {
                            interval--;
                        }
                        else {
                            car_info.setText("ID:       " + MainActivity.instance.hostVehicle.id + "\n"
                                    + "经度：    " + MainActivity.instance.hostVehicle.longitude + "\n"
                                    + "纬度：    " + MainActivity.instance.hostVehicle.latitude + "\n"
                                    + "速度：    " + format.format(Math.ceil(MainActivity.instance.hostVehicle.speed * 3.6 ))+ " km/h\n"
                                    + "航向角：  " + format.format(Math.ceil(MainActivity.instance.hostVehicle.heading)) + " 度\n");
                            interval = 10;
                        }
                    }
                });
            }};
        mytimer.schedule(checkStatus, 100, 100);


        back.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                //把返回数据存入Intent
                intent.putExtra("result", "1");
                //设置返回数据
                setResult(RESULT_OK, intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            }
        });
    }

    private void initUI(Bundle savedInstanceState){
        back = (Button)findViewById(R.id.car_back);
        car_info = (TextView)findViewById(R.id.car_info);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mytimer = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent();
        //把返回数据存入Intent
        intent.putExtra("result", "1");
        //设置返回数据
        setResult(RESULT_OK, intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }
}


