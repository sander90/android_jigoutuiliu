package com.sander.updateliudemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static void startActivity(Activity targetActivity, Class tClass){
        Intent intent = new Intent(targetActivity, tClass);
        targetActivity.startActivity(intent);
    }
}
