package com.why94.recycler;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class RecyclerLoadMoreHolderTestActivity extends Activity implements RecyclerLoadMoreHolder.Listener {

    private static final String TAG = "LoadMoreHolderTest";

    private RecyclerView mRecycler;
    private RecyclerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mRecycler = findViewById(R.id.recycler);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RecyclerAdapter(this);
        mRecycler.setAdapter(mAdapter);

        mAdapter.add(LoadMoreHolder.class, new RecyclerLoadMoreHolder.Manager(mAdapter, 0, 5, this));
    }

    @Override
    public void onLoadMore(final RecyclerLoadMoreHolder.Manager manager, final int page, final int size) {
        Log.e(TAG, String.format("start: %s,%s", page, size));
        mRecycler.postDelayed(new Runnable() {
            @Override
            public void run() {
                manager.pause(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < size; i++) {
                            mAdapter.add(Holder.class, page * size + i);
                        }
                        manager.resume(page < 10);
                    }
                });
                Log.e(TAG, String.format("ok: %s,%s", page, size));
            }
        }, 1000);
    }

    class LoadMoreHolder extends RecyclerLoadMoreHolder {

        String text1 = String.format(Locale.getDefault(), "加载中,请稍候:%d", hashCode());

        public LoadMoreHolder(ViewGroup group) {
            super(group, new TextView(group.getContext()));
        }

        @Override
        protected void bindData(int position, Manager manager) {
            itemView.setTranslationX(0);
            super.bindData(position, manager);
            itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ((TextView) itemView).setGravity(Gravity.CENTER);
            ((TextView) itemView).setText(text1);
        }

        @Override
        protected void onPause(final Runnable runnable) {
            itemView.postDelayed(new Runnable() {

                String text2 = "加载成功123456789";
                int index1 = text1.length() - 1, index2;

                @Override
                public void run() {
                    if (index1 == 0 && index2 == text2.length()) {
                        runnable.run();
                    } else if (index1 == 0) {
                        ((TextView) itemView).setText(text2.subSequence(0, index2++));
                        itemView.postDelayed(this, 100);
                    } else {
                        ((TextView) itemView).setText(text1.subSequence(0, index1--));
                        itemView.postDelayed(this, 100);
                    }
                }
            }, 100);
        }

        @Override
        protected void onComplete() {
            mAdapter.add(MsgHolder.class, "加载完成");
        }
    }

    class Holder extends RecyclerAdapter.Holder<Integer> {
        public Holder(ViewGroup group) {
            super(group, new Button(group.getContext()));
            itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        @Override
        protected void bindData(int position, Integer integer) {
            ((Button) itemView).setText(String.valueOf(integer));
        }
    }

    class MsgHolder extends RecyclerAdapter.Holder<String> {
        public MsgHolder(ViewGroup group) {
            super(group, new TextView(group.getContext()));
            itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ((TextView) itemView).setGravity(Gravity.CENTER);
        }

        @Override
        protected void bindData(int position, String msg) {
            ((TextView) itemView).setText(msg);
        }
    }
}
