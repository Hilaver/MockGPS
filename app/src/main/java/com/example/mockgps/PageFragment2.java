package com.example.mockgps;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.offline.MKOLUpdateElement;

import java.util.ArrayList;

import static com.example.mockgps.PageFragment.mOffline;

/*已下载离线地图页面*/

public class PageFragment2 extends Fragment {

    public static final String ARG_PAGE = "ARG_PAGE";
    private int mPage;

//    private MKOfflineMap mOffline = null;

    public static ArrayList<MKOLUpdateElement> localMapList = null;
    public static LocalMapAdapter lAdapter = null;

    public static PageFragment2 newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        PageFragment2 pageFragment = new PageFragment2();
        pageFragment.setArguments(args);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
        //Log.d("PageFragment2","mPage is "+mPage);
        Log.d("PageFragment2","onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page_2, container, false);
        //offline map
        if (mOffline==null){
            //??
        }
        // 获取已下过的离线地图信息
        localMapList = mOffline.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<MKOLUpdateElement>();
        }
        //init list view
        ListView localMapListView = (ListView) view.findViewById(R.id.localmaplist);
        lAdapter = new LocalMapAdapter();
        localMapListView.setAdapter(lAdapter);
        return view;
    }

    public static void updateView() {
        localMapList = mOffline.getAllUpdateInfo();
        if (localMapList == null) {
            localMapList = new ArrayList<MKOLUpdateElement>();
        }
        lAdapter.notifyDataSetChanged();
    }

    /**
     * 离线地图管理列表适配器
     */
    public class LocalMapAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return localMapList.size();
        }

        @Override
        public Object getItem(int index) {
            return localMapList.get(index);
        }

        @Override
        public long getItemId(int index) {
            return index;
        }

        @Override
        public View getView(int index, View view, ViewGroup arg2) {
            MKOLUpdateElement e = (MKOLUpdateElement) getItem(index);
            view = View.inflate(PageFragment2.this.getContext(), R.layout.local_map_item, null);
            initViewItem(view, e);
            return view;
        }

        void initViewItem(View view, final MKOLUpdateElement e) {
            Button remove = (Button) view.findViewById(R.id.remove);
            Button control = (Button) view.findViewById(R.id.control);
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView update = (TextView) view.findViewById(R.id.update);
            TextView ratio = (TextView) view.findViewById(R.id.ratio);
            ratio.setText(e.ratio + "%");
            title.setText(e.cityName);

            if (e.update) {
                update.setText("可更新");
                control.setVisibility(View.VISIBLE);
            } else {
                update.setText("最新");
            }

            if (e.ratio == 100) {
                remove.setEnabled(true);
            }

            //更新按钮
            control.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DisplayToast("开始更新");
                    mOffline.update(e.cityID);
                    //updateView();
                }
            });

            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    //
                    new AlertDialog.Builder(PageFragment2.this.getContext())
                            .setTitle("Tips")//这里是表头的内容
                            .setMessage("确定要删除"+e.cityName+"的离线地图吗?")//这里是中间显示的具体信息
                            .setPositiveButton("确定",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mOffline.remove(e.cityID);
                                            updateView();
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
    }
    public void DisplayToast(String str) {
        Toast toast = Toast.makeText(PageFragment2.this.getContext(), str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 220);
        toast.show();
    }
}