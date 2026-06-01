package udinsi.dev.progress_svg;

import android.content.Context;
import android.webkit.WebSettings;

import udinsi.dev.progress_svg.databinding.DialogSvgBinding;

public class ProgressSvg extends AbstractProgress {

    /** Raw asset filename; null until set — used by {@link #validate()}. */
    String svgAssets;
    /** HTML wrapper actually loaded into the WebView. */
    private String svgHtml;

    public ProgressSvg(Context context) {
        super(context);
    }

    /**
     * Sets the SVG asset by filename. The file must live in the app's {@code assets/} folder.
     * The filename is wrapped in a centered HTML {@code <img>} template and rendered via WebView.
     */
    public void setSvgAssets(String filename) {
        this.svgAssets = filename;
        this.svgHtml = "<html>\n" +
                "<head>\n" +
                "<style type=text/css>\n" +
                "body{margin:0 auto;text-align:center;}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<img src=\"" + filename + "\">\n" +
                "</object>\n" +
                "</body>\n" +
                "</html>";
    }

    @Override
    protected void validate() {
        if (svgAssets == null) {
            throw new IllegalStateException(
                    "svg asset not set — call setSvgAssets()/Builder.svg() before show()");
        }
    }

    @Override
    protected void renderContent() {
        DialogSvgBinding binding = DialogSvgBinding.inflate(dialog.getLayoutInflater());
        dialog.setContentView(binding.getRoot());

        binding.webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        binding.webView.setBackgroundColor(backgroundColor);
        binding.webView.loadDataWithBaseURL("file:///android_asset/", svgHtml,
                "text/html", "utf-8", null);

        applyMessage(binding.loadingMessage);
    }

    /** Fluent builder for {@link ProgressSvg}. Call {@link #build()} then {@code show()}. */
    public static class Builder {
        private final ProgressSvg target;

        public Builder(Context context) {
            this.target = new ProgressSvg(context);
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

        public Builder svg(String filename) {
            target.setSvgAssets(filename);
            return this;
        }

        public ProgressSvg build() {
            return target;
        }
    }
}
