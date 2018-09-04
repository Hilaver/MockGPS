package com.example.mockgps;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.mockgps.PageFragment2.localMapList;
import static com.example.mockgps.PageFragment2.updateView;

/*离线地图搜索页面*/

public class PageFragment extends Fragment implements MKOfflineMapListener {

    public static final String ARG_PAGE = "ARG_PAGE";
    private int mPage;

    private ListView cityListView;
    private SimpleAdapter simAdapt;
    private SearchView citySearchView;
    private TextView tipText;

    private int checkedCityID=-1;

    public static MKOfflineMap mOffline = null;

    //
    List<Map<String, Object>> allCityList;
    List<Map<String, Object>> hotCityList;

    public static PageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        PageFragment pageFragment = new PageFragment();
        pageFragment.setArguments(args);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
        //Log.d("PageFragment","mPage is "+mPage);
        Log.d("PageFragment","onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {

//        mOffline.destroy();

        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("PageFragment","onCreateView");
        View view = inflater.inflate(R.layout.fragment_page, container, false);
//        TextView textView = (TextView)view.findViewById(R.id.testText1);
//        textView.setText("你瞅啥");
        //init search view
        citySearchView=view.findViewById(R.id.searchView);
        citySearchView.setIconifiedByDefault(true);
//        citySearchView.setIconified(false);
//        citySearchView.setSubmitButtonEnabled(true);
//        citySearchView.onActionViewExpanded();
        citySearchView.setFocusable(false);
        citySearchView.requestFocusFromTouch();
        //去掉搜索框的下划线
        if (citySearchView != null) {
            try {        //--拿到字节码
                Class<?> argClass = citySearchView.getClass();
                //--指定某个私有属性,mSearchPlate是搜索框父布局的名字
                Field ownField = argClass.getDeclaredField("mSearchPlate");
                //--暴力反射,只有暴力反射才能拿到私有属性
                ownField.setAccessible(true);
                View mView = (View) ownField.get(citySearchView);
                //--设置背景
                mView.setBackgroundColor(Color.TRANSPARENT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //offline map
        if (mOffline==null){
            mOffline = new MKOfflineMap();
            mOffline.init(this);
        }
        //tipText
        tipText=view.findViewById(R.id.tipText);
        //获取城市列表
        allCityList=fetchAllCity();
        hotCityList=fetchAllHotCity();
        //fill listview
        cityListView=view.findViewById(R.id.city_list_view);
        simAdapt = new SimpleAdapter(
                view.getContext(),
                allCityList,
                R.layout.offline_city_item,
                new String[]{"key_cityname", "key_citysize","key_cityid"},// 与下面数组元素要一一对应
                new int[]{R.id.CityNameText, R.id.CitySizeText,R.id.CityIDText});
        cityListView.setAdapter(simAdapt);
        //list item click
        setItemClickListener();
        //设置搜索的监听
        setSearchListener();
        return view;
    }

    //WIFI是否可用
    private boolean isWifiConnected(){
        ConnectivityManager mConnectivityManager = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWiFiNetworkInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWiFiNetworkInfo != null) {
            return mWiFiNetworkInfo.isAvailable();
        }
        return false;
    }

    //MOBILE网络是否可用
    private boolean isMobileConnected(){
        ConnectivityManager mConnectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mMobileNetworkInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mMobileNetworkInfo != null) {
            return mMobileNetworkInfo.isAvailable();
        }
        return false;
    }

    //网络是否可用
    private boolean isNetworkAvailable(){
        return isWifiConnected()||isMobileConnected();
    }


    public String formatDataSize(long size) {
        String ret = "";
        if (size < (1024 * 1024)) {
            ret = String.format("%dK", size / 1024);
        } else {
            ret = String.format("%.1fM", size / (1024 * 1024.0));
        }
        return ret;
    }

    private void setSearchListener(){
        citySearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // 当点击搜索按钮时触发该方法
            @Override
            public boolean onQueryTextSubmit(String query) {
//                DisplayToast("click submit");
                return false;
            }

            // 当搜索内容改变时触发该方法
            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)){
                    tipText.setText("全国");
                    simAdapt = new SimpleAdapter(
                            PageFragment.this.getContext(),
                            allCityList,
                            R.layout.offline_city_item,
                            new String[]{"key_cityname", "key_citysize","key_cityid"},// 与下面数组元素要一一对应
                            new int[]{R.id.CityNameText, R.id.CitySizeText,R.id.CityIDText});
                    cityListView.setAdapter(simAdapt);
                }else{
                    ArrayList<MKOLSearchRecord> records = mOffline.searchCity(newText);
                    List<Map<String, Object>> searchRet=new ArrayList<Map<String, Object>>();
                    if (records!=null){
                        if (records.size() > 0) {
                            tipText.setText("搜索结果");
                            for (MKOLSearchRecord r :records){
                                Log.d("CITY",""+r.cityName);
                                Map<String, Object> item = new HashMap<String, Object>();
                                item.put("key_cityname", r.cityName);
                                item.put("key_citysize", formatDataSize(r.dataSize));
                                item.put("key_cityid", r.cityID);
                                searchRet.add(item);
                            }
                            simAdapt = new SimpleAdapter(
                                    PageFragment.this.getContext(),
                                    searchRet,
                                    R.layout.offline_city_item,
                                    new String[]{"key_cityname", "key_citysize","key_cityid"},// 与下面数组元素要一一对应
                                    new int[]{R.id.CityNameText, R.id.CitySizeText,R.id.CityIDText});
                            cityListView.setAdapter(simAdapt);
                        }
                    }
                    else {
                        tipText.setText("热门城市");
                        DisplayToast("未搜索到该城市,或该城市不支持离线地图");
                        simAdapt = new SimpleAdapter(
                                PageFragment.this.getContext(),
                                hotCityList,
                                R.layout.offline_city_item,
                                new String[]{"key_cityname", "key_citysize","key_cityid"},// 与下面数组元素要一一对应
                                new int[]{R.id.CityNameText, R.id.CitySizeText,R.id.CityIDText});
                        cityListView.setAdapter(simAdapt);
                    }
                }
                return false;
            }
        });
    }

    private void setItemClickListener(){
        cityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //获取cityID
                final String cityID=((TextView)(view.findViewById(R.id.CityIDText))).getText().toString();
                final String cityName=((TextView)(view.findViewById(R.id.CityNameText))).getText().toString();
//                DisplayToast("city id is "+cityID);
                checkedCityID=Integer.parseInt(cityID);
                new AlertDialog.Builder(PageFragment.this.getContext())
                        .setTitle("Tips")//这里是表头的内容
                        .setMessage("确定要下载"+cityName+"的离线地图吗?")//这里是中间显示的具体信息
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        boolean exist=false;
                                        boolean needUpdate=true;
                                        for (MKOLUpdateElement e:localMapList){
                                            if (e.cityID==Integer.parseInt(cityID)){
                                                exist=true;
                                                if (!e.update){
                                                    needUpdate=false;
                                                    DisplayToast("离线地图已存在");
                                                }
                                                break;
                                            }
                                        }
                                        if (isNetworkAvailable()){
                                            if (!exist){
                                                mOffline.start(Integer.parseInt(cityID));
                                                citySearchView.onActionViewCollapsed();
                                                DisplayToast("开始下载离线地图");
                                            }else {
                                                if (needUpdate){
                                                    mOffline.update(Integer.parseInt(cityID));
                                                    DisplayToast("开始更新离线地图");
                                                }
                                            }
                                        }else{
                                            DisplayToast("网络连接不可用,请检查网络设置");
                                        }

                                    }
                                })
                        .setNegativeButton("取消",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .show();
            }
        });
    }

    private List<Map<String, Object>> fetchAllCity(){
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        // 获取所有支持离线地图的城市
        try{
            ArrayList<MKOLSearchRecord> records1 = mOffline.getOfflineCityList();
            if (records1 != null) {
                for (MKOLSearchRecord r : records1) {
                    //V4.5.0起，保证数据不溢出，使用long型保存数据包大小结果
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("key_cityname", r.cityName);
                    item.put("key_citysize", this.formatDataSize(r.dataSize));
                    item.put("key_cityid", r.cityID);
                    data.add(item);
                }
            }
        }catch (Exception e){
            data.clear();
            e.printStackTrace();
        }

        return data;


    }

    private List<Map<String, Object>> fetchAllHotCity(){
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        // 获取热闹城市列表
        try{
            ArrayList<MKOLSearchRecord> records1 = mOffline.getHotCityList();
            if (records1 != null) {
                for (MKOLSearchRecord r : records1) {
                    //V4.5.0起，保证数据不溢出，使用long型保存数据包大小结果
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("key_cityname", r.cityName);
                    item.put("key_citysize", this.formatDataSize(r.dataSize));
                    item.put("key_cityid", r.cityID);
                    data.add(item);
                }
            }
        }catch (Exception e){
            data.clear();
            e.printStackTrace();
        }

        return data;

//        try{
//            int cnt=20;
//            while (cnt-->0) {
//                Map<String, Object> item = new HashMap<String, Object>();
//                item.put("key_cityname", cnt+"成都市");
//                item.put("key_citysize", "120.3M");
//                item.put("key_cityid", ""+cnt);
//                data.add(item);
//            }
//        }catch (Exception e){
//            data.clear();
//            e.printStackTrace();
//        }
//        return data;
    }

    @Override
    public void onGetOfflineMapState(int type, int state) {
        switch (type) {
            case MKOfflineMap.TYPE_DOWNLOAD_UPDATE: {
                MKOLUpdateElement update = mOffline.getUpdateInfo(state);
                // 处理下载进度更新提示
                if (update != null) {
//                    Log.d("TYPE_DOWNLOAD_UPDATE",String.format("%s : %d%%", update.cityName, update.ratio));
                    updateView();
                }
            }
            break;

            case MKOfflineMap.TYPE_NEW_OFFLINE:
                // 有新离线地图安装
                Log.d("OfflineDemo", String.format("add offlinemap num:%d", state));
                break;

            case MKOfflineMap.TYPE_VER_UPDATE:
                // 版本更新提示
                // MKOLUpdateElement e = mOffline.getUpdateInfo(state);
                break;

            default:
                break;
        }

    }

    public MKOfflineMapListener getMKOfflineMapListener(){
        return PageFragment.this;
    }

    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(PageFragment.this.getContext(), str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 250);
        toast.show();
    }
}