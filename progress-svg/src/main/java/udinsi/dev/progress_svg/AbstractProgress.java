package udinsi.dev.progress_svg;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

/**
 * Shared configuration and lifecycle for the SVG and GIF progress dialogs.
 *
 * <p>The {@link Dialog} is created lazily in {@link #show()} so that construction and
 * {@link #validate()} make no Android runtime calls and can be exercised in plain JVM tests.
 * Config fields are package-private so unit tests in the same package can read them directly.
 */
public abstract class AbstractProgress {

    final Context context;
    Dialog dialog;

    String message = "";
    float textSize = 20.0f;
    int textColor = Color.WHITE;
    int backgroundColor = Color.TRANSPARENT;
    boolean cancelable = true;
    boolean cancelOnTouchOutside = false;

    protected AbstractProgress(Context context) {
        this.context = context;
    }

    /** Validates required config, then builds and shows the dialog. */
    public final void show() {
        validate();
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        applyWindow();
        renderContent();
        dialog.show();
    }

    /** Dismisses the dialog if shown. Safe to call before {@link #show()}. */
    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    public void setCancelOnTouchOutside(boolean cancelOnTouchOutside) {
        this.cancelOnTouchOutside = cancelOnTouchOutside;
    }

    private void applyWindow() {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(backgroundColor));
        }
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(cancelOnTouchOutside);
    }

    /** Applies shared text styling/content to a layout's loading-message TextView. */
    protected void applyMessage(TextView tv) {
        tv.setTextColor(textColor);
        tv.setTextSize(textSize);
        tv.setText(message);
    }

    /** Inflates the subclass layout and binds its content. Called by {@link #show()}. */
    protected abstract void renderContent();

    /** Throws {@link IllegalStateException} if required config is missing. No Android calls. */
    protected abstract void validate();
}
