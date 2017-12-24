# RecyclerAdapter

Step 1. Add the JitPack repository to your build file

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
  Step 2. Add the dependency
  
  	dependencies {
	        compile 'com.github.WenHuayu:RecyclerAdapter:-SNAPSHOT'
	}

Step 3. set adapter

	mRecyclerView = findViewById(R.id.recycler_data);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter = new RecyclerAdapter(this));
	
Step 4. create your item holder and extends RecyclerAdapter.Holder

	class DataHolder extends RecyclerAdapter.Holder<Integer> {
        TextView tv;

        public DataHolder(ViewGroup group) {
            super(group, R.layout.item);
            tv = (TextView) itemView;
        }

        @Override
        protected void bindData(int position, Integer data) {
            tv.setText(String.valueOf(data));
        }
    }
    
Step 5. add this holder and data into adapter

	mAdapter.add(DataHolder.class, 123);
	
you can add any type of holders
