package hdfg159.zyftp.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.swiftp.FsSettings;

import java.net.InetAddress;

import hdfg159.zyftp.FsService;
import hdfg159.zyftp.R;
import hdfg159.zyftp.ui.StatusBarCompat;
import hdfg159.zyftp.ui.UiUpdateUtil;
import hdfg159.zyftp.utils.ClipboardUtils;
import hdfg159.zyftp.utils.DialogUtils;
import hdfg159.zyftp.utils.SharedPreferencesUtils;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private WifiManager wifiManager;
    private TextView wifiinfo;
    private TextView ftpinfo;
    private TextView userconfig;
    private ImageView wifiimg;
    private SwitchCompat ftpswitch;
    public static Context mm;
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    removeMessages(0);
                    updateUI();
                    break;
                case 1:
                    removeMessages(1);
            }
        }
    };

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctx, Intent intent) {
            updateUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mm = this;

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawlayout);

        wifiinfo = (TextView) findViewById(R.id.wifiinfo);
        ftpinfo = (TextView) findViewById(R.id.ftpinfo);
        ftpinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardUtils.Copy(MainActivity.this, ftpinfo.getText().toString());
            }
        });


        userconfig = (TextView) findViewById(R.id.wifiuser);
        wifiimg = (ImageView) findViewById(R.id.wifiimg);
        ftpswitch = (SwitchCompat) findViewById(R.id.ftpswith);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        StatusBarCompat.compat(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        actionBarDrawerToggle.syncState();
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.exitapp:
                        if (FsService.isRunning()) {
                            stopServer();
                        }
                        finish();
                        break;
                    case R.id.aboutapp:
                        AlertDialog aboutalert = new AlertDialog.Builder(MainActivity.this).setTitle("关于")
                                .setMessage(R.string.aboutcontnt)
                                .setPositiveButton("确定", null).setNegativeButton(getString(R.string.update_content), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DialogUtils.showPrompt(MainActivity.this, getString(R.string.update_content), getString(R.string.newcontent), "确认");
                                    }
                                }).create();
                        aboutalert.show();
                        Linkify.addLinks((TextView) aboutalert.findViewById(android.R.id.message), Linkify.ALL);
                        break;
                    case R.id.nav_home:
                        break;
                    case R.id.settings:
                        final Intent intent = new Intent(MainActivity.this, Settings.class);
                        if (FsService.isRunning()) {
                            DialogUtils.showAlertlr(MainActivity.this, getString(R.string.tips), getString(R.string.ServeisRunning), "取消", null, getString(R.string.StopServe), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    stopServer();
                                    updateUI();
                                    ftpswitch.setChecked(false);
                                    startActivity(intent);
                                }
                            });
                        } else {
                            startActivity(intent);
                        }
                        break;
                }
                menuItem.setChecked(true);
                drawerLayout.closeDrawers();
                return true;
            }
        });

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        ftpswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startServer();
                } else {
                    stopServer();
                }
            }
        });

        wifiimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        });

        updateUI();
        UiUpdateUtil.registerClient(handler);

        viewupdate();
    }

    private void viewupdate() {
        if (SharedPreferencesUtils.getBoolean(MainActivity.this, "firstupdate", true)) {
            DialogUtils.showAlertlr(this, getString(R.string.update_content), getString(R.string.newcontent), "确认", null, "不再提示", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferencesUtils.putBoolean(MainActivity.this, "firstupdate", false);
                }
            });
        }
    }

    private void updateUI() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String wifiIdString = wifiInfo == null ? null : wifiInfo.getSSID();
        boolean isWifiReady = FsService.isConnectedUsingWifi();
        wifiinfo.setText(isWifiReady ? wifiIdString
                : getString(R.string.checknowifi));
        wifiimg.setImageResource(isWifiReady ? R.drawable.ic_wifiavaliable
                : R.drawable.ic_nowifi);
        ftpswitch.setEnabled(isWifiReady);
        boolean isRunning = FsService.isRunning();
        if (isRunning) {
            InetAddress address = FsService.getLocalInetAddress();
            if (address != null) {
                String name = SharedPreferencesUtils.getString(this, "username", "admin");
                String password = SharedPreferencesUtils.getString(this, "password", "admin");
                String dir = SharedPreferencesUtils.getString(this, "chrootDir", "/");
                address = FsService.getLocalInetAddress();
                ftpinfo.setText("ftp://" + address.getHostAddress() + ":" + FsSettings.getPortNumber());
                if (SharedPreferencesUtils.getBoolean(this, "allow_anonymous", false)) {
                    userconfig.setText("匿名模式" + "\n" + "\n" + "访问路径:" + dir);
                } else {
                    userconfig.setText("用户名:" + name + "\n" + "密码:" + password + "\n" + "访问路径:" + dir);
                }
            } else {
                stopServer();
                userconfig.setText("\n\n");
                ftpinfo.setText("");
            }
        }
        ftpinfo.setVisibility(isRunning ? View.VISIBLE : View.INVISIBLE);
        userconfig.setVisibility(isRunning ? View.VISIBLE : View.INVISIBLE);
    }

    private void startServer() {
        Intent serverService = new Intent(MainActivity.this, FsService.class);
        if (!FsService.isRunning()) {
            this.startService(serverService);
        }
    }

    private void stopServer() {
        Intent serverService = new Intent(MainActivity.this, FsService.class);
        this.stopService(serverService);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(MainActivity.this).setNegativeButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (FsService.isRunning()) {
                    stopServer();
                }
                finish();
            }
        }).setPositiveButton("取消", null).setTitle("退出").setMessage("确认退出吗").show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UiUpdateUtil.unregisterClient(handler);
        System.exit(0);
//      //Activity finish后执行完全退出程序
    }

    @Override
    protected void onStop() {
        super.onStop();
        UiUpdateUtil.unregisterClient(handler);
        System.gc();
    }

    @Override
    protected void onStart() {
        super.onStart();
        UiUpdateUtil.registerClient(handler);
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiUpdateUtil.registerClient(handler);
        updateUI();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UiUpdateUtil.unregisterClient(handler);
        unregisterReceiver(wifiReceiver);
        System.gc();
    }

}
