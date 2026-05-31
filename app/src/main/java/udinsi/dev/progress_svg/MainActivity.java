package udinsi.dev.progress_svg;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import udinsi.dev.progress_svg.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity{
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loading.setOnClickListener(listener);
        binding.searching.setOnClickListener(listener);
        binding.login.setOnClickListener(listener);
        binding.update.setOnClickListener(listener);
        binding.sending.setOnClickListener(listener);
        binding.upload.setOnClickListener(listener);
        binding.searchingGif.setOnClickListener(listener);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            ProgressSvg progressSvg = new ProgressSvg(MainActivity.this);
            // AGP 8.x defaults android.nonFinalResIds=true, so R.id.* are non-final and can't be
            // used as switch case labels (which require constant expressions). Use if/else instead.
            if (id == R.id.loading) {
                progressSvg.setSvgAssets("loading_circle.svg");
                progressSvg.setMessage(getResources().getString(R.string.please_wait));
                progressSvg.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                progressSvg.setTextSize(11.0f);
                progressSvg.setBackgroundColor(Color.GRAY);
                progressSvg.show();
            } else if (id == R.id.searching) {
                progressSvg.setSvgAssets("loading_mag.svg");
                progressSvg.setMessage(getResources().getString(R.string.searching));
                progressSvg.setBackgroundColor(Color.GREEN);
                progressSvg.show();
            } else if (id == R.id.login) {
                progressSvg.setSvgAssets("login.svg");
                progressSvg.setMessage(getResources().getString(R.string.login));
                progressSvg.show();
            } else if (id == R.id.update) {
                progressSvg.setSvgAssets("reload.svg");
                progressSvg.setMessage(getResources().getString(R.string.update));
                progressSvg.show();
            } else if (id == R.id.sending) {
                progressSvg.setSvgAssets("sending.svg");
                progressSvg.setMessage(getResources().getString(R.string.sending));
                progressSvg.show();
            } else if (id == R.id.upload) {
                progressSvg.setSvgAssets("upload.svg");
                progressSvg.setMessage(getResources().getString(R.string.upload));
                progressSvg.show();
            } else if (id == R.id.searchingGif) {
                ProgressGif progressGif = new ProgressGif(MainActivity.this);
                progressGif.setGifResource(R.drawable.mag);
                progressGif.setMessage(getResources().getString(R.string.searching));
                progressGif.show();
            }
        }
    };
}
