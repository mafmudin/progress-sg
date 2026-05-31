package udinsi.dev.progress_svg;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity{
    Button loading, searching, login, update, sending, upload, searchingGif;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loading = findViewById(R.id.loading);
        searching = findViewById(R.id.searching);
        login = findViewById(R.id.login);
        update = findViewById(R.id.update);
        sending = findViewById(R.id.sending);
        upload = findViewById(R.id.upload);
        searchingGif = findViewById(R.id.searchingGif);

        loading.setOnClickListener(listener);
        searching.setOnClickListener(listener);
        login.setOnClickListener(listener);
        update.setOnClickListener(listener);
        sending.setOnClickListener(listener);
        upload.setOnClickListener(listener);
        searchingGif.setOnClickListener(listener);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            ProgressSvg progressSvg = new ProgressSvg(MainActivity.this);
            switch (id){
                case R.id.loading:
                    progressSvg.setSvgAssets("loading_circle.svg");
                    progressSvg.setMessage(getResources().getString(R.string.please_wait));
                    progressSvg.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
                    progressSvg.setTextSize(11.0f);
                    progressSvg.setBackgroundColor(Color.GRAY);
                    progressSvg.show();
                    break;
                case R.id.searching:
                    progressSvg.setSvgAssets("loading_mag.svg");
                    progressSvg.setMessage(getResources().getString(R.string.searching));
                    progressSvg.setBackgroundColor(Color.GREEN);
                    progressSvg.show();
                    break;
                case R.id.login:
                    progressSvg.setSvgAssets("login.svg");
                    progressSvg.setMessage(getResources().getString(R.string.login));
                    progressSvg.show();
                    break;
                case R.id.update:
                    progressSvg.setSvgAssets("reload.svg");
                    progressSvg.setMessage(getResources().getString(R.string.update));
                    progressSvg.show();
                    break;
                case R.id.sending:
                    progressSvg.setSvgAssets("sending.svg");
                    progressSvg.setMessage(getResources().getString(R.string.sending));
                    progressSvg.show();
                    break;
                case R.id.upload:
                    progressSvg.setSvgAssets("upload.svg");
                    progressSvg.setMessage(getResources().getString(R.string.upload));
                    progressSvg.show();
                    break;
                case R.id.searchingGif:
                    ProgressGif progressGif = new ProgressGif(MainActivity.this);
                    progressGif.setGifResource(R.drawable.mag);
                    progressGif.setMessage(getResources().getString(R.string.searching));
                    progressGif.show();
                    break;
            }
        }
    };
}
