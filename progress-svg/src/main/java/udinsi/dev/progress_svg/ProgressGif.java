package udinsi.dev.progress_svg;

import android.content.Context;

import udinsi.dev.progress_svg.databinding.DialogBinding;

public class ProgressGif extends AbstractProgress {

    /** Drawable resource id of the GIF; 0 until set — used by {@link #validate()}. */
    int gifResource;

    public ProgressGif(Context context) {
        super(context);
    }

    /** Sets the GIF by drawable/raw resource id (the file lives in {@code res/drawable}). */
    public void setGifResource(int gifResource) {
        this.gifResource = gifResource;
    }

    @Override
    protected void validate() {
        if (gifResource == 0) {
            throw new IllegalStateException(
                    "gif resource not set — call setGifResource()/Builder.gif() before show()");
        }
    }

    @Override
    protected void renderContent() {
        DialogBinding binding = DialogBinding.inflate(dialog.getLayoutInflater());
        dialog.setContentView(binding.getRoot());
        binding.gifPlayer.setGifFromResource(gifResource);
        applyMessage(binding.loadingMessage);
    }

    /** Fluent builder for {@link ProgressGif}. Call {@link #build()} then {@code show()}. */
    public static class Builder {
        private final ProgressGif target;

        public Builder(Context context) {
            this.target = new ProgressGif(context);
        }

        public Builder message(String message) {
            target.setMessage(message);
            return this;
        }

        public Builder textSize(float textSize) {
            target.setTextSize(textSize);
            return this;
        }

        public Builder textColor(int textColor) {
            target.setTextColor(textColor);
            return this;
        }

        public Builder backgroundColor(int backgroundColor) {
            target.setBackgroundColor(backgroundColor);
            return this;
        }

        public Builder cancelable(boolean cancelable) {
            target.setCancelable(cancelable);
            return this;
        }

        public Builder cancelOnTouchOutside(boolean cancelOnTouchOutside) {
            target.setCancelOnTouchOutside(cancelOnTouchOutside);
            return this;
        }

        public Builder gif(int resourceId) {
            target.setGifResource(resourceId);
            return this;
        }

        public ProgressGif build() {
            return target;
        }
    }
}
