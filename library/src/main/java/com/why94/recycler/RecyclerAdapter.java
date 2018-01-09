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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerAdapter
 * Created by WenHuayu<why94@qq.com> on 2017/11/27.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue", "WeakerAccess", "unused"})
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.Holder> {

    private final List<Meta> items = new ArrayList<>();
    private final List<Object> holderInitArgs = new ArrayList<>();
    private final SparseArray<ConstructorMeta> holderConstructors = new SparseArray<>();

    public RecyclerAdapter(Object... holderInitArgs) {
        this.holderInitArgs.addAll(Arrays.asList(holderInitArgs));
    }

    public void addHolderInitArgs(Object... initArgs) {
        this.holderInitArgs.addAll(Arrays.asList(initArgs));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int type) {
        return holderConstructors.get(type).newInstance(parent);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) { }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(Holder holder, int position, List<Object> payloads) {
        holder.bindData(position, holder.data = items.get(position).data, payloads);
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

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getDataCount() {
        return data().size();
    }

    private int type(Class<? extends Holder> type) {
        int hash = type.hashCode();
        if (holderConstructors.indexOfKey(hash) < 0) {
            holderConstructors.put(hash, new ConstructorMeta(holderInitArgs, type));
        }
        return hash;
    }

    /**
     * {0}{1}{2} -> {3} -> {0}{1}{2}{3}
     */
    public <DataType> RecyclerAdapter add(Class<? extends Holder<DataType>> type, DataType data) {
        data().add(new Meta(type(type), data));
        if (notInTransaction()) {
            notifyItemInserted(items.size() - 1);
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{1}{2}
     */
    public <DataType> RecyclerAdapter add(int index, Class<? extends Holder<DataType>> type, DataType data) {
        data().add(index, new Meta(type(type), data));
        if (notInTransaction()) {
            notifyItemInserted(index);
        }
        return this;
    }

    /**
     * {0}{1}{2} -> {3}{4} -> {0}{1}{2}{3}{4}
     */
    public <DataType> RecyclerAdapter add(Class<? extends Holder<DataType>> type, List<DataType> data) {
        if (data != null && !data.isEmpty()) {
            data().addAll(Meta.list(type(type), data));
            if (notInTransaction()) {
                notifyItemRangeInserted(items.size() - data.size(), data.size());
            }
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3}{4} -> {0}{3}{4}{1}{2}
     */
    public <DataType> RecyclerAdapter add(int index, @NonNull Class<? extends Holder<DataType>> type, List<DataType> data) {
        if (data != null && !data.isEmpty()) {
            data().addAll(index, Meta.list(type(type), data));
            if (notInTransaction()) {
                notifyItemRangeInserted(index, data.size());
            }
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{2}
     */
    public <DataType> RecyclerAdapter change(int index, Class<? extends Holder<DataType>> type, DataType data) {
        data().set(index, new Meta(type(type), data));
        if (notInTransaction()) {
            notifyItemChanged(index);
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{2}
     */
    public <DataType> RecyclerAdapter change(int index, Class<? extends Holder<DataType>> type, DataType data, Object payload) {
        data().set(index, new Meta(type(type), data));
        if (notInTransaction()) {
            notifyItemChanged(index, payload);
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1] -> {0}{2}
     */
    public RecyclerAdapter remove(int index) {
        data().remove(index);
        if (notInTransaction()) {
            notifyItemRemoved(index);
        }
        return this;
    }

    /**
     * {0}{1}{2}{3}{4} -> [1,3) -> {0}{3}{4}
     */
    public RecyclerAdapter remove(int from, int to) {
        data().subList(from, to).clear();
        if (notInTransaction()) {
            notifyItemRangeRemoved(from, to - from);
        }
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
        if (notInTransaction()) {
            notifyItemMoved(from, to);
        }
        return this;
    }

    private List<Meta> transaction;

    List<Meta> data() {
        return transaction == null ? items : transaction;
    }

    private boolean notInTransaction() {
        return transaction == null;
    }

    public RecyclerAdapter beginTransaction() {
        if (transaction == null) {
            transaction = new ArrayList<>(items);
        }
        return this;
    }

    public RecyclerAdapter cancelTransaction() {
        transaction = null;
        return this;
    }

    public RecyclerAdapter commitTransaction(DifferenceComparator... comparators) {
        return commitTransaction(false, true, comparators);
    }

    public RecyclerAdapter commitTransaction(boolean detectMoves, final DifferenceComparator... comparators) {
        return commitTransaction(false, detectMoves, comparators);
    }

    public RecyclerAdapter commitTransaction(boolean async, boolean detectMoves, DifferenceComparator... comparators) {
        if (transaction == null) {
            return this;
        }
        SparseArray<DifferenceComparator<?>> comparatorSparseArray = comparators2SparseArray(comparators);
        if (async) {
            new TransactionAsyncTask(this, detectMoves, comparatorSparseArray).execute();
        } else {
            List<Meta> oldItems = new ArrayList<>(items);
            List<Meta> newItems = new ArrayList<>(transaction);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TransactionDiffUtilCallback(this, oldItems, newItems, comparatorSparseArray), detectMoves);
            items.clear();
            items.addAll(transaction);
            diffResult.dispatchUpdatesTo(this);
            transaction = null;
        }
        return this;
    }

    private SparseArray<DifferenceComparator<?>> comparators2SparseArray(DifferenceComparator[] comparators) {
        SparseArray<DifferenceComparator<?>> comparatorSparseArray = new SparseArray<>(comparators.length);
        for (DifferenceComparator<?> comparator : comparators) {
            comparatorSparseArray.put(type(comparator.holder), comparator);
        }
        return comparatorSparseArray;
    }

    private static class TransactionAsyncTask extends AsyncTask<Object, Object, DiffUtil.DiffResult> {

        private final RecyclerAdapter mAdapter;
        private final boolean mDetectMoves;
        private final SparseArray<DifferenceComparator<?>> mComparators;
        private final List<Meta> mOldItems;
        private final List<Meta> mNewItems;

        public TransactionAsyncTask(RecyclerAdapter adapter, boolean detectMoves, SparseArray<DifferenceComparator<?>> comparators) {
            mAdapter = adapter;
            mDetectMoves = detectMoves;
            mComparators = comparators;
            mOldItems = new ArrayList<>(adapter.items);
            mNewItems = new ArrayList<>(adapter.transaction);
            adapter.transaction.clear();
        }

        @Override
        protected DiffUtil.DiffResult doInBackground(Object... objects) {
            return DiffUtil.calculateDiff(new TransactionDiffUtilCallback(mAdapter, mOldItems, mNewItems, mComparators), mDetectMoves);
        }

        @Override
        protected void onPostExecute(DiffUtil.DiffResult diffResult) {
            mAdapter.items.clear();
            mAdapter.items.addAll(mNewItems);
            diffResult.dispatchUpdatesTo(mAdapter);
            if (mAdapter.transaction.isEmpty()) {
                mAdapter.transaction = null;
            } else {
                new TransactionAsyncTask(mAdapter, mDetectMoves, mComparators).execute();
            }
        }
    }

    private static class TransactionDiffUtilCallback extends DiffUtil.Callback {

        private final RecyclerAdapter mAdapter;
        private final List<Meta> mOldItems;
        private final List<Meta> mNewItems;
        private final SparseArray<DifferenceComparator<?>> mComparators;

        private DifferenceComparator comparator;
        private Meta oldItem, newItem;

        public TransactionDiffUtilCallback(RecyclerAdapter adapter, List<Meta> oldItems, List<Meta> newItems, SparseArray<DifferenceComparator<?>> comparators) {
            mAdapter = adapter;
            mOldItems = oldItems;
            mNewItems = newItems;
            mComparators = comparators;
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
            oldItem = mOldItems.get(oldItemPosition);
            newItem = mNewItems.get(newItemPosition);
            if (oldItem.type != newItem.type) {
                return false;
            }
            comparator = mComparators.get(newItem.type);
            if (comparator == null) {
                return Objects.equals(oldItem.data, newItem.data);
            }
            //noinspection unchecked
            return comparator.areItemsTheSame(oldItem.data, newItem.data);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            oldItem = mOldItems.get(oldItemPosition);
            newItem = mNewItems.get(newItemPosition);
            comparator = mComparators.get(newItem.type);
            if (comparator == null) {
                return true;
            }
            //noinspection unchecked
            return comparator.areContentsTheSame(oldItem.data, newItem.data);
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            oldItem = mOldItems.get(oldItemPosition);
            newItem = mNewItems.get(newItemPosition);
            comparator = mComparators.get(newItem.type);
            if (comparator == null) {
                return null;
            }
            //noinspection unchecked
            return comparator.getChangePayload(oldItem.data, newItem.data);
        }
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

        public Holder(ViewGroup group, View view) {
            super(view);
        }

        public Holder(ViewGroup group, @LayoutRes int layout) {
            super(LayoutInflater.from(group.getContext()).inflate(layout, group, false));
        }

        protected void bindData(int position, DataType data, @NonNull List<Object> payloads) {
            bindData(position, data, payloads.isEmpty() ? null : payloads.get(0));
        }

        protected void bindData(int position, DataType data, @Nullable Object payload) {
            bindData(position, data);
        }

        protected void bindData(int position, DataType data) {
        }

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
}