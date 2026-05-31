package udinsi.dev.progress_svg;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;

import udinsi.dev.progress_svg.databinding.DialogBinding;

public class ProgressGif {

    /*
    set default value
     */
    Context context;
    Dialog dialog;

    int gifResource;
    String message = "";
    private float textSize = 20.0f;
    private int textColor = Color.WHITE;
    private int backgroundColor = android.graphics.Color.TRANSPARENT;
    private boolean cancleable = true;
    private boolean cancleOnTouchOutside = false;

    /*
    constructor for this class, to get current context
    */
    public ProgressGif(Context context){
        this.context = context;
        dialog = new Dialog(context);
    }

    /*
    show dialog to activity, by default the backgroun
     */
    public void show(){
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(backgroundColor));
        dialog.setCancelable(cancleable);
        dialog.setCanceledOnTouchOutside(cancleOnTouchOutside);
        DialogBinding binding = DialogBinding.inflate(dialog.getLayoutInflater());
        dialog.setContentView(binding.getRoot());

        binding.loadingMessage.setTextColor(textColor);
        binding.loadingMessage.setTextSize(textSize);

        binding.loadingMessage.setText(message);
        binding.gifPlayer.setGifFromResource(gifResource);
        dialog.show();
    }

    public void dissmis(){
        dialog.dismiss();
    }

    /*
    set gifplayer, gif must saved on drawable
    gif resource required
     */
    public void setGifResource(int gifResource){
        this.gifResource = gifResource;
    }

    /*
    set message for progress, String required
    */
    public void setMessage(String message){
        this.message = message;
    }

    /*
   change text size, float value required
    */
    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    /*
    change text color, int value required
     */
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    /*
   change background color, int value required
    */
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /*
    set cancleable, boolean value required
     */
    public void setCancleable(boolean cancleable) {
        this.cancleable = cancleable;
    }

    /*
    set cancle on touch outside, boolean value required
     */
    public void setCancleOnTouchOutside(boolean cancleOnTouchOutside) {
        this.cancleOnTouchOutside = cancleOnTouchOutside;
    }
}
