package com.example.atomlzer30;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.liys.view.LineProView;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public  SerialPortThread serialPortThread;
    private long countdown1=0,countdown2=0;//设备倒计时时长
    private int taskTime1=0,taskTime2=0;//设备定时时长
    private int setTaskTime1=0,setTaskTime2=0;//设备定时时长
    private Timer timerTask;//计时器
    private boolean state1=false,state2=false;//设备状态
    private byte sendData=0x20;
    private int temperature=25;
    private int humidity =60;
    private int level=90;
    /***********控件初始化*************/
    protected DashboardView tempDashboardView,humDashboardView,levelDashboard;
    protected Button device1Button,device2Button;
    protected TextView lastTime1,lastTime2;
    protected CircleProgress mCpLoading;
    protected MyNumberPicker np1,np2;
    protected LineProView lineProView1,lineProView2;
    protected TextView temperatureTextView,humidityTextView;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        hideBottomUIMenu();

        device1Button=findViewById(R.id.device1Button);
        device2Button=findViewById(R.id.device2Button);

        lastTime1=findViewById(R.id.lastTime1);
        lastTime2=findViewById(R.id.lastTime2);

        np1 = findViewById(R.id.np1);
        np2 = findViewById(R.id.np2);

        lineProView1=findViewById(R.id.lineProView1);
        lineProView2=findViewById(R.id.lineProView2);

        temperatureTextView=findViewById(R.id.temperature);
        humidityTextView=findViewById(R.id.humidityTextView);

        device1Button.setOnClickListener(this);
        device2Button.setOnClickListener(this);

        temperatureTextView.setOnClickListener(this);

        np1.setMinValue(0);
        np1.setMaxValue(100);
        np1.setValue(5);

        setTaskTime1=np1.getValue();
        //Log.d("TAG","taskTime1：" + taskTime1);
        np1.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        np1.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            //当NunberPicker的值发生改变时，将会激发该方法
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setTaskTime1=newVal;
                //Log.d("TAG","oldVal：" + oldVal + "   newVal：" + newVal);
            }
        });

        np2.setMinValue(0);
        np2.setMaxValue(100);
        np2.setValue(5);
        np2.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        setTaskTime2=np2.getValue();
        Log.d("TAG","taskTime2：" + taskTime2);
        np2.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        np2.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            //当NunberPicker的值发生改变时，将会激发该方法
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setTaskTime2=newVal;
                //Log.d("TAG","oldVal：" + oldVal + "   newVal：" + newVal);
            }
        });

        mCpLoading = findViewById(R.id.cp_loading);
        //mCpLoading.setProgress(100,5000);
        mCpLoading.setProgress(90);
        mCpLoading.setOnCircleProgressListener(new CircleProgress.OnCircleProgressListener() {
            @Override
            public boolean OnCircleProgress(int progress) {
                return false;
            }
        });
        mCpLoading.setOnClickListener(this);
        new Udp.udpReceiveBroadCast(mHandler).start();
//        if (timerTask==null){
//            timerTask = new Timer(true);
//            timerTask.schedule(countTask, 500, 1000);
//        }
    }
    protected void hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {

            Window _window = getWindow();
            WindowManager.LayoutParams params = _window.getAttributes();
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE;
            _window.setAttributes(params);
        }
    }
    public TimerTask countTask = new TimerTask() {
        public void run() {
            long currentTime = System.currentTimeMillis();
            TaskData taskData1=new TaskData();
            if (currentTime>=countdown1){//结束
                sendData=(byte)(sendData&(~0x01));
                state1=false;
                sendHandler(1,taskData1);
            }else{//进行中
                sendData=(byte)(sendData|0x01);
                double progess=((double) (countdown1-currentTime))/(double)(taskTime1*60*1000);
                //Log.v("tag","progess:"+progess+"  "+(countdown1-currentTime)+"  "+(taskTime1*60*1000));
                taskData1.setProgess(progess);
                int time= (int) ((countdown1-currentTime)/1000);
                taskData1.setLastTime(time);
                sendHandler(1,taskData1);

            }
            TaskData taskData2=new TaskData();
            if (currentTime>=countdown2){//结束
                sendData=(byte)(sendData&(~0x02));
                state2=false;
                sendHandler(2,taskData2);
            }else{//进行中
                sendData=(byte)(sendData|0x02);
                double progess=((double) (countdown2-currentTime))/(double)(taskTime2*60*1000);
                //Log.v("tag","progess:"+progess+"  "+(countdown2-currentTime)+"  "+(taskTime2*60*1000));
                taskData2.setProgess(progess);
                int time= (int) ((countdown2-currentTime)/1000);
                taskData2.setLastTime(time);
                sendHandler(2,taskData2);
                byte[] sendBuf={0x25};
                //serialPortThread.sendSerialPort(sendBuf);
            }

            byte[] sendBuf={0};
            sendBuf[0]=sendData;
            //Log.d("TAG","sendData:"+sendData);
            //serialPortThread.sendSerialPort(sendBuf);
            //发送udp数据格式
            byte[] udpSendBuf=new byte[80];
            System.arraycopy(DateForm.intToBytesArray(temperature),0,udpSendBuf,0,4);
            System.arraycopy(DateForm.intToBytesArray(humidity),0,udpSendBuf,4,4);
            System.arraycopy(DateForm.intToBytesArray(level),0,udpSendBuf,8,4);
            System.arraycopy(DateForm.intToBytesArray((int)sendData),0,udpSendBuf,12,4);

            System.arraycopy(DateForm.intToBytesArray(taskTime1),0,udpSendBuf,16,4);
            System.arraycopy(DateForm.intToBytesArray(taskData1.getLastTime()),0,udpSendBuf,20,4);
            System.arraycopy(DateForm.doubleToByteArray(taskData1.getProgess()),0,udpSendBuf,24,8);

            System.arraycopy(DateForm.intToBytesArray(taskTime2),0,udpSendBuf,32,4);
            System.arraycopy(DateForm.intToBytesArray(taskData2.getLastTime()),0,udpSendBuf,36,4);
            System.arraycopy(DateForm.doubleToByteArray(taskData2.getProgess()),0,udpSendBuf,40,8);
            new Udp.udpSendBroadCast(udpSendBuf).start();
            //Log.d("TAG", Arrays.toString(udpSendBuf));

        }
    };

    private Handler mHandler  = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    byte[] udpRcvBuf=(byte[])msg.obj;

                    byte[] temperatureByte=new byte[4];
                    byte[] humidityByte=new byte[4];
                    byte[] levelByte=new byte[4];
                    byte[] sendDataByte=new byte[4];

                    byte[] taskTime1Byte=new byte[4];
                    byte[] LastTime1Byte=new byte[4];
                    byte[] Progess1Byte=new byte[8];

                    byte[] taskTime2Byte=new byte[4];
                    byte[] LastTime2Byte=new byte[4];
                    byte[] Progess2Byte=new byte[8];

                    byte[] taskTime3Byte=new byte[4];
                    byte[] LastTime3Byte=new byte[4];
                    byte[] Progess3Byte=new byte[8];

                    byte[] taskTime4Byte=new byte[4];
                    byte[] LastTime4Byte=new byte[4];
                    byte[] Progess4Byte=new byte[8];


                    System.arraycopy(udpRcvBuf,0,temperatureByte,0,4);
                    System.arraycopy(udpRcvBuf,4,humidityByte,0,4);
                    System.arraycopy(udpRcvBuf,8,levelByte,0,4);
                    System.arraycopy(udpRcvBuf,12,sendDataByte,0,4);

                    System.arraycopy(udpRcvBuf,16,taskTime1Byte,0,4);
                    System.arraycopy(udpRcvBuf,20,LastTime1Byte,0,4);
                    System.arraycopy(udpRcvBuf,24,Progess1Byte,0,8);

                    System.arraycopy(udpRcvBuf,32,taskTime2Byte,0,4);
                    System.arraycopy(udpRcvBuf,36,LastTime2Byte,0,4);
                    System.arraycopy(udpRcvBuf,40,Progess2Byte,0,8);

                    System.arraycopy(udpRcvBuf,48,taskTime3Byte,0,4);
                    System.arraycopy(udpRcvBuf,52,LastTime3Byte,0,4);
                    System.arraycopy(udpRcvBuf,56,Progess3Byte,0,8);

                    System.arraycopy(udpRcvBuf,64,taskTime4Byte,0,4);
                    System.arraycopy(udpRcvBuf,68,LastTime4Byte,0,4);
                    System.arraycopy(udpRcvBuf,72,Progess4Byte,0,8);

                    temperature=DateForm.byteArrayToInt(temperatureByte);
                    humidity=DateForm.byteArrayToInt(humidityByte);
                    level=DateForm.byteArrayToInt(levelByte);
                    sendData=(byte)DateForm.byteArrayToInt(sendDataByte);
                    mCpLoading.setProgress(level);
                    temperatureTextView.setText(temperature+"℃");
                    humidityTextView.setText(humidity+"％");

                    TaskData taskData1=new TaskData();
                    taskTime1=DateForm.byteArrayToInt(taskTime1Byte);
                    taskData1.setLastTime(DateForm.byteArrayToInt(LastTime1Byte)); //;
                    taskData1.setProgess(100-DateForm.byteArrayToDouble(Progess1Byte,0)*100);


                    TaskData taskData2=new TaskData();
                    taskTime2=DateForm.byteArrayToInt(taskTime2Byte);
                    taskData2.setLastTime(DateForm.byteArrayToInt(LastTime2Byte)); //;
                    taskData2.setProgess(100-DateForm.byteArrayToDouble(Progess2Byte,0)*100);
                    Log.d("TAG","getLastTime2:"+taskData2.getLastTime());


                    if ((sendData & 1) == 1){
                        lineProView1.setProgress(taskData1.getProgess());
                        String minutes=String.format("%0" + 2 + "d", taskData1.getLastTime()/60);
                        String second=String.format("%0" + 2 + "d", ((int)taskData1.getLastTime()%60));
                        lastTime1.setText(minutes+":"+second);
                        device1Button.setText("停止");
                        state1=true;
                    }else{
                        lineProView1.setProgress(0);
                        lastTime1.setText("00:00");
                        device1Button.setText("开始");
                        state1=false;
                    }

                    if ((sendData & 0x02) == 0x02){
                        lineProView2.setProgress(taskData2.getProgess());
                        String minutes=String.format("%0" + 2 + "d", taskData2.getLastTime()/60);
                        String second=String.format("%0" + 2 + "d", ((int)taskData2.getLastTime()%60));
                        lastTime2.setText(minutes+":"+second);
                        device2Button.setText("停止");
                        state2=true;
                    }else{
                        lineProView2.setProgress(0);
                        lastTime2.setText("00:00");
                        device2Button.setText("开始");
                        state2=false;
                    }


                    break;
            }
        }
    };

    private void sendHandler(int what,Object obj){
        Message msg = new Message();
        msg.what=what;
        msg.obj=obj;
        mHandler.sendMessage(msg);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.device1Button:
                if (!state1){
                    byte[] sendBuf=new byte[5];
                    device1Button.setText("停止");
                    sendBuf[0]=1;
                    System.arraycopy(DateForm.intToBytesArray(setTaskTime1),0,sendBuf,1,4);
                    state1=true;
                    new Udp.udpSendBroadCast(sendBuf).start();

                }else {
                    byte[] sendBuf=new byte[5];
                    device1Button.setText("开始");
                    sendBuf[0]=1;
                    System.arraycopy(DateForm.intToBytesArray(0),0,sendBuf,1,4);
                    state1=false;
                    new Udp.udpSendBroadCast(sendBuf).start();
                }
                break;
            case R.id.device2Button:
                if (!state2){
                    byte[] sendBuf=new byte[5];
                    device2Button.setText("停止");
                    sendBuf[0]=2;
                    System.arraycopy(DateForm.intToBytesArray(setTaskTime2),0,sendBuf,1,4);
                    state2=true;
                    new Udp.udpSendBroadCast(sendBuf).start();
                }else {
                    byte[] sendBuf=new byte[5];
                    device2Button.setText("开始");
                    sendBuf[0]=2;
                    System.arraycopy(DateForm.intToBytesArray(0),0,sendBuf,1,4);
                    state2=false;
                    new Udp.udpSendBroadCast(sendBuf).start();
                }
                break;
            case R.id.cp_loading:
                //Log.d("TAG","hello");
                //new Udp.udpSendBroadCast("hello").start();
//                new Udp.udpReceiveBroadCast(mHandler).start();
                break;
            case R.id.temperature:
                //new Udp.udpReceiveBroadCast().start();
                break;
        }
    }
}
