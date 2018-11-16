package com.example.mockgps;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiBoundSearchOption;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.example.service.HistoryDBHelper;
import com.example.service.MockGpsService;
import com.example.service.SearchDBHelper;
import com.example.service.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mapapi.overlayutil.PoiOverlay;

import static com.example.service.MockGpsService.RunCode;
import static com.example.service.MockGpsService.StopCode;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private final int SDK_PERMISSION_REQUEST = 127;
    private String permissionInfo;

    //位置欺骗相关
    //  latLngInfo  经度&纬度
    public static String latLngInfo = "104.06121778639009&30.544111926165282";
    private boolean isMockLocOpen = false;
    private MockGpsService mockGpsService;
    private MockServiceReceiver mockServiceReceiver = null;
    private boolean isServiceRun = false;
    private boolean isMockServStart = false;

    //sqlite相关
    //定位历史
    private HistoryDBHelper historyDBHelper;
    private SQLiteDatabase locHistoryDB;
    //搜索历史
    private SearchDBHelper searchDBHelper;
    private SQLiteDatabase searchHistoryDB;

    private boolean isSQLiteStart = false;


    //http
    private RequestQueue mRequestQueue;
    private boolean isNetworkConnected = true;

    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private MyLocationConfiguration.LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker;
    private SensorManager mSensorManager;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;
    private String mCurrentCity = "成都市";
    private String mCurrentAddr;
    /**
     * 当前地点击点
     */
    public static LatLng currentPt = new LatLng(30.547743718042415, 104.07018449827267);
    public static BitmapDescriptor bdA = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_gcoding);


    public MapView mMapView;
    public static BaiduMap mBaiduMap;

    // UI相关
    RadioGroup.OnCheckedChangeListener radioButtonListener;
    RadioGroup.OnCheckedChangeListener radioButtonListener2;
    //Button requestLocButton;
    boolean isFirstLoc = true; // 是否首次定位
    private MyLocationData locData;

    private RadioGroup grouploc;
    private RadioGroup groupmap;

    private FloatingActionButton fab;
    private FloatingActionButton fabStop;

    //位置搜索相关
    PoiSearch poiSearch;
    private SearchView searchView;
    private ListView searchlist;
    private ListView historySearchlist;
    private SimpleAdapter simAdapt;
    private LinearLayout mlinearLayout;
    private LinearLayout mHistorylinearLayout;
    private MenuItem searchItem;
    private boolean isSubmit;
    private SuggestionSearch mSuggestionSearch;
    ////////

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        Log.d("PROGRESS", "isMockServStart=" + isMockServStart);
        Log.d("PROGRESS", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //sqlite
        try {
            historyDBHelper = new HistoryDBHelper(getApplicationContext());
            locHistoryDB = historyDBHelper.getWritableDatabase();
            searchDBHelper = new SearchDBHelper(getApplicationContext());
            searchHistoryDB = searchDBHelper.getWritableDatabase();
            isSQLiteStart = true;
//            historyDBHelper.onUpgrade(locHistoryDB,locHistoryDB.getVersion(),locHistoryDB.getVersion());
        } catch (Exception e) {
            Log.e("DATABASE", "sqlite init error");
            isSQLiteStart = false;
            e.printStackTrace();
        }

        //set fab listener
        setFabListener();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //http init
        mRequestQueue = Volley.newRequestQueue(this);

        //注册MockService广播接收器
        try {
            mockServiceReceiver = new MockServiceReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.example.service.MockGpsService");
//            this.unregisterReceiver(mockServiceReceiver);
            this.registerReceiver(mockServiceReceiver, filter);
        } catch (Exception e) {
            Log.e("UNKNOWN", "registerReceiver error");
            e.printStackTrace();
        }


        //获取权限
        getPersimmions();
        /////
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//获取传感器管理服务
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
        //RadioGroup
        setGroupListener();

        //网络是否可用
        if (!isNetworkAvailable()) {
            DisplayToast("网络连接不可用,请检查网络连接设置");
            isNetworkConnected = false;
        }


        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        initListener();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        //启用下面这个参数好像没什么卵用
//        option.setEnableSimulateGps(false);
        mLocClient.setLocOption(option);
        mLocClient.start();

        //poi search 实例化
        poiSearch = PoiSearch.newInstance();
        //搜索相关
        searchView = (SearchView) findViewById(R.id.action_search);
        searchlist = (ListView) findViewById(R.id.search_list_view);
        mlinearLayout = (LinearLayout) findViewById(R.id.search_linear);

        historySearchlist = (ListView) findViewById(R.id.search_history_list_view);
        mHistorylinearLayout = (LinearLayout) findViewById(R.id.search_history_linear);

        // 是否开启位置模拟
        isMockLocOpen = isAllowMockLocation();
        //提醒用户开启位置模拟
        if (!isMockLocOpen) {
            setDialog();
        }
        //悬浮窗权限判断
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(getApplicationContext())) {
                //启动Activity让用户授权
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
        //初始化POI搜索监听
        initPoiSearchResultListener();
        //搜索结果列表的点击监听
        setSearchRetClickListener();
        //搜索历史列表的点击监听
        setHistorySearchClickListener();
        //设置搜索建议返回值监听
        setSugSearchListener();
        //初始位置随机处理
        randomFix();
        //如果网络不可用，地图中心点置为最新定位点
        LatLng latLng = getLatestLocation(locHistoryDB, HistoryDBHelper.TABLE_NAME);
        MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.setMapStatus(mapstatusupdate);


        func();

    }

    //for debug
    public void func() {


    }

    //获取查询历史
    private List<Map<String, Object>> getSearchHistory() {
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        try {
            Cursor cursor = searchDBHelper.getWritableDatabase().query(SearchDBHelper.TABLE_NAME, null,
                    "ID > ?", new String[]{"0"},
                    null, null, "TimeStamp DESC", null);
            while (cursor.moveToNext()) {
                int ID = cursor.getInt(0);

                Map<String, Object> searchHistoryItem = new HashMap<String, Object>();
                searchHistoryItem.put("search_key", cursor.getString(1));
                searchHistoryItem.put("search_description", cursor.getString(2));
                searchHistoryItem.put("search_timestamp", "" + cursor.getInt(3));
                searchHistoryItem.put("search_isLoc", "" + cursor.getInt(4));
                searchHistoryItem.put("search_longitude", "" + cursor.getString(7));
                searchHistoryItem.put("search_latitude", "" + cursor.getString(8));

                data.add(searchHistoryItem);

            }
            // 关闭光标
            cursor.close();
        } catch (Exception e) {
            Log.e("DATABASE", "query error");
            e.printStackTrace();
        }
        return data;
    }

    //WIFI是否可用
    private boolean isWifiConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWiFiNetworkInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWiFiNetworkInfo != null) {
            return mWiFiNetworkInfo.isAvailable();
        }
        return false;
    }

    //MOBILE网络是否可用
    private boolean isMobileConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mMobileNetworkInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mMobileNetworkInfo != null) {
            return mMobileNetworkInfo.isAvailable();
        }
        return false;
    }

    //网络是否可用
    private boolean isNetworkAvailable() {
        return isWifiConnected() || isMobileConnected();
    }

    //位置随机处理
    private void randomFix() {
        double ra1 = Math.random() * 2.0 - 1.0;
        double ra2 = Math.random() * 2.0 - 1.0;
        double randLng = 104.07018449827267 + ra1 / 2000.0;
        double randLat = 30.547743718042415 + ra2 / 2000.0;
        currentPt = new LatLng(randLat, randLng);
        transformCoordinate(Double.toString(randLng), Double.toString(randLat));
    }

    //set group button listener
    private void setGroupListener() {
        grouploc = (RadioGroup) this.findViewById(R.id.RadioGroupLocType);
        radioButtonListener2 = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.normalloc) {
//                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                    mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                            mCurrentMode, true, mCurrentMarker));
                    MapStatus.Builder builder1 = new MapStatus.Builder();
                    builder1.overlook(0);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder1.build()));
                }
                if (checkedId == R.id.trackloc) {
//                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                    mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                            mCurrentMode, true, mCurrentMarker));
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.overlook(0);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                }
                if (checkedId == R.id.compassloc) {
//                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                    mCurrentMode = MyLocationConfiguration.LocationMode.COMPASS;
                    mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                            mCurrentMode, true, mCurrentMarker));
                }
            }
        };
        grouploc.setOnCheckedChangeListener(radioButtonListener2);


        groupmap = (RadioGroup) this.findViewById(R.id.RadioGroup);
        radioButtonListener = new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.normal) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                }
                if (checkedId == R.id.statellite) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                }
            }
        };
        groupmap.setOnCheckedChangeListener(radioButtonListener);
    }

    //set float action button listener
    private void setFabListener() {
        //应用内悬浮按钮
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabStop = (FloatingActionButton) findViewById(R.id.fabStop);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /////
                if (!(isMockLocOpen = isAllowMockLocation())) {
                    setDialog();
                } else {
                    if (!isMockServStart && !isServiceRun) {
                        Log.d("DEBUG", "current pt is " + currentPt.longitude + "  " + currentPt.latitude);
                        updateMapState();
                        //start mock location service
                        Intent mockLocServiceIntent = new Intent(MainActivity.this, MockGpsService.class);
                        mockLocServiceIntent.putExtra("key", latLngInfo);
                        //isFisrtUpdate=false;
                        //save record
                        updatePositionInfo();
                        //insert end
                        if (Build.VERSION.SDK_INT >= 26) {
                            startForegroundService(mockLocServiceIntent);
                            Log.d("DEBUG", "startForegroundService");
                        } else {
                            startService(mockLocServiceIntent);
                            Log.d("DEBUG", "startService");
                        }
                        isMockServStart = true;
                        Snackbar.make(view, "位置模拟已开启", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        fab.setVisibility(View.INVISIBLE);
                        fabStop.setVisibility(View.VISIBLE);
                        //track
                        grouploc.check(R.id.trackloc);
                    } else {
                        Snackbar.make(view, "位置模拟已在运行", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        fab.setVisibility(View.INVISIBLE);
                        fabStop.setVisibility(View.VISIBLE);
                        isMockServStart = true;
                    }
                }
            }
        });

        fabStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMockServStart) {
                    //end mock location
                    Intent mockLocServiceIntent = new Intent(MainActivity.this, MockGpsService.class);
                    stopService(mockLocServiceIntent);
                    Snackbar.make(v, "位置模拟服务终止", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    //service finish
                    isMockServStart = false;
                    fab.setVisibility(View.VISIBLE);
                    fabStop.setVisibility(View.INVISIBLE);
                    //重新定位
                    mLocClient.stop();
                    mLocClient.start();
                    //normal
                    grouploc.check(R.id.normalloc);
                    //clear
//                    mBaiduMap.clear();
                }
            }
        });
    }

    //设置search list 点击监听
    private void setSearchRetClickListener() {
        searchlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
                String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
//                DisplayToast("lng is "+lng+"lat is "+lat);
                currentPt = new LatLng(Double.valueOf(lat), Double.valueOf(lng));
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(currentPt);
                //对地图的中心点进行更新，
                mBaiduMap.setMapStatus(mapstatusupdate);
                updateMapState();
                transformCoordinate(lng, lat);
//                searchlist.setVisibility(View.GONE);

                //搜索历史 插表参数
                ContentValues contentValues = new ContentValues();
                contentValues.put("SearchKey", ((TextView) view.findViewById(R.id.poi_name)).getText().toString());
                contentValues.put("Description", ((TextView) view.findViewById(R.id.poi_addr)).getText().toString());
                contentValues.put("IsLocate", 1);
                contentValues.put("BD09Longitude", lng);
                contentValues.put("BD09Latitude", lat);
                String wgsLatLngStr[] = latLngInfo.split("&");
                contentValues.put("WGS84Longitude", wgsLatLngStr[0]);
                contentValues.put("WGS84Latitude", wgsLatLngStr[1]);
                contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);

                if (!insertHistorySearchTable(searchHistoryDB, SearchDBHelper.TABLE_NAME, contentValues)) {
                    Log.e("DATABASE", "insertHistorySearchTable[SearchHistory] error");
                } else {
                    Log.d("DATABASE", "insertHistorySearchTable[SearchHistory] success");
                }

                mlinearLayout.setVisibility(View.INVISIBLE);
                searchItem.collapseActionView();
//                transformCoordinate();
            }
        });
    }


    //设置history search list 点击监听
    private void setHistorySearchClickListener() {
        historySearchlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String searchDescription = ((TextView) view.findViewById(R.id.search_description)).getText().toString();
                String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
                String searchIsLoc = ((TextView) view.findViewById(R.id.search_isLoc)).getText().toString();

                //如果是定位搜索
                if (searchIsLoc.equals("1")) {
                    String lng = ((TextView) view.findViewById(R.id.search_longitude)).getText().toString();
                    String lat = ((TextView) view.findViewById(R.id.search_latitude)).getText().toString();
//                    DisplayToast("lng is " + lng + "lat is " + lat);
                    currentPt = new LatLng(Double.valueOf(lat), Double.valueOf(lng));
                    MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(currentPt);
                    //对地图的中心点进行更新，
                    mBaiduMap.setMapStatus(mapstatusupdate);
                    updateMapState();
                    transformCoordinate(lng, lat);
                    //设置列表不可见
                    mHistorylinearLayout.setVisibility(View.INVISIBLE);
                    searchItem.collapseActionView();
                    //更新表
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("SearchKey", searchKey);
                    contentValues.put("Description", searchDescription);
                    contentValues.put("IsLocate", 1);
                    contentValues.put("BD09Longitude", lng);
                    contentValues.put("BD09Latitude", lat);
                    String wgsLatLngStr[] = latLngInfo.split("&");
                    contentValues.put("WGS84Longitude", wgsLatLngStr[0]);
                    contentValues.put("WGS84Latitude", wgsLatLngStr[1]);
                    contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                    if (!insertHistorySearchTable(searchHistoryDB, SearchDBHelper.TABLE_NAME, contentValues)) {
                        Log.e("DATABASE", "insertHistorySearchTable[SearchHistory] error");
                    } else {
                        Log.d("DATABASE", "insertHistorySearchTable[SearchHistory] success");
                    }
                }
                //如果仅仅是搜索
                else if (searchIsLoc.equals("0")) {
                    try {
//                        resetMap();
                        isSubmit = true;
                        mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())

                                .keyword(searchKey)
                                .city(mCurrentCity)

                        );
                        mBaiduMap.clear();
                        mHistorylinearLayout.setVisibility(View.INVISIBLE);
                        searchItem.collapseActionView();

                        //更新表
                        //搜索历史 插表参数
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("SearchKey", searchKey);
                        contentValues.put("Description", "搜索...");
                        contentValues.put("IsLocate", 0);
                        contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                        if (!insertHistorySearchTable(searchHistoryDB, SearchDBHelper.TABLE_NAME, contentValues)) {
                            Log.e("DATABASE", "insertHistorySearchTable[SearchHistory] error");
                        } else {
                            Log.d("DATABASE", "insertHistorySearchTable[SearchHistory] success");
                        }

                    } catch (Exception e) {
                        DisplayToast("搜索失败，请检查网络连接");
                        e.printStackTrace();
                    }
                }
                //其他情况
                else {

                }


            }
        });
        historySearchlist.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Warning")//这里是表头的内容
                        .setMessage("确定要删除该项搜索记录吗?")//这里是中间显示的具体信息
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
                                        try {
                                            searchHistoryDB.delete(SearchDBHelper.TABLE_NAME, "SearchKey = ?", new String[]{searchKey});
                                            //删除成功
                                            //展示搜索历史
                                            List<Map<String, Object>> data = getSearchHistory();
                                            if (data.size() > 0) {
                                                simAdapt = new SimpleAdapter(
                                                        MainActivity.this,
                                                        data,
                                                        R.layout.history_search_item,
                                                        new String[]{"search_key", "search_description", "search_timestamp", "search_isLoc", "search_longitude", "search_latitude"},// 与下面数组元素要一一对应
                                                        new int[]{R.id.search_key, R.id.search_description, R.id.search_timestamp, R.id.search_isLoc, R.id.search_longitude, R.id.search_latitude});
                                                historySearchlist.setAdapter(simAdapt);
                                                mHistorylinearLayout.setVisibility(View.VISIBLE);
                                            }

                                        } catch (Exception e) {
                                            Log.e("DATABASE", "delete error");
                                            DisplayToast("DELETE ERROR[UNKNOWN]");
                                            e.printStackTrace();
                                        }
//                                        String locID=(String) ((TextView) view.findViewById(R.id.LocationID)).getText();
//                                        boolean deleteRet=deleteRecord(sqLiteDatabase,HistoryDBHelper.TABLE_NAME,Integer.valueOf(locID));
//                                        if (deleteRet){
//                                            DisplayToast("删除成功!");
//                                            initListView();
//                                        }
                                    }
                                })
                        .setNegativeButton("取消",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .show();

                return true;
            }
        });
    }


    //poi搜索初始化
    private void initPoiSearchResultListener() {
        OnGetPoiSearchResultListener poiSearchListener = new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult poiResult) {
                if (poiResult == null || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {// 没有找到检索结果
                    DisplayToast("没有找到检索结果");
                    return;
                }
                if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {// 检索结果正常返回

                    if (isSubmit) {
//                        mBaiduMap.clear();
                        MyPoiOverlay poiOverlay = new MyPoiOverlay(mBaiduMap);
                        poiOverlay.setData(poiResult);// 设置POI数据
                        mBaiduMap.setOnMarkerClickListener(poiOverlay);
                        poiOverlay.addToMap();// 将所有的overlay添加到地图上
                        poiOverlay.zoomToSpan();
                        mlinearLayout.setVisibility(View.INVISIBLE);
                        //标注搜索点 关闭搜索列表
//                        searchView.clearFocus();  //可以收起键盘
                        searchItem.collapseActionView(); //关闭搜索视图
                        isSubmit = false;

                    } else {
                        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
                        int retCnt = poiResult.getAllPoi().size();
                        for (int i = 0; i < retCnt; i++) {
                            Map<String, Object> testitem = new HashMap<String, Object>();
                            testitem.put("key_name", poiResult.getAllPoi().get(i).name);
                            testitem.put("key_addr", poiResult.getAllPoi().get(i).address);
                            testitem.put("key_lng", "" + poiResult.getAllPoi().get(i).location.longitude);
                            testitem.put("key_lat", "" + poiResult.getAllPoi().get(i).location.latitude);
                            data.add(testitem);
                        }
                        simAdapt = new SimpleAdapter(
                                MainActivity.this,
                                data,
                                R.layout.poi_search_item,
                                new String[]{"key_name", "key_addr", "key_lng", "key_lat"},// 与下面数组元素要一一对应
                                new int[]{R.id.poi_name, R.id.poi_addr, R.id.poi_longitude, R.id.poi_latitude});
                        searchlist.setAdapter(simAdapt);
//                    searchlist.setVisibility(View.VISIBLE);
                        mlinearLayout.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
                DisplayToast(poiDetailResult.name);
//                Log.d("DETAIL",poiDetailResult.address);
//                Log.d("DETAIL",poiDetailResult.name);
//                Log.d("DETAIL",poiDetailResult.tag);
            }

            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

            }

        };
        poiSearch.setOnGetPoiSearchResultListener(poiSearchListener);
    }

    //检索建议
    private void setSugSearchListener() {
        mSuggestionSearch = SuggestionSearch.newInstance();
        OnGetSuggestionResultListener listener = new OnGetSuggestionResultListener() {
            public void onGetSuggestionResult(SuggestionResult res) {

                if (res == null || res.getAllSuggestions() == null) {
                    //未找到相关结果
                    DisplayToast("没有找到检索结果");
                    return;
                }
                //获取在线建议检索结果
                else {
                    if (isSubmit) {
//                        mBaiduMap.clear();
                        //normal
                        grouploc.check(R.id.normalloc);
                        MyPoiOverlay poiOverlay = new MyPoiOverlay(mBaiduMap);
                        poiOverlay.setSugData(res);// 设置POI数据
                        mBaiduMap.setOnMarkerClickListener(poiOverlay);
                        poiOverlay.addToMap();// 将所有的overlay添加到地图上
                        poiOverlay.zoomToSpan();
                        mlinearLayout.setVisibility(View.INVISIBLE);
                        //标注搜索点 关闭搜索列表
//                        searchView.clearFocus();  //可以收起键盘
                        searchItem.collapseActionView(); //关闭搜索视图
                        isSubmit = false;

                    } else {
                        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
                        int retCnt = res.getAllSuggestions().size();
                        for (int i = 0; i < retCnt; i++) {
                            if (res.getAllSuggestions().get(i).pt == null) {
                                continue;
                            }
                            Map<String, Object> poiItem = new HashMap<String, Object>();
                            poiItem.put("key_name", res.getAllSuggestions().get(i).key);
                            poiItem.put("key_addr", res.getAllSuggestions().get(i).city + " " + res.getAllSuggestions().get(i).district);
                            poiItem.put("key_lng", "" + res.getAllSuggestions().get(i).pt.longitude);
                            poiItem.put("key_lat", "" + res.getAllSuggestions().get(i).pt.latitude);
                            data.add(poiItem);
                        }
                        simAdapt = new SimpleAdapter(
                                MainActivity.this,
                                data,
                                R.layout.poi_search_item,
                                new String[]{"key_name", "key_addr", "key_lng", "key_lat"},// 与下面数组元素要一一对应
                                new int[]{R.id.poi_name, R.id.poi_addr, R.id.poi_longitude, R.id.poi_latitude});
                        searchlist.setAdapter(simAdapt);
//                    searchlist.setVisibility(View.VISIBLE);
                        mlinearLayout.setVisibility(View.VISIBLE);
                    }
                }
            }
        };
        mSuggestionSearch.setOnGetSuggestionResultListener(listener);

    }

    //城市内搜索
    private void citySearch(int page, String city, String key) {
        // 设置检索参数
        PoiCitySearchOption citySearchOption = new PoiCitySearchOption();
        citySearchOption.city(city);// 城市
        citySearchOption.keyword(key);// 关键字
        citySearchOption.pageCapacity(15);// 默认每页10条
        citySearchOption.pageNum(page);// 分页编号
        // 发起检索请求
        poiSearch.searchInCity(citySearchOption);
    }


    //范围检索
    private void boundSearch(int page, double longitude, double latitude, String key) {
        PoiBoundSearchOption boundSearchOption = new PoiBoundSearchOption();
        LatLng southwest = new LatLng(latitude - 0.01, longitude - 0.012);// 西南
        LatLng northeast = new LatLng(latitude + 0.01, longitude + 0.012);// 东北
        LatLngBounds bounds = new LatLngBounds.Builder().include(southwest)
                .include(northeast).build();// 得到一个地理范围对象
        boundSearchOption.bound(bounds);// 设置poi检索范围
        boundSearchOption.keyword(key);// 检索关键字
        boundSearchOption.pageNum(page);
        poiSearch.searchInBound(boundSearchOption);// 发起poi范围检索请求
    }


    //附近检索
    private void nearbySearch(int page, double longitude, double latitude, String key) {
        PoiNearbySearchOption nearbySearchOption = new PoiNearbySearchOption();
        nearbySearchOption.location(new LatLng(latitude, longitude));
        nearbySearchOption.keyword(key);
        nearbySearchOption.radius(1000);// 检索半径，单位是米
        nearbySearchOption.pageNum(page);
        poiSearch.searchNearby(nearbySearchOption);// 发起附近检索请求
    }

    //sqlite 操作 插表HistoryLocation
    private boolean insertHistoryLocTable(SQLiteDatabase sqLiteDatabase, String tableName, ContentValues contentValues) {
        boolean insertRet = true;
        try {
            sqLiteDatabase.insert(tableName, null, contentValues);
        } catch (Exception e) {
            Log.e("DATABASE", "insert error");
            insertRet = false;
            e.printStackTrace();
        }
        return insertRet;
    }

    //sqlite 操作 插表HistoryLocation
    private boolean insertHistorySearchTable(SQLiteDatabase sqLiteDatabase, String tableName, ContentValues contentValues) {
        boolean insertRet = true;
        try {

            String searchKey = contentValues.get("SearchKey").toString();
            sqLiteDatabase.delete(tableName, "SearchKey = ?", new String[]{searchKey});
            sqLiteDatabase.insert(tableName, null, contentValues);
        } catch (Exception e) {
            Log.e("DATABASE", "insert error");
            insertRet = false;
            e.printStackTrace();
        }
        return insertRet;
    }


    //sqlite 获取上一次定位位置
    private LatLng getLatestLocation(SQLiteDatabase sqLiteDatabase, String tableName) {
        try {
            Cursor cursor = sqLiteDatabase.query(tableName, null,
                    "ID > ?", new String[]{"0"},
                    null, null, "TimeStamp DESC", "1");
            if (cursor.getCount() == 0) {
                randomFix();
                return MainActivity.currentPt;
            } else {
                cursor.moveToNext();
                String BD09Longitude = cursor.getString(5);
                String BD09Latitude = cursor.getString(6);
                return new LatLng(Double.valueOf(BD09Latitude), Double.valueOf(BD09Longitude));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return MainActivity.currentPt;
    }

    //提醒开启位置模拟的弹框
    private void setDialog() {
        //判断是否开启开发者选项
//        boolean enableAdb = (Settings.Secure.getInt(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) > 0);
//        if (!enableAdb) {
//            DisplayToast("请打先开开发者选项");
//            return;
//        }



        new AlertDialog.Builder(this)
                .setTitle("启用位置模拟")//这里是表头的内容
                .setMessage("请在开发者选项->选择模拟位置信息应用中进行设置")//这里是中间显示的具体信息
                .setPositiveButton("设置",//这个string是设置左边按钮的文字
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                    startActivity(intent);
                                }catch (Exception e){
                                    DisplayToast("无法跳转到开发者选项,请先确保您的设备已处于开发者模式");
                                    e.printStackTrace();
                                }
                            }
                        })//setPositiveButton里面的onClick执行的是左边按钮
                .setNegativeButton("取消",//这个string是设置右边按钮的文字
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })//setNegativeButton里面的onClick执行的是右边的按钮的操作
                .show();
    }

    //模拟位置权限是否开启
    public boolean isAllowMockLocation() {
        boolean canMockPosition = false;
        if (Build.VERSION.SDK_INT <= 22) {//6.0以下
            canMockPosition = Settings.Secure.getInt(this.getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0;
        } else {
            try {
                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);//获得LocationManager引用
                String providerStr = LocationManager.GPS_PROVIDER;
                LocationProvider provider = locationManager.getProvider(providerStr);
                if (provider != null) {
                    locationManager.addTestProvider(
                            provider.getName()
                            , provider.requiresNetwork()
                            , provider.requiresSatellite()
                            , provider.requiresCell()
                            , provider.hasMonetaryCost()
                            , provider.supportsAltitude()
                            , provider.supportsSpeed()
                            , provider.supportsBearing()
                            , provider.getPowerRequirement()
                            , provider.getAccuracy());
                } else {
                    locationManager.addTestProvider(
                            providerStr
                            , true, true, false, false, true, true, true
                            , Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
                }
                locationManager.setTestProviderEnabled(providerStr, true);
                locationManager.setTestProviderStatus(providerStr, LocationProvider.AVAILABLE, null, System.currentTimeMillis());
                // 模拟位置可用
                canMockPosition = true;
                locationManager.setTestProviderEnabled(providerStr, false);
                locationManager.removeTestProvider(providerStr);
            } catch (SecurityException e) {
                canMockPosition = false;
            }
        }
        return canMockPosition;
    }

    //设置是否显示交通图
    public void setTraffic(View view) {
        mBaiduMap.setTrafficEnabled(((CheckBox) view).isChecked());
    }

    //设置是否显示百度热力图
    public void setBaiduHeatMap(View view) {
        mBaiduMap.setBaiduHeatMapEnabled(((CheckBox) view).isChecked());
    }

    //对地图事件的消息响应
    private void initListener() {
        mBaiduMap.setOnMapTouchListener(new BaiduMap.OnMapTouchListener() {

            @Override
            public void onTouch(MotionEvent event) {

            }
        });


        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            /**
             * 单击地图
             */
            public void onMapClick(LatLng point) {
                currentPt = point;
//                DisplayToast("BD09\n[纬度:" + point.latitude + "]\n[经度:" + point.longitude + "]");
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
                updateMapState();

            }

            /**
             * 单击地图中的POI点
             */
            public boolean onMapPoiClick(MapPoi poi) {
                currentPt = poi.getPosition();
//                DisplayToast("BD09\n[维度:" + poi.getPosition().latitude + "]\n[经度:" + poi.getPosition().longitude + "]");
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(poi.getPosition().longitude), String.valueOf(poi.getPosition().latitude));
                updateMapState();
                return false;
            }
        });
        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            /**
             * 长按地图
             */
            public void onMapLongClick(LatLng point) {
                currentPt = point;
//                DisplayToast("BD09\n[维度:" + point.latitude + "]\n[经度:" + point.longitude + "]");
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
                updateMapState();
            }
        });
        mBaiduMap.setOnMapDoubleClickListener(new BaiduMap.OnMapDoubleClickListener() {
            /**
             * 双击地图
             */
            public void onMapDoubleClick(LatLng point) {
                currentPt = point;
//                DisplayToast("BD09\n[维度:" + point.latitude + "]\n[经度:" + point.longitude + "]");
                //百度坐标系转wgs坐标系
                transformCoordinate(String.valueOf(point.longitude), String.valueOf(point.latitude));
                updateMapState();
            }
        });

        /**
         * 地图状态发生变化
         */
        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            public void onMapStatusChangeStart(MapStatus status) {
//                updateMapState();
            }

            @Override
            public void onMapStatusChangeStart(MapStatus status, int reason) {

            }

            public void onMapStatusChangeFinish(MapStatus status) {
//                updateMapState();
            }

            public void onMapStatusChange(MapStatus status) {
//                updateMapState();
            }
        });
    }

    //更新地图状态显示面板
    private void updateMapState() {
        Log.d("DEBUG", "updateMapState");
        if (currentPt != null) {
            MarkerOptions ooA = new MarkerOptions().position(currentPt).icon(bdA);
            mBaiduMap.clear();
            mBaiduMap.addOverlay(ooA);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(mCurrentLat)
                    .longitude(mCurrentLon).build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //定位SDK监听函数
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            mCurrentAddr = location.getAddrStr();
            mCurrentCity = location.getCity();
            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    @Override
    protected void onPause() {
        Log.d("PROGRESS", "onPause");
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d("PROGRESS", "onPause");
        mMapView.onResume();
        super.onResume();
        //为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onStop() {
        Log.d("PROGRESS", "onStop");
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("PROGRESS", "onDestroy");
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        //poi search destroy
        poiSearch.destroy();
        mSuggestionSearch.destroy();
        //close db
        locHistoryDB.close();
        searchHistoryDB.close();
        super.onDestroy();
    }

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }


    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            //悬浮窗
//            if (checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
//            }
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
            if (shouldShowRequestPermissionRationale(permission)) {
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        //找到searchView
        searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setIconified(false);//设置searchView处于展开状态
        searchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        searchView.setIconifiedByDefault(true);//默认为true在框内，设置false则在框外
        searchView.setSubmitButtonEnabled(true);//显示提交按钮

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // Do something when collapsed
                menu.setGroupVisible(0, true);
//                searchlist.setVisibility(View.GONE);
                mlinearLayout.setVisibility(View.INVISIBLE);
                mHistorylinearLayout.setVisibility(View.INVISIBLE);
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // Do something when expanded
                menu.setGroupVisible(0, false);
                mlinearLayout.setVisibility(View.INVISIBLE);
                //展示搜索历史
                List<Map<String, Object>> data = getSearchHistory();
                if (data.size() > 0) {
                    simAdapt = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            R.layout.history_search_item,
                            new String[]{"search_key", "search_description", "search_timestamp", "search_isLoc", "search_longitude", "search_latitude"},// 与下面数组元素要一一对应
                            new int[]{R.id.search_key, R.id.search_description, R.id.search_timestamp, R.id.search_isLoc, R.id.search_longitude, R.id.search_latitude});
                    historySearchlist.setAdapter(simAdapt);
                    mHistorylinearLayout.setVisibility(View.VISIBLE);
                }


                return true;  // Return true to expand action view
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //提交按钮的点击事件
                //do search
                try {
                    isSubmit = true;
//                    poiSearch.searchInCity((new PoiCitySearchOption())
//                            .city(mCurrentCity)
//                            .keyword(query)
//                            .pageCapacity(10)
//                            .pageNum(0));
                    mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())

                            .keyword(query)
                            .city(mCurrentCity)

                    );
                    //搜索历史 插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("SearchKey", query);
                    contentValues.put("Description", "搜索...");
                    contentValues.put("IsLocate", 0);
                    contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                    if (!insertHistorySearchTable(searchHistoryDB, SearchDBHelper.TABLE_NAME, contentValues)) {
                        Log.e("DATABASE", "insertHistorySearchTable[SearchHistory] error");
                    } else {
                        Log.d("DATABASE", "insertHistorySearchTable[SearchHistory] success");
                    }

                    mBaiduMap.clear();
                    mlinearLayout.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    DisplayToast("搜索失败，请检查网络连接");
                    e.printStackTrace();
                }
                //
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //当输入框内容改变的时候回调

                //搜索历史置为不可见
                mHistorylinearLayout.setVisibility(View.INVISIBLE);

                if (!newText.equals("")) {
                    //do search
                    //WATCH ME
                    try {
                        mSuggestionSearch.requestSuggestion((new SuggestionSearchOption())
                                .keyword(newText)
                                .city(mCurrentCity)
                        );
//                        poiSearch.searchInCity((new PoiCitySearchOption())
//                                .city(mCurrentCity)
//                                .keyword(newText)
//                                .pageCapacity(30)
//                                .pageNum(0));
                    } catch (Exception e) {
                        DisplayToast("搜索失败，请检查网络连接");
                        e.printStackTrace();
                    }
                    //
                }
                return true;
            }
        });

        return true;
    }

    //重置地图
    private void resetMap() {
        mBaiduMap.clear();
        MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(new LatLng(mCurrentLat, mCurrentLon));
        //对地图的中心点进行更新
        mBaiduMap.setMapStatus(mapstatusupdate);
        //更新当前位置
        currentPt = new LatLng(mCurrentLat, mCurrentLon);
        transformCoordinate(Double.toString(currentPt.longitude), Double.toString(currentPt.latitude));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_setting) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                startActivity(intent);
            }catch (Exception e){
                DisplayToast("无法跳转到开发者选项,请先确保您的设备已处于开发者模式");
                e.printStackTrace();
            }
            //判断是否开启开发者选项
//            boolean enableAdb = (Settings.Secure.getInt(getContentResolver(), Settings.Secure.ADB_ENABLED, 0) > 0);
//            if (!enableAdb) {
//                DisplayToast("请打先开开发者选项");
//            } else {
//                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
//                startActivity(intent);
//            }
            return true;
        } else if (id == R.id.action_resetMap) {
            resetMap();
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_map) {
            // Handle
        } else if (id == R.id.nav_history) {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_localmap) {
            Intent intent = new Intent(MainActivity.this, OfflineMapActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_manage) {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
        }
//        else if (id == R.id.nav_share) {
//
//        }
        else if (id == R.id.nav_send) {
            Intent i = new Intent(Intent.ACTION_SEND);
            // i.setType("text/plain"); //模拟器请使用这行
            i.setType("message/rfc822"); // 真机上使用这行
            i.putExtra(Intent.EXTRA_EMAIL,
                    new String[]{"hilavergil@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "SUGGESTION");
//            i.putExtra(Intent.EXTRA_TEXT, "非常感谢您的宝贵意见，我会努力做得更好。");
            startActivity(Intent.createChooser(i,
                    "Select email application."));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //坐标转换
    private void transformCoordinate(final String longitude, final String latitude) {
        //参数坐标系：bd09
//        boolean isInCHN=false;
        final double error = 0.00000001;
        final String mcode = "52:67:11:2E:F4:56:92:4F:3D:C9:A7:DD:CA:80:D8:29:C1:E1:0A:96;com.example.mockgps";
        final String ak = "8geZBnkT8aQ9zwWownqr9ZSesAfxnGbM";
        //判断bd09坐标是否在国内
        String mapApiUrl = "http://api.map.baidu.com/geoconv/v1/?coords=" + longitude + "," + latitude +
                "&from=5&to=3&ak=" + ak + "&mcode=" + mcode;
        //bd09坐标转gcj02
        StringRequest stringRequest = new StringRequest(mapApiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject getRetJson = new JSONObject(response);
                            //如果api接口转换成功
                            if (Integer.valueOf(getRetJson.getString("status")) == 0) {
                                Log.d("HTTP", "call api success");
                                JSONArray coordinateArr = getRetJson.getJSONArray("result");
                                JSONObject coordinate = coordinateArr.getJSONObject(0);
                                String gcj02Longitude = coordinate.getString("x");
                                String gcj02Latitude = coordinate.getString("y");

                                Log.d("DEBUG", "bd09Longitude is " + longitude);
                                Log.d("DEBUG", "bd09Latitude is " + latitude);

                                Log.d("DEBUG", "gcj02Longitude is " + gcj02Longitude);
                                Log.d("DEBUG", "gcj02Latitude is " + gcj02Latitude);

                                BigDecimal bigDecimalGcj02Longitude = new BigDecimal(Double.valueOf(gcj02Longitude));
                                BigDecimal bigDecimalGcj02Latitude = new BigDecimal(Double.valueOf(gcj02Latitude));

                                BigDecimal bigDecimalBd09Longitude = new BigDecimal(Double.valueOf(longitude));
                                BigDecimal bigDecimalBd09Latitude = new BigDecimal(Double.valueOf(latitude));

                                double gcj02LongitudeDouble = bigDecimalGcj02Longitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                                double gcj02LatitudeDouble = bigDecimalGcj02Latitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                                double bd09LongitudeDouble = bigDecimalBd09Longitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();
                                double bd09LatitudeDouble = bigDecimalBd09Latitude.setScale(9, BigDecimal.ROUND_HALF_UP).doubleValue();


                                Log.d("DEBUG", "gcj02LongitudeDouble is " + gcj02LongitudeDouble);
                                Log.d("DEBUG", "gcj02LatitudeDouble is " + gcj02LatitudeDouble);
                                Log.d("DEBUG", "bd09LongitudeDouble is " + bd09LongitudeDouble);
                                Log.d("DEBUG", "bd09LatitudeDouble is " + bd09LatitudeDouble);


                                //如果bd09转gcj02 结果误差很小  认为该坐标在国外
                                if ((Math.abs(gcj02LongitudeDouble - bd09LongitudeDouble)) <= error && (Math.abs(gcj02LatitudeDouble - bd09LatitudeDouble)) <= error) {
                                    //不进行坐标转换
                                    latLngInfo = longitude + "&" + latitude;
                                    Log.d("DEBUG", "OUT OF CHN, NO NEED TO TRANSFORM COORDINATE");
//                                    DisplayToast("OUT OF CHN, NO NEED TO TRANSFORM COORDINATE");
                                } else {
                                    //离线转换坐标系
//                                    double latLng[] = Utils.bd2wgs(Double.valueOf(longitude), Double.valueOf(latitude));
                                    double latLng[] = Utils.gcj02towgs84(Double.valueOf(gcj02Longitude), Double.valueOf(gcj02Latitude));
                                    latLngInfo = latLng[0] + "&" + latLng[1];
                                    Log.d("DEBUG", "IN CHN, NEED TO TRANSFORM COORDINATE");
//                                    DisplayToast("IN CHN, NEED TO TRANSFORM COORDINATE");
                                }
                            }
                            //api接口转换失败 认为在国内
                            else {
                                //离线转换坐标系
                                double latLng[] = Utils.bd2wgs(Double.valueOf(longitude), Double.valueOf(latitude));
                                latLngInfo = latLng[0] + "&" + latLng[1];
                                Log.d("DEBUG", "IN CHN, NEED TO TRANSFORM COORDINATE");
//                                DisplayToast("BD Map Api Return not Zero, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
                            }

                        } catch (JSONException e) {
                            Log.e("JSON", "resolve json error");
                            e.printStackTrace();
                            //离线转换坐标系
                            double latLng[] = Utils.bd2wgs(Double.valueOf(longitude), Double.valueOf(latitude));
                            latLngInfo = latLng[0] + "&" + latLng[1];
                            Log.d("DEBUG", "IN CHN, NEED TO TRANSFORM COORDINATE");
//                            DisplayToast("Resolve JSON Error, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //http 请求失败
                Log.e("HTTP", "HTTP GET FAILED");
                //离线转换坐标系
                double latLng[] = Utils.bd2wgs(Double.valueOf(longitude), Double.valueOf(latitude));
                latLngInfo = latLng[0] + "&" + latLng[1];
                Log.d("DEBUG", "IN CHN, NEED TO TRANSFORM COORDINATE");
//                DisplayToast("HTTP Get Failed, ASSUME IN CHN, NEED TO TRANSFORM COORDINATE");
            }
        });
        // 给请求设置tag
        stringRequest.setTag("MapAPI");
        // 添加tag到请求队列
        mRequestQueue.add(stringRequest);
        //离线转换坐标系
//        double latLng[]= Utils.bd2wgs(Double.valueOf(longitude),Double.valueOf(latitude));
//        latLngInfo=longitude+"&"+latitude;
//        latLngInfo=latLng[0]+"&"+latLng[1];
    }

    //根据经纬度更新位置信息 并插表
    private void updatePositionInfo() {
        //参数坐标系：bd09
        final String mcode = "52:67:11:2E:F4:56:92:4F:3D:C9:A7:DD:CA:80:D8:29:C1:E1:0A:96;com.example.mockgps";
        final String ak = "8geZBnkT8aQ9zwWownqr9ZSesAfxnGbM";
        //bd09坐标的位置信息
        String mapApiUrl = "http://api.map.baidu.com/geocoder/v2/?location=" + currentPt.latitude + "," + currentPt.longitude + "&output=json&pois=1&ak=" + ak + "&mcode=" + mcode;
        StringRequest stringRequest = new StringRequest(mapApiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject getRetJson = new JSONObject(response);
                            //位置获取成功
                            if (Integer.valueOf(getRetJson.getString("status")) == 0) {
                                Log.d("HTTP", "call api success");
                                JSONObject posInfoJson = getRetJson.getJSONObject("result");
                                String formatted_address = posInfoJson.getString("formatted_address");
//                                DisplayToast(tmp);
                                Log.d("DEBUG", formatted_address);

                                //插表参数
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("Location", formatted_address);
                                String latLngStr[] = latLngInfo.split("&");
                                contentValues.put("WGS84Longitude", latLngStr[0]);
                                contentValues.put("WGS84Latitude", latLngStr[1]);
                                contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                                contentValues.put("BD09Longitude", "" + currentPt.longitude);
                                contentValues.put("BD09Latitude", "" + currentPt.latitude);

                                if (!insertHistoryLocTable(locHistoryDB, HistoryDBHelper.TABLE_NAME, contentValues)) {
                                    Log.e("DATABASE", "insertHistoryLocTable[HistoryLocation] error");
                                } else {
                                    Log.d("DATABASE", "insertHistoryLocTable[HistoryLocation] success");
                                }
                            }
                            //位置获取失败
                            else {
                                //插表参数
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("Location", "NULL");
                                String latLngStr[] = latLngInfo.split("&");
                                contentValues.put("WGS84Longitude", latLngStr[0]);
                                contentValues.put("WGS84Latitude", latLngStr[1]);
                                contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                                contentValues.put("BD09Longitude", "" + currentPt.longitude);
                                contentValues.put("BD09Latitude", "" + currentPt.latitude);

                                if (!insertHistoryLocTable(locHistoryDB, HistoryDBHelper.TABLE_NAME, contentValues)) {
                                    Log.e("DATABASE", "insertHistoryLocTable[HistoryLocation] error");
                                } else {
                                    Log.d("DATABASE", "insertHistoryLocTable[HistoryLocation] success");
                                }
                            }

                        } catch (JSONException e) {
                            Log.e("JSON", "resolve json error");
                            //插表参数
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("Location", "NULL");
                            String latLngStr[] = latLngInfo.split("&");
                            contentValues.put("WGS84Longitude", latLngStr[0]);
                            contentValues.put("WGS84Latitude", latLngStr[1]);
                            contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                            contentValues.put("BD09Longitude", "" + currentPt.longitude);
                            contentValues.put("BD09Latitude", "" + currentPt.latitude);

                            if (!insertHistoryLocTable(locHistoryDB, HistoryDBHelper.TABLE_NAME, contentValues)) {
                                Log.e("DATABASE", "insertHistoryLocTable[HistoryLocation] error");
                            } else {
                                Log.d("DATABASE", "insertHistoryLocTable[HistoryLocation] success");
                            }
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //http 请求失败
                Log.e("HTTP", "HTTP GET FAILED");
                //插表参数
                ContentValues contentValues = new ContentValues();
                contentValues.put("Location", "NULL");
                String latLngStr[] = latLngInfo.split("&");
                contentValues.put("WGS84Longitude", latLngStr[0]);
                contentValues.put("WGS84Latitude", latLngStr[1]);
                contentValues.put("TimeStamp", System.currentTimeMillis() / 1000);
                contentValues.put("BD09Longitude", "" + currentPt.longitude);
                contentValues.put("BD09Latitude", "" + currentPt.latitude);

                if (!insertHistoryLocTable(locHistoryDB, HistoryDBHelper.TABLE_NAME, contentValues)) {
                    Log.e("DATABASE", "insertHistoryLocTable[HistoryLocation] error");
                } else {
                    Log.d("DATABASE", "insertHistoryLocTable[HistoryLocation] success");
                }
            }
        });
        // 给请求设置tag
        stringRequest.setTag("MapAPI");
        // 添加tag到请求队列
        mRequestQueue.add(stringRequest);
    }

    public class MockServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int statusCode;
            Bundle bundle = intent.getExtras();
            assert bundle != null;
            statusCode = bundle.getInt("statusCode");
            Log.d("DEBUG", statusCode + "");
            if (statusCode == RunCode) {
                isServiceRun = true;
            } else if (statusCode == StopCode) {
                isServiceRun = false;
            }
        }
    }

    public static boolean setHistoryLocation(String bd09Longitude, String bd09Latitude, String wgs84Longitude, String wgs84Latitude) {
        boolean ret = true;
        try {
            if (!bd09Longitude.isEmpty() && !bd09Latitude.isEmpty()) {
                currentPt = new LatLng(Double.valueOf(bd09Latitude), Double.valueOf(bd09Longitude));
                MarkerOptions ooA = new MarkerOptions().position(currentPt).icon(bdA);
                mBaiduMap.clear();
                mBaiduMap.addOverlay(ooA);
                MapStatusUpdate mapstatusupdate = MapStatusUpdateFactory.newLatLng(currentPt);
                mBaiduMap.setMapStatus(mapstatusupdate);
                latLngInfo = wgs84Longitude + "&" + wgs84Latitude;

            }
        } catch (Exception e) {
            ret = false;
            Log.e("UNKNOWN", "setHistoryLocation error");
            e.printStackTrace();
        }
        return ret;
    }

    class MyPoiOverlay extends PoiOverlay {
        private MyPoiOverlay(BaiduMap arg0) {
            super(arg0);
        }

        @Override
        public boolean onPoiClick(int arg0) {
            super.onPoiClick(arg0);
            PoiResult poiResult = getPoiResult();
            if (poiResult != null && poiResult.getAllPoi() != null) {
                PoiInfo poiInfo;
                poiInfo = poiResult.getAllPoi().get(arg0);
                currentPt = poiInfo.location;
                transformCoordinate(Double.toString(currentPt.longitude), Double.toString(currentPt.latitude));
                // 检索poi详细信息
                poiSearch.searchPoiDetail(new PoiDetailSearchOption()
                        .poiUid(poiInfo.uid));
            }
            SuggestionResult suggestionResult = getSugResult();
            if (suggestionResult != null && suggestionResult.getAllSuggestions() != null) {
                SuggestionResult.SuggestionInfo suggestionInfo;
                suggestionInfo = suggestionResult.getAllSuggestions().get(arg0);
                currentPt = suggestionInfo.pt;
                transformCoordinate(Double.toString(currentPt.longitude), Double.toString(currentPt.latitude));
                // 检索sug详细信息
                poiSearch.searchPoiDetail(new PoiDetailSearchOption()
                        .poiUid(suggestionInfo.uid));
            }
            return true;
        }
    }

}
