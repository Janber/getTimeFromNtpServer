package jp.co.mbpsoft.gettimefromntpserver;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    Handler oshandler = new Handler();

    Handler nfthandler = new Handler();

    private TextView androidTime;
    private TextView nftTime;

    private Button osButton;
    private Button syncButton;

    Date ntpCurrent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());

        androidTime = (TextView) this.findViewById(R.id.android_text);
        nftTime = (TextView) this.findViewById(R.id.nft_text);

        osButton = (Button) this.findViewById(R.id.osButton);
        syncButton = (Button) this.findViewById(R.id.syncButton);


        Date current = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
        String datetime = df.format(current);

        androidTime.setText(datetime);

        oshandler = new Handler() {
            public void handleMessage(Message msg) {
                androidTime.setText((String) msg.obj);
            }
        };
        OsThreads thread = new OsThreads();
        thread.start();


        SntpClient client = new SntpClient();
        if (client.requestTime("0.jp.pool.ntp.org", 30000)) {
            long now = client.getNtpTime() + System.nanoTime() / 1000
                    - client.getNtpTimeReference();
            Date ntpCurrent = new Date(now);
            SimpleDateFormat df2 = new SimpleDateFormat("yyyyMMdd.HHmmss");
            String datetime2 = df2.format(ntpCurrent);
            System.out.println(datetime2.toString());
//            nftTime.setText(datetime2.toString());

            nfthandler = new Handler() {
                public void handleMessage(Message msg) {
                    nftTime.setText((String) msg.obj);
                }
            };
            NftThreads thread2 = new NftThreads();
            thread2.start();

        }else {
            nftTime.setText("ntp時間取得失敗！");
        }

        syncButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {

//                    SntpClient client = new SntpClient();
//                    long now = client.getNtpTime() + System.nanoTime() / 1000
//                            - client.getNtpTimeReference();
//                    SystemClock.setCurrentTimeMillis(now);

                    if (ShellUtils.checkRootPermission()) {
                        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));

                        SntpClient client = new SntpClient();
                        if (client.requestTime("0.jp.pool.ntp.org", 30000)) {
                            long now1 = client.getNtpTime() + System.nanoTime() / 1000
                                    - client.getNtpTimeReference();
                            ntpCurrent = new Date(now1);
                            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
                            String datetime = df.format(ntpCurrent);
                            Process process = Runtime.getRuntime().exec("su");
                            DataOutputStream os = new DataOutputStream(process.getOutputStream());
                            //os.writeBytes("setprop persist.sys.timezone GMT\n");
                            os.writeBytes("/system/bin/date -s " + datetime + "\n");
                            os.writeBytes("clock -w\n");
                            os.writeBytes("exit\n");
                            os.flush();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "no rootPermission", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                }
            }
        });



        osButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {

//                    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
//                    Calendar c = Calendar.getInstance();
//                    long when = c.getTimeInMillis();
//                    SystemClock.setCurrentTimeMillis(when);

                    if (ShellUtils.checkRootPermission()) {
                        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
                            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
                            String datetime=df.format(new Date());
                            Process process = Runtime.getRuntime().exec("su");
                            DataOutputStream os = new DataOutputStream(process.getOutputStream());
                            //os.writeBytes("setprop persist.sys.timezone GMT\n");
                            os.writeBytes("/system/bin/date -s " + datetime + "\n");
                            os.writeBytes("clock -w\n");
                            os.writeBytes("exit\n");
                            os.flush();
                    } else {
                        Toast.makeText(MainActivity.this, "no rootPermission", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                }
            }
        });



    }


    class OsThreads extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            "yyyy年MM月dd日   HH:mm:ss");
                    String str = sdf.format(new Date());
                    oshandler.sendMessage(oshandler.obtainMessage(100, str));
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    class NftThreads extends Thread {
        @Override
        public void run() {
            try {

                Date ntpCurrent = null;

                while (true) {
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            "yyyy年MM月dd日   HH:mm:ss");

                    SntpClient client = new SntpClient();
                    if (client.requestTime("0.jp.pool.ntp.org", 30000)) {
                        long now = client.getNtpTime() + System.nanoTime() / 1000
                                - client.getNtpTimeReference();
                        ntpCurrent = new Date(now);
                    }
                    String str = sdf.format(ntpCurrent);
                    nfthandler.sendMessage(nfthandler.obtainMessage(100, str));
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
