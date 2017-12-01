package com.sander.updateliudemo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class SettingActivity extends BaseActivity {

    private Button first_button;

    private Button second_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        sd_configView();
    }


    private void sd_configView()
    {
        first_button = (Button) findViewById(R.id.first_button);
        second_button = (Button) findViewById(R.id.second_button);

        first_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("sander","这个是主");

                PrefUtil.getInstance().setStreamRoot(true);


                ((LiuApplication )getApplication()).getStream_header().sendEmptyMessage(1);
            }
        });

        second_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("sander","这个是从");

                PrefUtil.getInstance().setStreamRoot(false);
                ((LiuApplication )getApplication()).getStream_header().sendEmptyMessage(2);
            }
        });

    }
}
