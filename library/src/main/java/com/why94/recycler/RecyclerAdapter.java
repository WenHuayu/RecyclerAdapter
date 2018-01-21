package com.why94.recycler;

import android.os.AsyncTask;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerAdapter
 * Created by WenHuayu<why94@qq.com> on 2017/11/27.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue", "WeakerAccess", "unused"})
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.Holder> {

    private final List<Meta> mItems = new ArrayList<>();
    private final List<Object> mHolderInitArgs = new ArrayList<>();
    private final SparseArray<ConstructorMeta> mHolderConstructors = new SparseArray<>();
    private final HashMap<Class, Integer> mHolderTypeMap = new HashMap<>(1);

    private List<Meta> mTransactionItems;
    private AsyncTask mTransactionAsyncTask;

    public RecyclerAdapter(Object... holderInitArgs) {
        this.mHolderInitArgs.addAll(Arrays.asList(holderInitArgs));
    }

    public void addHolderInitArgs(Object... initArgs) {
        this.mHolderInitArgs.addAll(Arrays.asList(initArgs));
    }

    private List<Meta> data() {
        return mTransactionItems == null ? mItems : mTransactionItems;
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).type;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int type) {
        return mHolderConstructors.get(type).newInstance(parent);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) { }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(Holder holder, int position, List<Object> payloads) {
        holder.bindData(position, holder.data = mItems.get(position).data, payloads);
    }

    @Override
    public void onViewAttachedToWindow(Holder holder) {
        holder.onViewAttachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(Holder holder) {
        holder.onViewDetachedFromWindow();
    }

    @Override
    public void onViewRecycled(Holder holder) {
        holder.onViewRecycled();
    }

    public int getDataCount() {
        return data().size();
    }

    private int type(Class<? extends Holder> clas) {
        Integer type = mHolderTypeMap.get(clas);
        if (type == null) {
            type = mHolderTypeMap.size();
            mHolderTypeMap.put(clas, type);
        }
        if (mHolderConstructors.indexOfKey(type) < 0) {
            mHolderConstructors.put(type, new ConstructorMeta(mHolderInitArgs, clas));
        }
        return type;
    }

    /**
     * {0}{1}{2} -> {3} -> {0}{1}{2}{3}
     */
    public <DataType> RecyclerAdapter add(Class<? extends Holder<DataType>> type, DataType data) {
        data().add(new Meta(type(type), data));
        if (mTransactionItems == null) notifyItemInserted(mItems.size() - 1);
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{1}{2}
     */
    public <DataType> RecyclerAdapter add(int index, Class<? extends Holder<DataType>> type, DataType data) {
        data().add(index, new Meta(type(type), data));
        if (mTransactionItems == null) notifyItemInserted(index);
        return this;
    }

    /**
     * {0}{1}{2} -> {3}{4} -> {0}{1}{2}{3}{4}
     */
    public <DataType> RecyclerAdapter add(Class<? extends Holder<DataType>> type, List<DataType> data) {
        if (data != null && !data.isEmpty()) {
            data().addAll(Meta.list(type(type), data));
            if (mTransactionItems == null) notifyItemRangeInserted(mItems.size() - data.size(), data.size());
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3}{4} -> {0}{3}{4}{1}{2}
     */
    public <DataType> RecyclerAdapter add(int index, @NonNull Class<? extends Holder<DataType>> type, List<DataType> data) {
        if (data != null && !data.isEmpty()) {
            data().addAll(index, Meta.list(type(type), data));
            if (mTransactionItems == null) notifyItemRangeInserted(index, data.size());
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{2}
     */
    public <DataType> RecyclerAdapter change(int index, Class<? extends Holder<DataType>> type, DataType data) {
        data().set(index, new Meta(type(type), data));
        if (mTransactionItems == null) notifyItemChanged(index);
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{2}
     */
    public <DataType> RecyclerAdapter change(int index, Class<? extends Holder<DataType>> type, DataType data, Object payload) {
        data().set(index, new Meta(type(type), data));
        if (mTransactionItems == null) notifyItemChanged(index, payload);
        return this;
    }

    /**
     * {0}{1}{2} -> [1] -> {0}{2}
     */
    public RecyclerAdapter remove(int index) {
        data().remove(index);
        if (mTransactionItems == null) notifyItemRemoved(index);
        return this;
    }

    /**
     * {0}{1}{2}{3}{4} -> [1,3) -> {0}{3}{4}
     */
    public RecyclerAdapter remove(int from, int to) {
        data().subList(from, to).clear();
        if (mTransactionItems == null) notifyItemRangeRemoved(from, to - from);
        return this;
    }

    /**
     * {0}{1}{2} ->  -> Ø
     */
    public RecyclerAdapter clear() {
        return remove(0, data().size());
    }

    /**
     * {0}{1}{2} -> [2][0] -> {2}{0}{1}
     */
    public RecyclerAdapter move(int from, int to) {
        List<Meta> data = data();
        data.add(to, data.remove(from));
        if (mTransactionItems == null) notifyItemMoved(from, to);
        return this;
    }


    public RecyclerAdapter beginTransaction() {
        return beginTransaction(true);
    }

    public RecyclerAdapter beginTransaction(boolean cancelLast) {
        if (cancelLast) {
            cancelTransaction();
        }
        if (mTransactionItems == null) {
            mTransactionItems = new ArrayList<>(mItems);
        }
        return this;
    }

    public RecyclerAdapter cancelTransaction() {
        if (mTransactionAsyncTask != null) {
            mTransactionAsyncTask.cancel(true);
        }
        mTransactionItems = null;
        return this;
    }

    public RecyclerAdapter commitTransaction(DifferenceComparator... comparators) {
        return commitTransaction(true, true, comparators);
    }

    public RecyclerAdapter commitTransaction(boolean detectMoves, final DifferenceComparator... comparators) {
        return commitTransaction(true, detectMoves, comparators);
    }

    public RecyclerAdapter commitTransaction(boolean async, boolean detectMoves, DifferenceComparator... comparators) {
        if (mTransactionItems == null) {
            return this;
        }
        TransactionDiffUtilCallback callback = new TransactionDiffUtilCallback(this, comparators);
        mTransactionItems = null;
        if (async) {
            mTransactionAsyncTask = new TransactionAsyncTask(this, callback, detectMoves).execute();
        } else {
            dispatchUpdatesTransaction(DiffUtil.calculateDiff(callback, detectMoves), callback.getNewItems());
        }
        return this;
    }

    private void dispatchUpdatesTransaction(DiffUtil.DiffResult diffResult, List<Meta> newItems) {
        mItems.clear();
        mItems.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
        mTransactionItems = null;
    }

    static class Meta {
        final int type;
        Object data;

        Meta(int type, Object data) {
            this.type = type;
            this.data = data;
        }

        static List<Meta> list(int type, List data) {
            List<Meta> list = new ArrayList<>(data.size());
            for (Object o : data) {
                list.add(new Meta(type, o));
            }
            return list;
        }
    }

    static class ConstructorMeta {
        final Constructor<? extends Holder> constructor;
        final Object[] args;

        ConstructorMeta(List<Object> outers, Class<? extends Holder> type) {
            for (Constructor<?> constructor : type.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 0) {
                    continue;
                }
                if (parameters[parameters.length - 1] != ViewGroup.class) {
                    continue;
                }
                Object[] args = new Object[parameters.length];
                for (int i = 0, length = parameters.length - 1; i < length; i++) {
                    for (Object outer : outers) {
                        if (parameters[i].isAssignableFrom(outer.getClass())) {
                            args[i] = outer;
                            break;
                        }
                    }
                    if (args[i] == null) {
                        args = null;
                        break;
                    }
                }
                if (args != null) {
                    //noinspection unchecked
                    this.constructor = (Constructor<? extends Holder>) constructor;
                    this.constructor.setAccessible(true);
                    this.args = args;
                    return;
                }
            }
            throw new RuntimeException(String.format(Locale.getDefault(), "unable to find a appropriate constructor from outers:%s", type));
        }

        Holder newInstance(ViewGroup parent) {
            try {
                args[args.length - 1] = parent;
                return constructor.newInstance(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public abstract static class Holder<DataType> extends RecyclerView.ViewHolder {
        protected DataType data;

        /**
         * Your holder's constructor signature should look like this
         * but the super constructor should call others, such as{@link #Holder(ViewGroup, int)} or {@link #Holder(ViewGroup, View)}
         */
        @Deprecated
        public Holder(ViewGroup group) {
            //noinspection ConstantConditions
            super(null);
        }

        public Holder(ViewGroup group, View view) { super(view); }

        public Holder(ViewGroup group, @LayoutRes int layout) { super(LayoutInflater.from(group.getContext()).inflate(layout, group, false)); }

        protected void bindData(int position, DataType data, @NonNull List<Object> payloads) { bindData(position, data, payloads.isEmpty() ? null : payloads.get(0)); }

        protected void bindData(int position, DataType data, @Nullable Object payload) { bindData(position, data); }

        protected void bindData(int position, DataType data) { }

        protected void onViewAttachedToWindow() { }

        protected void onViewDetachedFromWindow() { }

        protected void onViewRecycled() { }
    }

    public static abstract class DifferenceComparator<DataType> {
        final Class<? extends Holder<DataType>> holder;

        public DifferenceComparator(Class<? extends Holder<DataType>> holder) {
            this.holder = holder;
        }

        protected abstract boolean areItemsTheSame(DataType o1, DataType o2);

        protected abstract Object getChangePayload(DataType o1, DataType o2);

        protected boolean areContentsTheSame(DataType o1, DataType o2) {
            return getChangePayload(o1, o2) == null;
        }
    }

    private static class TransactionDiffUtilCallback extends DiffUtil.Callback {

        private final RecyclerAdapter mAdapter;

        private final SparseArray<DifferenceComparator<?>> mComparators;
        private final List<Meta> mOldItems;
        private final List<Meta> mNewItems;

        private DifferenceComparator mComparator;
        private Meta mOldItem, mNewItem;

        public TransactionDiffUtilCallback(RecyclerAdapter adapter, DifferenceComparator... comparators) {
            mAdapter = adapter;
            mComparators = new SparseArray<>(comparators.length);
            for (DifferenceComparator<?> comparator : comparators) {
                mComparators.put(adapter.type(comparator.holder), comparator);
            }
            mOldItems = new ArrayList<>(adapter.mItems);
            mNewItems = new ArrayList<>(adapter.mTransactionItems);
        }

        @Override
        public int getOldListSize() {
            return mOldItems.size();
        }

        @Override
        public int getNewListSize() {
            return mNewItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            mOldItem = mOldItems.get(oldItemPosition);
            mNewItem = mNewItems.get(newItemPosition);
            if (mOldItem.type != mNewItem.type) {
                return false;
            }
            mComparator = mComparators.get(mNewItem.type);
            if (mComparator == null) {
                return Objects.equals(mOldItem.data, mNewItem.data);
            }
            //noinspection unchecked
            return mComparator.areItemsTheSame(mOldItem.data, mNewItem.data);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            mOldItem = mOldItems.get(oldItemPosition);
            mNewItem = mNewItems.get(newItemPosition);
            mComparator = mComparators.get(mNewItem.type);
            if (mComparator == null) {
                return true;
            }
            //noinspection unchecked
            return mComparator.areContentsTheSame(mOldItem.data, mNewItem.data);
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            mOldItem = mOldItems.get(oldItemPosition);
            mNewItem = mNewItems.get(newItemPosition);
            mComparator = mComparators.get(mNewItem.type);
            if (mComparator == null) {
                return null;
            }
            //noinspection unchecked
            return mComparator.getChangePayload(mOldItem.data, mNewItem.data);
        }

        public List<Meta> getNewItems() {
            return mNewItems;
        }
    }

    private static class TransactionAsyncTask extends AsyncTask<Object, Object, DiffUtil.DiffResult> {
        private final RecyclerAdapter mAdapter;
        private final TransactionDiffUtilCallback mCallback;
        private final boolean mDetectMoves;

        public TransactionAsyncTask(RecyclerAdapter adapter, TransactionDiffUtilCallback callback, boolean detectMoves) {
            mAdapter = adapter;
            mCallback = callback;
            mDetectMoves = detectMoves;
        }

        @Override
        protected DiffUtil.DiffResult doInBackground(Object... objects) {
            return DiffUtil.calculateDiff(mCallback, mDetectMoves);
        }

        @Override
        protected void onPostExecute(DiffUtil.DiffResult diffResult) {
            mAdapter.dispatchUpdatesTransaction(diffResult, mCallback.getNewItems());
        }
    }
}