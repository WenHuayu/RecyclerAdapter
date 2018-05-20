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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个通用的RecyclerView适配器
 * 用户可向内添加任意的Holder类型与数据
 * 适配器将自动管理Holder的实例化以及局部刷新
 * Created by WenHuayu<why94@qq.com> on 2017/11/27.
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "unused"})
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.Holder> {

    /**
     * 实例化Holder备用的参数列表
     * 需要注意的是,如果Holder是非静态内部类,必须提供它的外部类实例,否则会因缺少参数而导致失败
     * Holder可拥有任意数量构造方法,自动创建Holder时,会选择构造参数与备用参数匹配的第一个来实例化对象
     */
    @NonNull
    private final ArrayList<Object> mHolderInstantiationArgs;
    /**
     * 缓存构造Holder的数据,第一次查找成功后,下次使用的时候就可以直接使用了
     */
    @NonNull
    private final SparseArray<QuickConstructor> mHolderConstructors;
    /**
     * Holder与ItemViewType的映射表,使用HashMap可以更快返回Holder的ItemViewType
     */
    @NonNull
    private final HashMap<Class<? extends Holder>, Integer> mHolder2ItemViewTypeMap;
    /**
     * 适配器真正向外部提供的数据源
     */
    @NonNull
    private ArrayList<Meta> mAdapterData = new ArrayList<>();
    /**
     * 适配器进入事务模式后真正操作的数据源,但是数据并不会向外部提供
     * 当事务提交后,会直接将事务数据替换至适配器数据,并将事务数据设置为null
     */
    @Nullable
    private ArrayList<Meta> mTransactionData;
    /**
     * Change和Remove操作默认的快速返回设置,你依然可以调用具体的方法单独设置
     * 当适配器中的数据与Holder一对一时,开启快速返回可以在匹配上第一个数据后就跳出方法,节省匹配时间
     * 当适配器中的数据与Holder一对多时,若不关闭快速返回,则仅修改匹配上的第一个数据,可能留下隐藏BUG
     */
    private boolean mDefaultQuickReturn = true;

    /**
     * @see RecyclerAdapter#RecyclerAdapter(Object, int)
     */
    public RecyclerAdapter() {
        this(null, 1);
    }

    /**
     * @see RecyclerAdapter#RecyclerAdapter(Object, int)
     */
    public RecyclerAdapter(@Nullable Object initialHolderInstantiationArg) {
        this(initialHolderInstantiationArg, 1);
    }

    /**
     * @param initialHolderInstantiationArg {@link RecyclerAdapter#mHolderInstantiationArgs}
     * @param initialHolderTypeCount        Holder的类型数量,适当的设置大小可减少扩容的开销
     */
    public RecyclerAdapter(@Nullable Object initialHolderInstantiationArg, int initialHolderTypeCount) {
        if (initialHolderInstantiationArg == null) {
            mHolderInstantiationArgs = new ArrayList<>();
        } else {
            mHolderInstantiationArgs = new ArrayList<>(1);
            addHolderInstantiationArg(initialHolderInstantiationArg);
        }
        mHolderConstructors = new SparseArray<>(initialHolderTypeCount);
        mHolder2ItemViewTypeMap = new HashMap<>(initialHolderTypeCount);
    }

    /**
     * 添加实例化Holder的备用构造参数
     *
     * @see RecyclerAdapter#mHolderInstantiationArgs
     */
    public RecyclerAdapter addHolderInstantiationArg(Object holderInstantiationArg) {
        if (holderInstantiationArg.getClass().isAssignableFrom(ViewGroup.class)) {
            throw new IllegalArgumentException("已存在类型为" + holderInstantiationArg.getClass() + "的Holder构造参数");
        }
        for (Object instantiationArg : mHolderInstantiationArgs) {
            if (holderInstantiationArg.getClass().isAssignableFrom(holderInstantiationArg.getClass())) {
                throw new IllegalArgumentException("已存在类型为" + holderInstantiationArg.getClass() + "的Holder构造参数");
            }
        }
        mHolderInstantiationArgs.add(holderInstantiationArg);
        return this;
    }

    /**
     * @see RecyclerAdapter#mDefaultQuickReturn
     */
    public RecyclerAdapter setDefaultQuickReturn(boolean defaultQuickReturn) {
        mDefaultQuickReturn = defaultQuickReturn;
        return this;
    }

    /**
     * @return 若适配器处于事务状态, 则返回事务数据, 若适配器不处于事务状态, 则返回适配器数据
     */
    private List<Meta> data() {
        return notInTransaction() ? mAdapterData : mTransactionData;
    }

    /**
     * @return 是否处于事务模式.
     */
    public boolean isInTransaction() {
        return mTransactionData != null;
    }

    /**
     * @return 是否不处于事务模式
     */
    private boolean notInTransaction() {
        return mTransactionData == null;
    }

    /**
     * @return 适配器数据是否为空
     */
    public boolean isEmpty() {
        return mAdapterData.isEmpty();
    }

    /**
     * @return 事务数据或适配器数据是否为空
     */
    public boolean isDataEmpty() {
        return data().isEmpty();
    }

    /**
     * @return 适配器数据数量
     */
    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    /**
     * @return 事务数据或适配器数据数量
     */
    public int getDataCount() {
        return data().size();
    }

    /**
     * @return 适配器数据指定下标处数据的类型
     */
    @Override
    public int getItemViewType(int position) {
        return mAdapterData.get(position).type;
    }

    /**
     * @return Holder对应的数据类型
     */
    public int getItemViewType(@NonNull Class<? extends Holder> holder) {
        Integer type = mHolder2ItemViewTypeMap.get(holder);
        if (type == null) {
            type = mHolder2ItemViewTypeMap.size();
            mHolder2ItemViewTypeMap.put(holder, type);
        }
        return type;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        QuickConstructor constructor = mHolderConstructors.get(type);
        if (constructor == null) {
            for (Map.Entry<Class<? extends Holder>, Integer> entry : mHolder2ItemViewTypeMap.entrySet()) {
                if (entry.getValue() == type) {
                    constructor = new QuickConstructor(entry.getKey(), mHolderInstantiationArgs);
                    mHolderConstructors.put(type, constructor);
                    break;
                }
            }
        }
        assert constructor != null;
        return constructor.newInstance(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        holder.bindData(position, holder.data = mAdapterData.get(position).data, payloads);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull Holder holder) {
        holder.onViewAttachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull Holder holder) {
        holder.onViewDetachedFromWindow();
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        holder.onViewRecycled();
    }

    /**
     * [0,1,2] -> add(?,3) -> [0,1,2,3]
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> add(String,"3") -> [meta(int,0),meta(int,1),meta(int,2),meta(String,"3")]
     */
    public <T> RecyclerAdapter add(Class<? extends Holder<T>> holder, T data) {
        data().add(Meta.single(getItemViewType(holder), data));
        if (notInTransaction()) {
            notifyItemInserted(mAdapterData.size() - 1);
        }
        return this;
    }

    /**
     * [0,1,2] -> add(1,?,3) -> [0,3,1,2]
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> add(1,String,"3") -> [meta(int,0),meta(String,"3"),meta(int,1),meta(int,2)]
     */
    public <T> RecyclerAdapter add(int index, Class<? extends Holder<T>> holder, T data) {
        data().add(index, Meta.single(getItemViewType(holder), data));
        if (notInTransaction()) {
            notifyItemInserted(index);
        }
        return this;
    }

    /**
     * [0,1,2] -> add(?,[3,4]) -> [0,1,2,3,4]
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> add(String,["3","4"]) -> [meta(int,0),meta(int,1),meta(int,2),meta(String,"3"),meta(String,"4")]
     */
    public <T> RecyclerAdapter add(Class<? extends Holder<T>> holder, List<T> data) {
        if (data != null && !data.isEmpty()) {
            data().addAll(Meta.list(getItemViewType(holder), data));
            if (notInTransaction()) {
                notifyItemRangeInserted(mAdapterData.size() - data.size(), data.size());
            }
        }
        return this;
    }

    /**
     * [0,1,2] -> add(1,?,[3,4]) -> [0,3,4,1,2]
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> add(1,String,["3","4"]) -> [meta(int,0),meta(String,"3"),meta(String,"4"),meta(int,1),meta(int,2)]
     */
    public <T> RecyclerAdapter add(int index, @NonNull Class<? extends Holder<T>> holder, List<T> data) {
        if (data != null && !data.isEmpty()) {
            data().addAll(index, Meta.list(getItemViewType(holder), data));
            if (notInTransaction()) {
                notifyItemRangeInserted(index, data.size());
            }
        }
        return this;
    }

    /**
     * [0,1,2] -> change(1,?,3) -> [0,3,2]
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> change(1,String,3) -> [meta(int,0),meta(String,3),meta(int,2)]
     */
    public <T> RecyclerAdapter change(int index, Class<? extends Holder<T>> holder, T data, Object payload) {
        data().set(index, Meta.single(getItemViewType(holder), data));
        if (notInTransaction()) {
            notifyItemChanged(index, payload);
        }
        return this;
    }

    /**
     * @see RecyclerAdapter#change(int, Class, Object, Object)
     */
    public <T> RecyclerAdapter change(int index, Class<? extends Holder<T>> holder, T data) {
        return change(index, holder, data, null);
    }

    public RecyclerAdapter change(int index, Object payload) {
        if (notInTransaction()) {
            notifyItemChanged(index, payload);
        }
        return this;
    }

    public RecyclerAdapter change(int index) {
        return change(index, null);
    }

    /**
     * define obj(key,valueA) == obj(key,valueB)
     * <p>
     * [meta(obj,obj(0,0)),meta(obj,obj(1,1)),meta(obj,obj(2,2)] -> change(?,obj(1,11),?,?) -> [meta(obj,obj(0,0)),meta(obj,obj(1,11)),meta(obj,obj(2,2)]
     */
    public <T> RecyclerAdapter change(@Nullable Class<? extends Holder<T>> holder, T data, @Nullable Object payload, boolean quickReturn) {
        List<Meta> metas = data();
        int type = holder == null ? -1 : getItemViewType(holder);
        for (int i = 0, size = metas.size(); i < size; i++) {
            Meta meta = metas.get(i);
            if ((holder == null || type == meta.type) && RecyclerAdapter.equals(meta.data, data)) {
                metas.set(i, meta);
                if (notInTransaction()) {
                    notifyItemChanged(i, payload);
                }
                if (quickReturn) {
                    break;
                }
            }
        }
        return this;
    }

    /**
     * @see RecyclerAdapter#change(Class, Object, Object, boolean)
     */
    public <T> RecyclerAdapter change(Class<? extends Holder<T>> holder, T data, boolean quickReturn) {
        return change(holder, data, null, quickReturn);
    }

    /**
     * @see RecyclerAdapter#change(Class, Object, Object, boolean)
     */
    public <T> RecyclerAdapter change(Class<? extends Holder<T>> holder, T data, Object payload) {
        return change(holder, data, payload, mDefaultQuickReturn);
    }

    /**
     * @see RecyclerAdapter#change(Class, Object, Object, boolean)
     */
    public <T> RecyclerAdapter change(Class<? extends Holder<T>> holder, T data) {
        return change(holder, data, null, mDefaultQuickReturn);
    }

    /**
     * @see RecyclerAdapter#change(Class, Object, Object, boolean)
     */
    public <T> RecyclerAdapter change(T data, Object payload, boolean quickReturn) {
        return change(null, data, payload, quickReturn);
    }

    /**
     * @see RecyclerAdapter#change(Class, Object, Object, boolean)
     */
    public <T> RecyclerAdapter change(T data, Object payload) {
        return change(null, data, payload, mDefaultQuickReturn);
    }

    /**
     * @see RecyclerAdapter#change(Class, Object, Object, boolean)
     */
    public <T> RecyclerAdapter change(T data, boolean quickReturn) {
        return change(null, data, null, quickReturn);
    }

    /**
     * @see RecyclerAdapter#change(Class, Object, Object, boolean)
     */
    public <T> RecyclerAdapter change(T data) {
        return change(null, data, null, mDefaultQuickReturn);
    }

    /**
     * [meta(int,0),meta(int,1),meta(int,2)] -> change(11,comparator(int,1)) -> [meta(int,0),meta(int,11),meta(int,2)]
     *
     * @param quickReturn 是否在匹配上一个数据后就结束方法,如果同一个数据在适配器中有多个Holder,需设置此参数为false,否则只会立即更新第一个Holder
     */
    public <T> RecyclerAdapter change(T data, DifferenceComparator<T> comparator, boolean quickReturn) {
        List<Meta> metas = data();
        int itemViewType = getItemViewType(comparator.holder);
        for (int i = 0, size = metas.size(); i < size; i++) {
            Meta meta = metas.get(i);
            if (itemViewType == metas.get(i).type) {
                //noinspection unchecked
                T datum = (T) metas.get(i).data;
                if (comparator.areItemsTheSame(datum, data)) {
                    metas.set(i, Meta.single(itemViewType, data));
                    if (notInTransaction() && !comparator.areContentsTheSame(datum, data)) {
                        Object payload = comparator.getChangePayload(datum, data);
                        notifyItemChanged(i, payload);
                    }
                    if (quickReturn) {
                        break;
                    }
                }
            }
        }
        return this;
    }

    /**
     * @see RecyclerAdapter#change(Object, DifferenceComparator, boolean)
     */
    public <T> RecyclerAdapter change(T data, DifferenceComparator<T> comparator) {
        return change(data, comparator, mDefaultQuickReturn);
    }

    /**
     * [0,1,2] -> remove(1) -> [0,2]
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> remove(1) -> [meta(int,0),meta(int,2)]
     */
    public RecyclerAdapter remove(int index) {
        data().remove(index);
        if (notInTransaction()) {
            notifyItemRemoved(index);
        }
        return this;
    }

    /**
     * [0,1,2] -> remove(0,2) -> []
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> remove(0,2) -> []
     */
    public RecyclerAdapter remove(int from, int to) {
        if (from != to) {
            data().subList(from, to).clear();
            if (notInTransaction()) {
                notifyItemRangeRemoved(from, to - from);
            }
        }
        return this;
    }

    public <T> RecyclerAdapter remove(@Nullable Class<? extends Holder<T>> holder, T data, boolean quickReturn) {
        int type = holder == null ? -1 : getItemViewType(holder);
        List<Meta> metas = data();
        for (int i = 0, remove = 0, size = metas.size(); i + remove < size; ) {
            Meta meta = metas.get(i);
            if ((holder == null || type == meta.type) && RecyclerAdapter.equals(meta.data, data)) {
                metas.remove(i);
                if (notInTransaction()) {
                    notifyItemRemoved(i);
                }
                if (quickReturn) {
                    break;
                }
                remove++;
            } else {
                i++;
            }
        }
        return this;
    }

    /**
     * @see RecyclerAdapter#remove(Class, Object, boolean)
     */
    public <T> RecyclerAdapter remove(Class<? extends Holder<T>> holder, T data) {
        return remove(holder, data, mDefaultQuickReturn);
    }

    /**
     * @see RecyclerAdapter#remove(Class, Object, boolean)
     */
    public <T> RecyclerAdapter remove(T data, boolean quickReturn) {
        return remove(null, data, quickReturn);
    }

    /**
     * @see RecyclerAdapter#remove(Class, Object, boolean)
     */
    public <T> RecyclerAdapter remove(T data) {
        return remove(null, data, mDefaultQuickReturn);
    }

    /**
     * {0:0}{1:1}{2:2} -> {1:11} -> {0:0}{1:11}{2:2}
     */
    public <T> RecyclerAdapter remove(T data, DifferenceComparator<T> comparator) {
        return remove(data, comparator, mDefaultQuickReturn);
    }

    public <T> RecyclerAdapter remove(T data, DifferenceComparator<T> comparator, boolean quickReturn) {
        List<Meta> metas = data();
        int type = getItemViewType(comparator.holder);
        for (int i = 0, size = metas.size(); i < size; i++) {
            Meta meta = metas.get(i);
            //noinspection unchecked
            if (type == metas.get(i).type && comparator.areItemsTheSame((T) metas.get(i).data, data)) {
                metas.remove(i);
                if (notInTransaction()) {
                    notifyItemRemoved(i);
                }
                if (quickReturn) {
                    break;
                }
            }
        }
        return this;
    }

    /**
     * [0,1,2] -> clear -> []
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> move(0,2) -> []
     */
    public RecyclerAdapter clear() {
        return remove(0, data().size());
    }

    /**
     * [0,1,2] -> move(0,2) -> [2,1,0]
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> move(0,2) -> [meta(int,2),meta(int,1),meta(int,0)]
     */
    public RecyclerAdapter move(int from, int to) {
        List<Meta> metas = data();
        metas.add(to, metas.remove(from));
        if (notInTransaction()) {
            notifyItemMoved(from, to);
        }
        return this;
    }

    /**
     * 通知所有数据项检查payload并根据需要更新
     */
    public RecyclerAdapter notifyDataSetChanged(int payload) {
        notifyItemRangeChanged(0, getItemCount(), payload);
        return this;
    }

    /**
     * @see RecyclerAdapter#beginTransaction(boolean)
     */
    public RecyclerAdapter beginTransaction() {
        return beginTransaction(true);
    }

    /**
     * 适配器进入事务模式,所有数据更改项均不会立即影响适配器的向外提供数据源,只有在提交事务后,才会统一更新数据
     *
     * @param cancelExistingTransaction 如果适配器正处于事务模式中,是否取消现有事务数据,如果不取消,则会在现有事务的基础上叠加改动
     */
    public RecyclerAdapter beginTransaction(boolean cancelExistingTransaction) {
        if (cancelExistingTransaction || mTransactionData == null) {
            mTransactionData = new ArrayList<>(mAdapterData);
        }
        return this;
    }

    /**
     * 适配器退出事务模式,丢弃所有事务改动
     */
    public RecyclerAdapter cancelTransaction() {
        mTransactionData = null;
        return this;
    }

    /**
     * @see RecyclerAdapter#commitTransaction(boolean, DifferenceComparator[])
     */
    public RecyclerAdapter commitTransaction(DifferenceComparator... comparators) {
        return commitTransaction(true, comparators);
    }

    /**
     * 适配器退出事务模式,并应用事务改动
     */
    public RecyclerAdapter commitTransaction(boolean detectMoves, DifferenceComparator... differenceComparators) {
        if (mTransactionData != null) {
            final SparseArray<DifferenceComparator<?>> comparators = new SparseArray<>(differenceComparators.length);
            for (DifferenceComparator<?> comparator : differenceComparators) {
                comparators.put(getItemViewType(comparator.holder), comparator);
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
                    mComparator = comparators.get(mNewItem.type);
                    if (mComparator == null) {
                        return RecyclerAdapter.equals(mOldItem.data, mNewItem.data);
                    }
                    //noinspection unchecked
                    return mComparator.areItemsTheSame(mOldItem.data, mNewItem.data);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    mOldItem = oldItems.get(oldItemPosition);
                    mNewItem = newItems.get(newItemPosition);
                    mComparator = comparators.get(mNewItem.type);
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
                    mComparator = comparators.get(mNewItem.type);
                    if (mComparator == null) {
                        return null;
                    }
                    //noinspection unchecked
                    return mComparator.getChangePayload(mOldItem.data, mNewItem.data);
                }
            }, detectMoves).dispatchUpdatesTo(this);
        }
        return this;
    }

    private static class Meta {
        final int type;
        final Object data;

        private Meta(int type, Object data) {
            this.type = type;
            this.data = data;
        }

        static Meta single(int type, Object data) {
            return new Meta(type, data);
        }

        static List<Meta> list(int type, List data) {
            List<Meta> list = new ArrayList<>(data.size());
            for (Object o : data) {
                list.add(new Meta(type, o));
            }
            return list;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || (obj instanceof Meta && ((Meta) obj).type == type && RecyclerAdapter.equals(((Meta) obj).data, data));
        }
    }

    /**
     * 快速实例化Holder的帮助类
     */
    private static class QuickConstructor {
        final Constructor<? extends Holder> constructor;
        final Class<? extends Holder> holder;
        final int parentIndex;
        final Object[] args;

        QuickConstructor(Class<? extends Holder> holder, List<Object> candidates) {
            //寻找第一个能与候选参数匹配的构造方法
            for (Constructor<?> constructor : holder.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                //保存已匹配上的参数
                Object[] matchArgs = new Object[parameters.length];
                //保存ViewGroup类型参数的下标
                int matchParentIndex = -1;
                List<Object> tempCandidates = new ArrayList<>(candidates);
                //依次检查构造方法的各个参数是否能在候选参数列表中寻找到相应类型的候选参数
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].isAssignableFrom(ViewGroup.class)) {
                        //有超过1个的ViewGroup以及其父类型参数,则导致失败
                        if (matchParentIndex == -1) {
                            matchArgs[i] = ViewGroup.class;
                            matchParentIndex = i;
                        } else {
                            matchArgs[i] = null;
                        }
                    } else {
                        for (Object tempCandidate : tempCandidates) {
                            if (parameters[i].isAssignableFrom(tempCandidate.getClass())) {
                                //有超过1个的候选参数以及其父类型参数,则导致失败
                                tempCandidates.remove(tempCandidate);
                                matchArgs[i] = tempCandidate;
                                break;
                            }
                        }
                    }
                    if (matchArgs[i] == null) {
                        matchArgs = null;
                        break;
                    }
                }
                if (matchArgs != null) {
                    //noinspection unchecked
                    this.constructor = (Constructor<? extends Holder>) constructor;
                    this.constructor.setAccessible(true);
                    this.parentIndex = matchParentIndex;
                    this.args = matchArgs;
                    this.holder = holder;
                    return;
                }
            }
            StringBuilder builder = new StringBuilder();
            builder.append("\n无法从").append(holder).append("的构造方法中寻找到能匹配已有构造参数的构造方法");
            builder.append("\n可选构造方法:");
            for (Constructor<?> constructor : holder.getConstructors()) {
                builder.append("\n").append(holder.getSimpleName()).append("(");
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                for (Class<?> parameterType : parameterTypes) {
                    builder.append(parameterType).append(", ");
                }
                if (parameterTypes.length > 0) {
                    builder.delete(builder.length() - 2, builder.length());
                }
                builder.append(")");
            }
            builder.append("\n已有构造参数:");
            builder.append("\n").append(ViewGroup.class).append(":").append("适配器自动提供");
            for (Object candidate : candidates) {
                builder.append("\n").append(candidate.getClass()).append(":").append(candidate);
            }
            throw new IllegalArgumentException(builder.toString());
        }

        Holder newInstance(ViewGroup parent) {
            if (parentIndex != -1) {
                args[parentIndex] = parent;
            }
            Holder holder = null;
            try {
                holder = constructor.newInstance(args);
            } catch (InstantiationException e) {
                throw new RuntimeException("无法实例化:" + this.holder, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法实例化:" + this.holder, e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("无法实例化:" + this.holder, e);
            } finally {
                if (parentIndex != -1) {
                    args[parentIndex] = null;
                }
            }
            return holder;
        }
    }

    public abstract static class Holder<T> extends RecyclerView.ViewHolder {
        protected T data;

        /**
         * 这个构造方法仅用来作IDE的快捷重写用的,不可在子类中调用
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

        /**
         * @see RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int, List)
         */
        protected void bindData(int position, T data, @NonNull List<Object> payloads) {
            bindData(position, data, payloads.isEmpty() ? null : payloads.get(0));
        }

        /**
         * @see RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int, List)
         */
        protected void bindData(int position, T data, @Nullable Object payload) {
            bindData(position, data);
        }

        /**
         * @see RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int, List)
         */
        protected void bindData(int position, T data) {
        }

        /**
         * @see RecyclerView.Adapter#onViewAttachedToWindow(RecyclerView.ViewHolder)
         */
        protected void onViewAttachedToWindow() {
        }

        /**
         * @see RecyclerView.Adapter#onViewDetachedFromWindow(RecyclerView.ViewHolder)
         */
        protected void onViewDetachedFromWindow() {
        }

        /**
         * @see RecyclerView.Adapter#onViewRecycled(RecyclerView.ViewHolder)
         */
        protected void onViewRecycled() {
        }
    }

    /**
     * @see DiffUtil.Callback
     */
    public static abstract class DifferenceComparator<T> {
        private final Class<? extends Holder<T>> holder;

        public DifferenceComparator(Class<? extends Holder<T>> holder) {
            this.holder = holder;
        }

        /**
         * @see DiffUtil.Callback#areItemsTheSame(int, int)
         */
        protected boolean areItemsTheSame(T oldItem, T newItem) {
            return RecyclerAdapter.equals(oldItem, newItem);
        }

        /**
         * @see DiffUtil.Callback#areContentsTheSame(int, int)
         */
        protected boolean areContentsTheSame(T oldItem, T newItem) {
            return getChangePayload(oldItem, newItem) == null;
        }

        /**
         * @see DiffUtil.Callback#getChangePayload(int, int)
         */
        protected Object getChangePayload(T oldItem, T newItem) {
            return null;
        }
    }

    private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}