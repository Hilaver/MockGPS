package com.example.mockgps;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.example.service.HistoryDBHelper;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.mockgps.MainActivity.setHistoryLocation;


public class HistoryActivity extends AppCompatActivity {
    private ListView listView;
    private SimpleAdapter simAdapt;
    private TextView noRecordText;
//    private List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

    private HistoryDBHelper historyDBHelper;
    private SQLiteDatabase sqLiteDatabase;

    private String bd09Longitude="104.07018449827267";
    private String bd09Latitude="30.547743718042415";
    private String wgs84Longitude="104.06121778639009";
    private String wgs84Latitude="30.544111926165282";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_list);

        Log.d("HistoryActivity","SQLiteDatabase init");

        //sqlite
        try{
            historyDBHelper = new HistoryDBHelper(getApplicationContext());
            sqLiteDatabase = historyDBHelper.getWritableDatabase();
        }catch (Exception e){
            Log.e("HistoryActivity","SQLiteDatabase init error");
            e.printStackTrace();
        }

        listView = (ListView) findViewById(R.id.list_view);
        noRecordText=(TextView)findViewById(R.id.no_record_textview);

        initListView();


//        listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //bd09坐标
                String bd09LatLng=(String) ((TextView) view.findViewById(R.id.BDLatLngText)).getText();
                bd09LatLng=bd09LatLng.substring(bd09LatLng.indexOf("[")+1,bd09LatLng.indexOf("]"));
                String latLngStr[]=bd09LatLng.split(" ");
                bd09Longitude=latLngStr[0].substring(latLngStr[0].indexOf(":")+1);
                bd09Latitude=latLngStr[1].substring(latLngStr[1].indexOf(":")+1);
                //wgs84坐标
                String wgs84LatLng=(String) ((TextView) view.findViewById(R.id.WGSLatLngText)).getText();
                wgs84LatLng=wgs84LatLng.substring(wgs84LatLng.indexOf("[")+1,wgs84LatLng.indexOf("]"));
                String latLngStr2[]=wgs84LatLng.split(" ");
                wgs84Longitude=latLngStr2[0].substring(latLngStr2[0].indexOf(":")+1);
                wgs84Latitude=latLngStr2[1].substring(latLngStr2[1].indexOf(":")+1);
                if (!setHistoryLocation(bd09Longitude,bd09Latitude,wgs84Longitude,wgs84Latitude)){
                    DisplayToast("定位失败,请手动选取定位点");
                }
//                updatePositionInfo(bd09Longitude,bd09Latitude);
                returnLastActivity();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle("Warning")//这里是表头的内容
                        .setMessage("确定要删除该项历史记录吗?")//这里是中间显示的具体信息
                        .setPositiveButton("确定",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String locID=(String) ((TextView) view.findViewById(R.id.LocationID)).getText();
                                        boolean deleteRet=deleteRecord(sqLiteDatabase,HistoryDBHelper.TABLE_NAME,Integer.valueOf(locID));
                                        if (deleteRet){
                                            DisplayToast("删除成功!");
                                            initListView();
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
                return true;
            }
        });


        if (recordArchive(sqLiteDatabase,HistoryDBHelper.TABLE_NAME)){
            Log.d("HistoryActivity","archive success");
        }

//        sqLiteDatabase.close();

    }

    private void initListView(){
        List<Map<String, Object>> allHistoryRecord=fetchAllRecord(sqLiteDatabase,HistoryDBHelper.TABLE_NAME);
        if (allHistoryRecord.size()==0){
            listView.setVisibility(View.GONE);
            noRecordText.setVisibility(View.VISIBLE);
        }else {
            try {
                simAdapt = new SimpleAdapter(
                        this,
                        allHistoryRecord,
                        R.layout.history_item,
                        new String[]{"key_id", "key_location", "key_time","key_wgslatlng","kdy_bdlatlng"},// 与下面数组元素要一一对应
                        new int[]{R.id.LocationID, R.id.LoctionText, R.id.TimeText,R.id.WGSLatLngText,R.id.BDLatLngText});
                listView.setAdapter(simAdapt);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }


    public void returnLastActivity(){
        this.finish();
    }

    //sqlite 操作 查询所有记录
    private List<Map<String, Object>> fetchAllRecord(SQLiteDatabase sqLiteDatabase, String tableName){
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        try{
            Cursor cursor = sqLiteDatabase.query(tableName, null,
                    "ID > ?", new String[]{"0"},
                    null, null, "TimeStamp DESC", null);
            while (cursor.moveToNext()) {
                Map<String, Object> item = new HashMap<String, Object>();
                int ID=cursor.getInt(0);
                String Location=cursor.getString(1);
                String Longitude=cursor.getString(2);
                String Latitude=cursor.getString(3);
                long TimeStamp=cursor.getInt(4);
                String BD09Longitude=cursor.getString(5);
                String BD09Latitude=cursor.getString(6);
                Log.d("TB",ID+"\t"+Location+"\t"+Longitude+"\t"+Latitude+"\t"+TimeStamp+"\t"+BD09Longitude+"\t"+BD09Latitude);

                BigDecimal bigDecimalLongitude = new BigDecimal(Double.valueOf(Longitude));
                BigDecimal bigDecimalLatitude = new BigDecimal(Double.valueOf(Latitude));
                BigDecimal bigDecimalBDLongitude = new BigDecimal(Double.valueOf(BD09Longitude));
                BigDecimal bigDecimalBDLatitude = new BigDecimal(Double.valueOf(BD09Latitude));

                double doubleLongitude = bigDecimalLongitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();
                double doubleLatitude = bigDecimalLatitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();

                double doubleBDLongitude = bigDecimalBDLongitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();
                double doubleBDLatitude = bigDecimalBDLatitude.setScale(11, BigDecimal.ROUND_HALF_UP).doubleValue();

                item.put("key_id",""+ID);
                item.put("key_location", Location);
                item.put("key_time", timeStamp2Date(Long.toString(TimeStamp),null));
                item.put("key_wgslatlng", "[经度:"+doubleLongitude+" 纬度:"+doubleLatitude+"]");
                item.put("kdy_bdlatlng", "[经度:"+doubleBDLongitude+" 纬度:"+doubleBDLatitude+"]");
                data.add(item);
            }
            // 关闭光标
            cursor.close();
        }catch (Exception e){
            Log.e("SQLITE","query error");
            data.clear();
            e.printStackTrace();
        }
        return data;
    }

    public static String timeStamp2Date(String seconds,String format) {
        if(seconds == null || seconds.isEmpty() || seconds.equals("null")){
            return "";
        }
        if(format == null || format.isEmpty()) format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(Long.valueOf(seconds+"000")));
    }

    //sqlite 操作 保留七天的数据
    private boolean recordArchive(SQLiteDatabase sqLiteDatabase, String tableName) {
        boolean archiveRet = true;
        final long weekSecond = 7 * 24 * 60 * 60;
        try {
            sqLiteDatabase.delete(tableName,
                    "TimeStamp < ?", new String[]{Long.toString(System.currentTimeMillis() / 1000 - weekSecond)});
        } catch (Exception e) {
            Log.e("SQLITE", "archive error");
            archiveRet = false;
            e.printStackTrace();
        }Log.d("SQLITE", "archive success");
        return archiveRet;
    }

    //sqlite 操作 删除记录
    private boolean deleteRecord(SQLiteDatabase sqLiteDatabase, String tableName, int ID) {
        boolean deleteRet = true;
        try {
            sqLiteDatabase.delete(tableName,
                    "ID = ?", new String[]{Integer.toString(ID)});
            Log.d("DDDDDD","delete success");
        } catch (Exception e) {
            Log.e("SQLITE", "delete error");
            deleteRet = false;
            e.printStackTrace();
        }
        return deleteRet;
    }

    @Override
    protected void onDestroy() {
        //close db
        sqLiteDatabase.close();
        super.onDestroy();
    }




    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }


}