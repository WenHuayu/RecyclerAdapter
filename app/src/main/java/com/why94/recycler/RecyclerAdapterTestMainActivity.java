package com.why94.recycler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class RecyclerAdapterTestMainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    RecyclerView mRecyclerData;
    RecyclerAdapter mDataAdapter;

    RecyclerView mRecyclerStep;
    RecyclerAdapter mStepAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerData = findViewById(R.id.recycler_data);
        mRecyclerData.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerData.setAdapter(mDataAdapter = new RecyclerAdapter(this));
        mRecyclerStep = findViewById(R.id.recycler_step);
        mRecyclerStep.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerStep.setAdapter(mStepAdapter = new RecyclerAdapter(this));

        ((Switch) findViewById(R.id.transaction)).setOnCheckedChangeListener(this);

        findViewById(R.id.clear_step).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStepAdapter.clear();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, Menu.NONE, "加载更多").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                startActivity(new Intent(this, RecyclerLoadMoreHolderTestActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<Integer> mRealData = new ArrayList<>();
    private Random mRandom = new Random();

    private int number;

    private int number(boolean plus) {
        if (plus) {
            number++;
        }
        return number;
    }

    private int random(int bound) {
        return bound > 0 ? mRandom.nextInt(bound) : 0;
    }

    public void clear(View view) {
        mRealData.clear();
        mDataAdapter.clear();
        mStepAdapter.add(StepHolder.class, new Step("clear"));
    }

    public void remove(View view) {
        int index = random(mRealData.size() - 3);
        int size = 1 + random(3);
        mRealData.subList(index, index + size).clear();
        mDataAdapter.remove(index, index + size);
        mStepAdapter.add(StepHolder.class, new Step("remove", String.format(Locale.CANADA, "%d ~ %d", index, index + size)));
    }

    public void move(View view) {
        int from = random(mRealData.size());
        int to = random(mRealData.size());
        Collections.swap(mRealData, from, to);
        mDataAdapter.move(from, to);
        mStepAdapter.add(StepHolder.class, new Step("move", String.format(Locale.CANADA, "%d > %d", from, to)));
    }

    public void change(View view) {
        int index = random(mRealData.size());
        mRealData.set(index, number(true));
        mDataAdapter.change(index, DataHolder.class, number(false));
        mStepAdapter.add(StepHolder.class, new Step("change", index, number(false)));
    }

    public void add(View view) {
        int index = random(mRealData.size());
        int size = 1 + random(3);
        for (int i = index; i < index + size; i++) {
            mRealData.add(i, number(true));
            mDataAdapter.add(i, DataHolder.class, number(false));
            mStepAdapter.add(StepHolder.class, new Step("add", i, number(false)));
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mDataAdapter.beginTransaction();
            mStepAdapter.add(StepHolder.class, new Step("transaction"));
        } else {
            mDataAdapter.commitTransaction();
            mStepAdapter.add(StepHolder.class, new Step("commit"));
        }
    }

    class DataHolder extends RecyclerAdapter.Holder<Integer> {
        TextView tv;

        public DataHolder(ViewGroup group) {
            super(group, R.layout.item);
            tv = (TextView) itemView;
        }

        @Override
        protected void bindData(int position, Integer data) {
            tv.setText(data.toString());
        }
    }

    class Step {
        final String title;
        final String index;
        final String content;

        public Step(String title) {
            this.title = title;
            this.index = "";
            this.content = "";
        }

        public Step(String title, String content) {
            this.title = title;
            this.index = "";
            this.content = content;
        }

        public Step(String title, int index, int content) {
            this.title = title;
            this.index = String.valueOf(index);
            this.content = String.valueOf(content);
        }
    }

    class StepHolder extends RecyclerAdapter.Holder<Step> {
        TextView title;
        TextView index;
        TextView content;

        public StepHolder(ViewGroup group) {
            super(group, R.layout.steps);
            title = itemView.findViewById(R.id.title);
            index = itemView.findViewById(R.id.index);
            content = itemView.findViewById(R.id.content);
        }

        @Override
        protected void bindData(int position, Step data) {
            title.setText(data.title);
            index.setText(data.index);
            content.setText(data.content);
        }
    }
}