package com.example.indokicksprototype;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_IndoKicks_Splash); // theme khusus splash (di bawah)
        setContentView(R.layout.activity_splash);

        // Tampilkan sebentar lalu lanjut ke MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish(); // agar tidak kembali ke splash saat back
        }, 600); // 0.6 detik (atur sesuka kamu)
    }
}
