package com.why94.recycler;

import android.view.View;
import android.view.ViewGroup;

/**
 * LoadMoreHolder
 * Created by WenHuayu(why94@qq.com) on 2017/3/30.
 */
public abstract class RecyclerLoadMoreHolder extends RecyclerAdapter.Holder<RecyclerLoadMoreHolder.Manager> {

    public RecyclerLoadMoreHolder(ViewGroup group, View view) {
        super(group, view);
    }

    public RecyclerLoadMoreHolder(ViewGroup group, int layout) {
        super(group, layout);
    }

    @Override
    protected void bindData(int position, Manager manager) {
        manager.start(this);
    }

    protected void onPause(Runnable runnable) {
        runnable.run();
    }

    /**
     * 这个方法的作用不应该与对象绑定,因为可能有多个holder,但是并不确定会调用哪一个的方法,可以用来处理一些外部类或全局的数据,比如当没有更多数据需要加载时,添加一个提示
     * The effect of this method should not be bound to the object, because there may be more than one holder, but it is not certain which one will be called. It can be used to process some external classes or global data. For example, when no more data needs to be loaded, add a message holder
     */
    protected void onComplete() {
    }

    public static class Manager {

        private final RecyclerAdapter mAdapter;
        private final Listener mListener;

        private RecyclerLoadMoreHolder mHolder;
        private int mHolderPosition;

        private boolean mIsLoading;
        private boolean mIsPause;

        private final int size;
        private int page;

        public Manager(RecyclerAdapter adapter, int page, int size, Listener listener) {
            this.mListener = listener;
            this.mAdapter = adapter;
            this.page = page;
            this.size = size;
        }

        void start(RecyclerLoadMoreHolder holder) {
            //
            mHolder = holder;
            //
            if (mIsLoading) {
                return;
            }
            mIsLoading = true;
            //
            mHolderPosition = holder.getAdapterPosition();
            //
            mListener.onLoadMore(this, page, size);
            page++;
            //
        }

        public final void pause() {
            pause(null);
        }

        public final void pause(final Runnable runnable) {
            if (mIsPause || !mIsLoading) {
                return;
            }
            mHolder.onPause(new Runnable() {
                @Override
                public void run() {
                    mIsPause = true;
                    mAdapter.remove(mHolderPosition);
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            });
        }

        public final void resume(boolean hasMore) {
            if (!mIsPause || !mIsLoading) {
                return;
            }
            mIsPause = false;
            if (hasMore) {
                mAdapter.add(mHolder.getClass(), Manager.this);
                mIsLoading = false;
            } else {
                mHolder.onComplete();
            }
        }
    }

    public interface Listener {
        void onLoadMore(Manager manager, int page, int size);
    }
}