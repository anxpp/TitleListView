package com.anxpp.titlelistview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    WrapperViewList wrapperViewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wrapperViewList = (WrapperViewList) findViewById(R.id.list);
        InitialAdapter mAdapter = new InitialAdapter(this);
        getResources().getStringArray(R.array.countries);
        wrapperViewList.setAdapter(mAdapter);
    }
}
