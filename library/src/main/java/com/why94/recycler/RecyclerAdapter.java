package com.why94.recycler;

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

/**
 * RecyclerAdapter
 * Created by WenHuayu<why94@qq.com> on 2017/11/27.
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "SameParameterValue", "unused"})
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.Holder> {

    private final ArrayList<Object> mHolderInitArgs;
    private final SparseArray<ConstructorMeta> mHolderConstructors;
    private final HashMap<Class, Integer> mHolderTypeMap;
    private ArrayList<Meta> mAdapterData = new ArrayList<>();
    private ArrayList<Meta> mTransactionData;

    public RecyclerAdapter(Object... holderInitialArgs) {
        this(1, holderInitialArgs);
    }

    public RecyclerAdapter(int typeInitialCapacity, Object... holderInitialArgs) {
        mHolderInitArgs = new ArrayList<>(holderInitialArgs.length);
        if (holderInitialArgs.length == 1) {
            mHolderInitArgs.add(holderInitialArgs[0]);
        } else {
            mHolderInitArgs.addAll(Arrays.asList(holderInitialArgs));
        }
        mHolderConstructors = new SparseArray<>(typeInitialCapacity);
        mHolderTypeMap = new HashMap<>(typeInitialCapacity);
    }

    public void addHolderInitArgs(Object... holderInitialArgs) {
        if (holderInitialArgs.length == 1) {
            mHolderInitArgs.add(holderInitialArgs[0]);
        } else {
            mHolderInitArgs.addAll(Arrays.asList(holderInitialArgs));
        }
    }

    private List<Meta> data() {
        return mTransactionData == null ? mAdapterData : mTransactionData;
    }

    public boolean isEmpty() {
        return mAdapterData.isEmpty();
    }

    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mAdapterData.get(position).type;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int type) {
        return mHolderConstructors.get(type).newInstance(parent);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(Holder holder, int position, List<Object> payloads) {
        holder.bindData(position, holder.data = mAdapterData.get(position).data, payloads);
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
        if (mTransactionData == null) {
            mAdapterData.add(new Meta(type(type), data));
            notifyItemInserted(mAdapterData.size() - 1);
        } else {
            mTransactionData.add(new Meta(type(type), data));
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{1}{2}
     */
    public <DataType> RecyclerAdapter add(int index, Class<? extends Holder<DataType>> type, DataType data) {
        if (mTransactionData == null) {
            mAdapterData.add(index, new Meta(type(type), data));
            notifyItemInserted(index);
        } else {
            mTransactionData.add(index, new Meta(type(type), data));
        }
        return this;
    }

    /**
     * {0}{1}{2} -> {3}{4} -> {0}{1}{2}{3}{4}
     */
    public <DataType> RecyclerAdapter add(Class<? extends Holder<DataType>> type, List<DataType> data) {
        if (data != null && !data.isEmpty()) {
            if (mTransactionData == null) {
                mAdapterData.addAll(Meta.list(type(type), data));
                notifyItemRangeInserted(mAdapterData.size() - data.size(), data.size());
            } else {
                mTransactionData.addAll(Meta.list(type(type), data));
            }
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3}{4} -> {0}{3}{4}{1}{2}
     */
    public <DataType> RecyclerAdapter add(int index, @NonNull Class<? extends Holder<DataType>> type, List<DataType> data) {
        if (data != null && !data.isEmpty()) {
            if (mTransactionData == null) {
                mAdapterData.addAll(index, Meta.list(type(type), data));
                notifyItemRangeInserted(index, data.size());
            } else {
                mTransactionData.addAll(index, Meta.list(type(type), data));
            }
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{2}
     */
    public <DataType> RecyclerAdapter change(int index, Class<? extends Holder<DataType>> type, DataType data) {
        if (mTransactionData == null) {
            mAdapterData.set(index, new Meta(type(type), data));
            notifyItemChanged(index);
        } else {
            mTransactionData.set(index, new Meta(type(type), data));
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1]:{3} -> {0}{3}{2}
     */
    public <DataType> RecyclerAdapter change(int index, Class<? extends Holder<DataType>> type, DataType data, Object payload) {
        if (mTransactionData == null) {
            mAdapterData.set(index, new Meta(type(type), data));
            notifyItemChanged(index, payload);
        } else {
            mTransactionData.set(index, new Meta(type(type), data));
        }
        return this;
    }

    /**
     * {0:0}{1:1}{2:2} -> {1:11} -> {0:0}{1:11}{2:2}
     */
    public <DataType> RecyclerAdapter change(DataType data, DifferenceComparator<DataType> comparator) {
        List<Meta> metas = mTransactionData == null ? mAdapterData : mTransactionData;
        int type = type(comparator.holder);
        for (int i = 0, size = metas.size(); i < size; i++) {
            Meta meta = metas.get(i);
            if (type == metas.get(i).type) {
                //noinspection unchecked
                DataType d = (DataType) metas.get(i).data;
                if (comparator.areItemsTheSame(d, data)) {
                    if (mTransactionData != null) {
                        metas.set(i, new Meta(type, data));
                    } else if (!comparator.areContentsTheSame(d, data)) {
                        Object payload = comparator.getChangePayload(d, data);
                        metas.set(i, new Meta(type, data));
                        notifyItemChanged(i, payload);
                    }
                    return this;
                }
            }
        }
        return this;
    }

    /**
     * {0}{1}{2} -> [1] -> {0}{2}
     */
    public RecyclerAdapter remove(int index) {
        if (mTransactionData == null) {
            mAdapterData.remove(index);
            notifyItemRemoved(index);
        } else {
            mTransactionData.remove(index);
        }
        return this;
    }

    /**
     * {0}{1}{2}{3}{4} -> [1,3) -> {0}{3}{4}
     */
    public RecyclerAdapter remove(int from, int to) {
        if (from != to) {
            if (mTransactionData == null) {
                mAdapterData.subList(from, to).clear();
                notifyItemRangeRemoved(from, to - from);
            } else {
                mTransactionData.subList(from, to).clear();
            }
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
        if (mTransactionData == null) {
            mAdapterData.add(to, mAdapterData.remove(from));
            notifyItemMoved(from, to);
        } else {
            mTransactionData.add(to, mTransactionData.remove(from));
        }
        return this;
    }

    public RecyclerAdapter beginTransaction() {
        return beginTransaction(true);
    }

    public RecyclerAdapter beginTransaction(boolean cancelLast) {
        if (cancelLast || mTransactionData == null) {
            mTransactionData = new ArrayList<>(mAdapterData);
        }
        return this;
    }

    public RecyclerAdapter cancelTransaction() {
        mTransactionData = null;
        return this;
    }

    public RecyclerAdapter commitTransaction(DifferenceComparator... comparators) {
        return commitTransaction(true, comparators);
    }

    public RecyclerAdapter commitTransaction(boolean detectMoves, DifferenceComparator... comparators) {
        if (mTransactionData == null) {
            return this;
        }
        final SparseArray<DifferenceComparator<?>> comparatorsSparseArray = new SparseArray<>(comparators.length);
        for (DifferenceComparator<?> comparator : comparators) {
            comparatorsSparseArray.put(type(comparator.holder), comparator);
        }
        final List<Meta> oldItems = mAdapterData;
        final List<Meta> newItems = mTransactionData;
        mAdapterData = mTransactionData;
        mTransactionData = null;
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            private DifferenceComparator mComparator;
            private Meta mOldItem, mNewItem;

            @Override
            public int getOldListSize() {
                return oldItems.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                mOldItem = oldItems.get(oldItemPosition);
                mNewItem = newItems.get(newItemPosition);
                if (mOldItem.type != mNewItem.type) {
                    return false;
                }
                mComparator = comparatorsSparseArray.get(mNewItem.type);
                if (mComparator == null) {
                    return mOldItem.data == mNewItem.data || (mOldItem.data != null && mOldItem.data.equals(mNewItem.data));
                }
                //noinspection unchecked
                return mComparator.areItemsTheSame(mOldItem.data, mNewItem.data);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                mOldItem = oldItems.get(oldItemPosition);
                mNewItem = newItems.get(newItemPosition);
                mComparator = comparatorsSparseArray.get(mNewItem.type);
                if (mComparator == null) {
                    return true;
                }
                //noinspection unchecked
                return mComparator.areContentsTheSame(mOldItem.data, mNewItem.data);
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                mOldItem = oldItems.get(oldItemPosition);
                mNewItem = newItems.get(newItemPosition);
                mComparator = comparatorsSparseArray.get(mNewItem.type);
                if (mComparator == null) {
                    return null;
                }
                //noinspection unchecked
                return mComparator.getChangePayload(mOldItem.data, mNewItem.data);
            }
        }, detectMoves).dispatchUpdatesTo(this);
        return this;
    }

    private static class Meta {
        final int type;
        final Object data;

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

    private static class ConstructorMeta {
        final Constructor<? extends Holder> constructor;
        final Object[] args;

        ConstructorMeta(List<Object> args, Class<? extends Holder> type) {
            for (Constructor<?> constructor : type.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 0) {
                    continue;
                }
                if (parameters[parameters.length - 1] != ViewGroup.class) {
                    continue;
                }
                Object[] temp = new Object[parameters.length];
                for (int i = 0, length = parameters.length - 1; i < length; i++) {
                    for (Object arg : args) {
                        if (parameters[i].isAssignableFrom(arg.getClass())) {
                            temp[i] = arg;
                            break;
                        }
                    }
                    if (temp[i] == null) {
                        temp = null;
                        break;
                    }
                }
                if (temp != null) {
                    //noinspection unchecked
                    this.constructor = (Constructor<? extends Holder>) constructor;
                    this.constructor.setAccessible(true);
                    this.args = temp;
                    return;
                }
            }
            throw new RuntimeException(String.format(Locale.getDefault(), "unable to find a appropriate constructor by args:%s", type));
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
        @SuppressWarnings("ConstantConditions")
        public Holder(ViewGroup group) {
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

        protected void onViewAttachedToWindow() {
        }

        protected void onViewDetachedFromWindow() {
        }

        protected void onViewRecycled() {
        }
    }

    public static abstract class DifferenceComparator<DataType> {
        final Class<? extends Holder<DataType>> holder;

        public DifferenceComparator(Class<? extends Holder<DataType>> holder) {
            this.holder = holder;
        }

        protected abstract boolean areItemsTheSame(DataType oldItem, DataType newItem);

        protected boolean areContentsTheSame(DataType oldItem, DataType newItem) {
            return getChangePayload(oldItem, newItem) == null;
        }

        protected Object getChangePayload(DataType oldItem, DataType newItem) {
            return null;
        }
    }
}