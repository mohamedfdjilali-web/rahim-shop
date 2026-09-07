package com.rahimshop.admin;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);

        textView.setText("Rahim Shop\n\nالتطبيق يعمل بنجاح ✅");
        textView.setTextSize(24);
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.CENTER);

        setContentView(textView);
    }
}
