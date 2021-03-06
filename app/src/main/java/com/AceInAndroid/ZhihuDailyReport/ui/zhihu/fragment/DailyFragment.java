package com.AceInAndroid.ZhihuDailyReport.ui.zhihu.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.AceInAndroid.ZhihuDailyReport.component.RxBus;
import com.AceInAndroid.ZhihuDailyReport.presenter.DailyPresenter;
import com.AceInAndroid.ZhihuDailyReport.presenter.contract.DailyContract;
import com.AceInAndroid.ZhihuDailyReport.ui.zhihu.activity.CalendarActivity;
import com.AceInAndroid.ZhihuDailyReport.ui.zhihu.activity.ZhihuDetailActivity;
import com.AceInAndroid.ZhihuDailyReport.R;
import com.AceInAndroid.ZhihuDailyReport.base.BaseFragment;
import com.AceInAndroid.ZhihuDailyReport.model.bean.DailyBeforeListBean;
import com.AceInAndroid.ZhihuDailyReport.model.bean.DailyListBean;
import com.AceInAndroid.ZhihuDailyReport.ui.zhihu.adapter.DailyAdapter;
import com.AceInAndroid.ZhihuDailyReport.util.CircularAnimUtil;
import com.AceInAndroid.ZhihuDailyReport.util.DateUtil;
import com.AceInAndroid.ZhihuDailyReport.util.SnackbarUtil;
import com.AceInAndroid.ZhihuDailyReport.widget.ProgressImageView;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by AceInAndroid on 2017/4/11.
 */
public class DailyFragment extends BaseFragment<DailyPresenter> implements DailyContract.View {

    @BindView(R.id.fab_calender)
    FloatingActionButton fabCalender;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.rv_daily_list)
    RecyclerView rvDailyList;
    @BindView(R.id.iv_progress)
    ProgressImageView ivProgress;

    String currentDate;
    DailyAdapter mAdapter;
    List<DailyListBean.StoriesBean> mList = new ArrayList<>();

    @Override
    protected void initInject() {
        getFragmentComponent().inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_daily;
    }

    @Override
    protected void initEventAndData() {
        currentDate = DateUtil.getTomorrowDate();
        mAdapter = new DailyAdapter(mContext,mList);
        mAdapter.setOnItemClickListener(new DailyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position,View shareView) {
                mPresenter.insertReadToDB(mList.get(position).getId());
                mAdapter.setReadState(position,true);
                if(mAdapter.getIsBefore()) {
                    mAdapter.notifyItemChanged(position + 1);
                } else {
                    mAdapter.notifyItemChanged(position + 2);
                }
                Intent intent = new Intent();
                intent.setClass(mContext, ZhihuDetailActivity.class);
                intent.putExtra("id",mList.get(position).getId());
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(mActivity, shareView, "shareView");
                mContext.startActivity(intent,options.toBundle());
            }
        });
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(currentDate.equals(DateUtil.getTomorrowDate())) {
                    mPresenter.getDailyData();
                } else {
                    int year = Integer.valueOf(currentDate.substring(0,4));
                    int month = Integer.valueOf(currentDate.substring(4,6));
                    int day = Integer.valueOf(currentDate.substring(6,8));
                    CalendarDay date = CalendarDay.from(year, month - 1, day);
                    RxBus.getDefault().post(date);
                }
            }
        });
        rvDailyList.setLayoutManager(new LinearLayoutManager(mContext));
        rvDailyList.setAdapter(mAdapter);
        ivProgress.start();
        mPresenter.getDailyData();
    }

    /**
     * 当天数据
     * @param info
     */
    @Override
    public void showContent(DailyListBean info) {
        if(swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        } else {
            ivProgress.stop();
        }
        mList = info.getStories();
        currentDate = String.valueOf(Integer.valueOf(info.getDate()) + 1);
        mAdapter.addDailyDate(info);
        mPresenter.stopInterval();
        mPresenter.startInterval();
    }

    /**
     * 过往数据
     * @param date
     * @param info
     */
    @Override
    public void showMoreContent(String date,DailyBeforeListBean info) {
        if(swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        } else {
            ivProgress.stop();
        }
        mPresenter.stopInterval();
        mList = info.getStories();
        currentDate = String.valueOf(Integer.valueOf(info.getDate()));
        mAdapter.addDailyBeforeDate(info);
    }

    @Override
    public void doInterval(int currentCount) {
        mAdapter.changeTopPager(currentCount);
    }

    @OnClick(R.id.fab_calender)
    void startCalender() {
        Intent it = new Intent();
        it.setClass(mContext,CalendarActivity.class);
        CircularAnimUtil.startActivity(mActivity,it,fabCalender,R.color.fab_bg);
    }

    @Override
    public void showError(String msg) {
        if(swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        } else {
            ivProgress.stop();
        }
        SnackbarUtil.showShort(rvDailyList,msg);
    }
}
