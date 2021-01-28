package com.cloudtenlabs.ping.activity.signin;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.cloudtenlabs.ping.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        TextView resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(view -> {

        });

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> finish());
    }
}
