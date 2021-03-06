package com.example.mengqi.sportsdemo.Activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.example.mengqi.sportsdemo.Model.LatiLong;
import com.example.mengqi.sportsdemo.Utils.DrawUtils.CircleProgressBar;
import com.example.mengqi.sportsdemo.Utils.DrawUtils.DrawOnMap;
import com.example.mengqi.sportsdemo.Dao.DrawLineDaoImpl;
import com.example.mengqi.sportsdemo.R;
import com.example.mengqi.sportsdemo.Utils.DrawUtils.GaodeLocationUtil;
import com.example.mengqi.sportsdemo.Utils.DrawUtils.LongClickUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements LocationSource {
    private static final String TAG = "SportsDemo";

    private static final int LINE_WIDTH = 16;

    @BindView(R.id.run_hint)
    ImageView runHintButton;
    @BindView(R.id.run_location)
    ImageView runLocationButton;
    @BindView(R.id.run_destination)
    ImageView runDestinationButton;
    @BindView(R.id.run_hint_layout)
    LinearLayout runHintLayout;
    @BindView(R.id.run_hint_close)
    ImageView runHintCloseButton;
    @BindView(R.id.map_toolbar)
    LinearLayout mapToolBarLayout;
    @BindView(R.id.finish_layout)
    RelativeLayout finishLayout;
    @BindView(R.id.finish_layout_back)
    ImageView finishCloseButton;
    @BindView(R.id.finish_layout_pause)
    LinearLayout finishPauseButton;
    @BindView(R.id.finish_layout_continue)
    LinearLayout finishContinueButton;
    @BindView(R.id.finish_layout_finish)
    FrameLayout finishRunButton;
    @BindView(R.id.finish_progressBar)
    CircleProgressBar circleProgressBar;

    private Animation aplphaAnimationIn;
    private Animation aplphaAnimationBack;

    int mTotalProgress = 100;
    int mCurrentProgress = 0;
    boolean isClick = false;
    final Handler mHandler = new Handler();


    MapView mMapView = null;
    AMap aMap = null;

    // 权限初始化
    private final int SDK_PERMISSION_REQUEST = 127;
    private String permissionInfo;

    // 是否第一次加载地图
    private boolean firstIn = true;

    // 默认显示Zoom
    private static final float BASE_LOCATION_ZOOM = 18.0f;

    // 高德地图初始化
    private LocationSource.OnLocationChangedListener mListener = null;//定位监听器
    private GaodeLocationUtil locationUtil;

    // DrawLine数据库初始化
    private DrawLineDaoImpl drawLineFormDB;
    private DrawOnMap drawOnMap;

    //Arrow
    BitmapDescriptor arrowBitmap = null;
    //End,StartPoint
    BitmapDescriptor endBitmap = null;
    BitmapDescriptor startBitmap = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = (MapView) findViewById(R.id.gmapView);
        // 在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        aplphaAnimationIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.hint_layout_alpha_in);
        aplphaAnimationBack = AnimationUtils.loadAnimation(MainActivity.this, R.anim.hint_layout_alpha_back);

        arrowBitmap = BitmapDescriptorFactory.fromResource(R.drawable.arrow1);
        startBitmap = BitmapDescriptorFactory.fromResource(R.drawable.start);
        endBitmap = BitmapDescriptorFactory.fromResource(R.drawable.start);


        // 获取权限
        getPersimmions();


//        Button mDlBtn = findViewById(R.id.btn_drawline);


        if (aMap == null) {
            aMap = mMapView.getMap();
        }
//        MAP_TYPE_NORMAL：普通地图，值为1；
//        MAP_TYPE_SATELLITE：卫星地图，值为2；
//        MAP_TYPE_NIGHT 黑夜地图，夜间模式，值为3；
//        MAP_TYPE_NAVI 导航模式，值为4;
//        MAP_TYPE_BUS 公交模式，值为5。
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        // 获取经纬度数据库
        drawLineFormDB = new DrawLineDaoImpl(this);
        // drawLine历史路线功能
        drawOnMap = new DrawOnMap(this);

        runHintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (runHintLayout.getVisibility() == View.GONE) {
                    runHintLayout.setVisibility(View.VISIBLE);
                    runHintLayout.startAnimation(aplphaAnimationIn);
                    runHintLayout.setClickable(true);
                }
            }
        });

        runHintCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (runHintLayout.getVisibility() == View.VISIBLE) {
                    runHintLayout.setVisibility(View.GONE);
                    runHintLayout.startAnimation(aplphaAnimationBack);
                    runHintLayout.setClickable(false);
                }
            }
        });
        runDestinationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finishLayout.getVisibility() == View.GONE) {
                    finishLayout.setVisibility(View.VISIBLE);
                    finishLayout.startAnimation(aplphaAnimationIn);
                    finishLayout.setClickable(true);
                }
            }
        });
        finishCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finishLayout.getVisibility() == View.VISIBLE) {
                    finishLayout.setVisibility(View.GONE);
                    finishLayout.startAnimation(aplphaAnimationBack);
                    finishLayout.setClickable(false);
                }
            }
        });
        finishPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finishContinueButton.getVisibility() == View.INVISIBLE && finishRunButton.getVisibility() == View.INVISIBLE) {
                    finishContinueButton.setVisibility(View.VISIBLE);
                    finishRunButton.setVisibility(View.VISIBLE);
                    finishPauseButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        finishContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finishPauseButton.getVisibility() == View.INVISIBLE) {
                    finishPauseButton.setVisibility(View.VISIBLE);
                    finishRunButton.setVisibility(View.INVISIBLE);
                    finishContinueButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        finishRunButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isClick = true;
                        mCurrentProgress = 0;
                        startPlay();
                        return true;
                    case MotionEvent.ACTION_UP:
                        isClick = false;
                        break;
                }
                return false;
            }
        });


        runLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                init();
            }
        });

//
//        mDlBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                drawLine();
//            }
//        });

    }

    //结束时圆形按钮动画
    private void startPlay() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCurrentProgress < mTotalProgress + 1) {
                    if (isClick) {
                        mCurrentProgress += 1;
                        mHandler.postDelayed(this, 10);
                        circleProgressBar.setProgress(mCurrentProgress);
                    } else {
                        mCurrentProgress = 0;
                        circleProgressBar.setProgress(mCurrentProgress);
                    }
                }
                if (mCurrentProgress == mTotalProgress) {
                    deactivate();
                    Log.d(TAG, "onTouch: equal");
                }
            }
        });
    }

    private void init() {
        setLocationCallBack();
        //设置定位监听
        aMap.setLocationSource(this);
        //设置缩放级别
        aMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        //显示定位层并可触发，默认false
        aMap.setMyLocationEnabled(true);
    }

    private void setLocationCallBack() {
        locationUtil = new GaodeLocationUtil();
        locationUtil.setLocationCallBack(new GaodeLocationUtil.ILocationCallBack() {
            @Override
            public void callBack(double lat, double lgt, AMapLocation aMapLocation) {
                LatiLong latLng = new LatiLong(lat, lgt);
                //根据获取的经纬度，将地图移动到定位位置
                aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(lat, lgt)));
                mListener.onLocationChanged(aMapLocation);
                drawLineFormDB.insertLatlng(latLng);
            }
        });
    }

    //定位激活回调
    @Override
    public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        locationUtil.startLocate(getApplicationContext());

    }

    @Override
    public void deactivate() {
        mListener = null;
    }


    private void drawLine() {
        // 画出导航线路
        List<PolylineOptions> roadOverlay = drawOnMap.drawRoad(arrowBitmap, LINE_WIDTH);
        for (PolylineOptions options : roadOverlay) {
            aMap.addPolyline(options);
        }

        List<MarkerOptions> startAndEndMarker = drawOnMap.drawStartAndEnd(startBitmap, endBitmap);
        for (MarkerOptions options : startAndEndMarker) {
            aMap.addMarker(options);
        }
    }


    //获取权限
    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<>();
            // 定位权限为必须权限，用户如果禁止，则每次进入都会申请
            // 定位精确位置
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.INTERNET);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
            }
            if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
             */
            // 读写权限
            if (addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionInfo += "Manifest.permission.WRITE_EXTERNAL_STORAGE Deny \n";
            }
            // 读取电话状态权限
            if (addPermission(permissions, Manifest.permission.READ_PHONE_STATE)) {
                permissionInfo += "Manifest.permission.READ_PHONE_STATE Deny \n";
            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)) {  // 如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。
                return true;
            } else {
                permissionsList.add(permission);
                return false;
            }
        } else {
            return true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case SDK_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "请允许所有权限才能允许本软件", Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }


}

