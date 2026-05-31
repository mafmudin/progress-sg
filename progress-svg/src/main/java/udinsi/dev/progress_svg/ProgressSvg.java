package udinsi.dev.progress_svg;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.webkit.WebSettings;

import udinsi.dev.progress_svg.databinding.DialogSvgBinding;

public class ProgressSvg {
    /*
    set default value
     */
    private Context context;
    private Dialog dialog;
    private String svgAssets;
    private String message = "";
    private float textSize = 20.0f;
    private int textColor = Color.WHITE;
    private int backgroundColor = android.graphics.Color.TRANSPARENT;
    private boolean cancleable = true;
    private boolean cancleOnTouchOutside = false;

    /*
    constructor for this class, to get current context
     */
    public ProgressSvg(Context context){
        this.context = context;
        dialog = new Dialog(this.context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    /*
    show dialog to activity, by default the backgroun
     */
    public void show(){
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(backgroundColor));
        dialog.setCancelable(cancleable);
        dialog.setCanceledOnTouchOutside(cancleOnTouchOutside);

        DialogSvgBinding binding = DialogSvgBinding.inflate(dialog.getLayoutInflater());
        dialog.setContentView(binding.getRoot());

        binding.webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        binding.webView.setBackgroundColor(backgroundColor);
        binding.webView.loadDataWithBaseURL("file:///android_asset/", svgAssets,
                "text/html", "utf-8", null);

        binding.loadingMessage.setTextSize(textSize);
        binding.loadingMessage.setTextColor(textColor);

        binding.loadingMessage.setText(message);

        dialog.show();
    }

    public void dissmis(){
        dialog.dismiss();
    }

    /*
    set message for progress, String required
     */
    public void setMessage(String message){
        this.message = message;
    }

    /*
    set svg with webview, svg file must placed in assets file.
    need svg file name to load to webview
     */
    public void setSvgAssets(String svgAssets) {
        this.svgAssets = "<html>\n" +
                "<head>\n" +
                "<style type=text/css>\n" +
                "body{margin:0 auto;text-align:center;}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<img src=\""+svgAssets+"\">\n" +
                "</object>\n" +
                "</body>\n" +
                "</html>";
    }

    /*
    change text size, float value required
     */
    public void setTextSize(float textSize){
        this.textSize = textSize;
    }

    /*
    change text color, int value required
     */
    public void setTextColor(int textColor){
        this.textColor = textColor;
    }

    /*
    change background color, int value required
     */
    public void setBackgroundColor(int backgroundColor){
        this.backgroundColor = backgroundColor;
    }

    /*
    set cancleable, boolean value required
     */
    public void setCancleable(boolean cancleable){
        this.cancleable = cancleable;
    }

    /*
    set cancle on touch outside, boolean value required
     */
    public void setCancleOnTouchOutside(boolean cancleOnTouchOutside) {
        this.cancleOnTouchOutside = cancleOnTouchOutside;
    }
}
