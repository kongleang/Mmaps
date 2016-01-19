package dg.shenm233.mmaps.ui.maps.views;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.overlay.PoiOverlay;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.help.Tip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dg.shenm233.mmaps.R;
import dg.shenm233.mmaps.databinding.PoiDetailBinding;
import dg.shenm233.mmaps.presenter.IMapsFragment;
import dg.shenm233.mmaps.presenter.MapsModule;
import dg.shenm233.mmaps.ui.maps.ViewContainerManager;
import dg.shenm233.mmaps.util.AMapUtils;

public class PoiItems extends ViewContainerManager.ViewContainer implements AMap.OnMarkerClickListener {
    public static final int POI_ITEMS_ID = 3;
    public static final String POI_ITEM_LIST = "poi_item_list"; // List<PoiItem>

    private ViewGroup rootView;
    private Context mContext;
    private IMapsFragment mMapsFragment;

    private PoiDetailBinding mPoiDetailBinding;

    private List poiItems;
    private PoiItem curPoiItem;
    private PoiOverlay curPoiOverlay;

    public PoiItems(ViewGroup rootView, IMapsFragment mapsFragment) {
        this.rootView = rootView;
        mContext = rootView.getContext();
        mMapsFragment = mapsFragment;
        onCreateView();
    }

    @Override
    public void onCreateView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mPoiDetailBinding = PoiDetailBinding.inflate(inflater, rootView, false);
        ViewGroup v = (ViewGroup) mPoiDetailBinding.getRoot();
        CoordinatorLayout.LayoutParams layoutParams =
                (CoordinatorLayout.LayoutParams) v.getLayoutParams();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.anchorGravity = Gravity.BOTTOM;
        mPoiDetailBinding.setHandler(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewContainerManager vm = mMapsFragment.getViewContainerManager();
                Map<String, Object> args = new HashMap<>();
                args.put(Directions.CLEAR_ALL, true);

                Tip stip = new Tip();
                stip.setName(mContext.getString(R.string.my_location));
                stip.setPostion(mMapsFragment.getMapsModule().getMyLatLonPoint());
                args.put(Directions.STARTING_POINT, stip);

                Tip dtip = new Tip();
                dtip.setName(curPoiItem.getTitle());
                dtip.setPostion(curPoiItem.getLatLonPoint());
                args.put(Directions.DESTINATION, dtip);

                vm.putViewContainer(new Directions(rootView, mMapsFragment),
                        args, false, Directions.DIRECTIONS_ID);
            }
        });

        // 设置本View被action_my_location这个View ID依赖,即定位按钮将向上移动,使它和PoiDetail视图不重合
        v.setTag(R.id.action_my_location, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void show() {
        IMapsFragment mapsFragment = mMapsFragment;
        mapsFragment.setDirectionsBtnVisibility(View.GONE);
        MapsModule mapsModule = mapsFragment.getMapsModule();

        // Hack: 显示搜索条
        ViewContainerManager.ViewContainer searchBox =
                mapsFragment.getViewContainerManager().getViewContainer(SearchBox.SEARCH_BOX_ID);
        Map<String, Object> searchBoxArguments = searchBox.getArguments();
        searchBoxArguments.put(SearchBox.BACK_BTN_AS_DRAWER, true);
        searchBoxArguments.put(SearchBox.ONLY_SEARCH_BOX, true);
        searchBox.show();

        poiItems = (List) args.get(POI_ITEM_LIST);
        (curPoiOverlay = mapsModule.addPoiOverlay(poiItems)).zoomToSpan();
        rootView.addView(mPoiDetailBinding.getRoot(), 0);

        PoiItem firstPoiItem = (PoiItem) poiItems.get(0);
        mPoiDetailBinding.setPoi(firstPoiItem);
        mapsModule.moveCamera(AMapUtils.convertToLatLng(firstPoiItem.getLatLonPoint()), 17);
        curPoiItem = firstPoiItem;
    }

    @Override
    public void exit() {
        // Hack: 隐藏搜索框
        ViewContainerManager.ViewContainer searchBox =
                mMapsFragment.getViewContainerManager().getViewContainer(SearchBox.SEARCH_BOX_ID);
        searchBox.exit();
        ((SearchBox) searchBox).clearSearchText();
        rootView.removeView(mPoiDetailBinding.getRoot());

        curPoiOverlay.removeFromMap();
        mMapsFragment.setDirectionsBtnVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {

    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    /**
     * 处理地图点击Marker事件
     *
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        mPoiDetailBinding.setPoi(curPoiItem = ((PoiItem) poiItems.get(((Integer) marker.getObject()))));
        LatLng position = marker.getPosition();
        mMapsFragment.getMapsModule().moveCamera(position, 20);
        return false;
    }
}