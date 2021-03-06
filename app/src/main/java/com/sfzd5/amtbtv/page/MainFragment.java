package com.sfzd5.amtbtv.page;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PageRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.view.View;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.sfzd5.amtbtv.R;
import com.sfzd5.amtbtv.TVApplication;
import com.sfzd5.amtbtv.card.CardPresenterSelector;
import com.sfzd5.amtbtv.model.Card;
import com.sfzd5.amtbtv.model.Channel;
import com.sfzd5.amtbtv.model.History;
import com.sfzd5.amtbtv.model.JsonResult;
import com.sfzd5.amtbtv.model.Live;
import com.sfzd5.amtbtv.model.Program;
import com.sfzd5.amtbtv.util.CacheResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/2/28.
 */

public class MainFragment extends BrowseFragment {

    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter mRowsAdapter;
    private TVApplication app;
    Handler handler;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        app = TVApplication.getInstance();
        setupUi();
        loadData();
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        getMainFragmentRegistry().registerFragment(PageRow.class,
                new PageRowFragmentFactory(mBackgroundManager));
    }

    private void setupUi() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(getResources().getColor(R.color.fastlane_background));

        //设置标题
        setTitle(getString(R.string.amtb_tv_title));

        //搜索，打开搜索页
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "该功能暂未完成",
                        Toast.LENGTH_LONG).show();
                //Intent intent = new Intent(getActivity().getBaseContext(), SearchActivity.class);
                //startActivity(intent);
            }
        });
        prepareEntranceTransition();
    }


    private void loadData() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);

        app.http.asyncTakeFile("program.txt", new CacheResult() {
            @Override
            public void tackFile(String txt, Bitmap bmp, boolean isTxt) {
                if(txt!=null){
                    app.data = JSON.parseObject(txt, JsonResult.class);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            createRows();
                            startEntranceTransition();
                        }
                    });
                }
            }
        }, true);
    }

    private void createRows() {
        JsonResult data = TVApplication.getInstance().data;

        int id = -1;
        //检测是否存在播放记录，若存在则加入播放记录
        if(app.historyManager.historyList.size()>0){
            HeaderItem headerItem = new HeaderItem(id, "播放历史");
            PageRow pageRow = new PageRow(headerItem);
            mRowsAdapter.add(pageRow);
        }

        //直播节目
        id++;
        HeaderItem headerItem1 = new HeaderItem(id, "直播");
        PageRow pageRow1 = new PageRow(headerItem1);
        mRowsAdapter.add(pageRow1);

        //点播节目
        for(Channel c : data.channels){
            if(c.programs.size()>0) {
                id++;
                HeaderItem headerItem = new HeaderItem(id, c.name);
                PageRow pageRow = new PageRow(headerItem);
                mRowsAdapter.add(pageRow);
            }
        }
    }

    private static class PageRowFragmentFactory extends BrowseFragment.FragmentFactory {
        private final BackgroundManager mBackgroundManager;

        PageRowFragmentFactory(BackgroundManager backgroundManager) {
            this.mBackgroundManager = backgroundManager;
        }

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row)rowObj;
            HeaderItem headerItem = row.getHeaderItem();
            mBackgroundManager.setDrawable(null);
            Fragment fragment = new CardGridFragment();
            Bundle bundle = new Bundle();
            bundle.putString("channel", headerItem.getName());
            bundle.putInt("id", (int)headerItem.getId());
            fragment.setArguments(bundle);
            return fragment;
        }
    }

    public static class CardGridFragment extends GridFragment {
        private static final int COLUMNS = 6;
        private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
        private ArrayObjectAdapter mAdapter;
        private String channel;
        private int id;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle bundle = this.getArguments();
            this.channel = bundle.getString("channel");
            this.id = bundle.getInt("id");
            setupAdapter();
            loadData();
            this.getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }


        private void setupAdapter() {
            VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR);
            presenter.setNumberOfColumns(COLUMNS);
            setGridPresenter(presenter);

            CardPresenterSelector cardPresenter = new CardPresenterSelector(getActivity());
            mAdapter = new ArrayObjectAdapter(cardPresenter);
            setAdapter(mAdapter);

            setOnItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(
                        Presenter.ViewHolder itemViewHolder,
                        Object item,
                        RowPresenter.ViewHolder rowViewHolder,
                        Row row) {

                    Card card = (Card) item;
                    TVApplication app = TVApplication.getInstance();
                    app.curChannel = card.channel;
                    app.curCardId = card.id;

                    if(item instanceof Program){
                        Intent intent = new Intent(getActivity().getBaseContext(), DetailActivity.class);
                        startActivity(intent);
                    } else if(item instanceof Live){
                        Intent intent = new Intent(getActivity().getBaseContext(), PlayActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(),"未知 "+card.name, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void loadData() {
            TVApplication app = TVApplication.getInstance();
            if(id==-1){//历史
                List<Card> cards = new ArrayList<>();
                for(History history : app.historyManager.historyList){
                    /*
                    if(history.channel.equals("直播")){
                        for(Live live : app.data.lives){
                            if(live.id == history.id) {
                                live.currentPosition = history.currentPosition;
                                cards.add(live);
                                break;
                            }
                        }
                    } else { */
                        for(Channel channel : app.data.channels){
                            if(channel.name.equals(history.channel)){
                                for(Program p : channel.programs){
                                    if(p.id == history.id){
                                        p.currentPosition = history.currentPosition;
                                        cards.add(p);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    //}
                }
                mAdapter.addAll(0, cards);
            } else if (id==0){
                mAdapter.addAll(0, app.data.lives);
            } else {
                for(Channel channel : app.data.channels){
                    if(channel.name.equals(this.channel)){
                        mAdapter.addAll(0, channel.programs);
                        break;
                    }
                }
            }
        }
    }
}
