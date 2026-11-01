package org.telegram.lab.pinchzoom;

import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.lab.LabActivity;
import org.telegram.lab.R;
import org.telegram.lab.Tg;
import org.telegram.lab.TouchDriver;
import org.telegram.lab.ui.ChatHeaderView;
import org.telegram.lab.ui.ChatScreen;
import org.telegram.lab.ui.MessageCell;
import org.telegram.lab.ui.WallpaperDrawable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.ChatTextScaleController;

import java.util.Locale;

public class MainActivity extends LabActivity implements ChatTextScaleController.Listener {

    private static final float BASE_SP = 16;
    private ChatScreen chat;
    private ChatTextScaleController scaler;
    private LinearLayout indicator;
    private TextView percent, reset;
    private final Runnable hideIndicator = () -> indicator.animate().alpha(0).translationY(-AndroidUtilities.dp(8)).setDuration(220).start();
    private View anchor;
    private float anchorOffset;
    private MessageCell recipe;

    @Override
    protected void onCreateScreen(FrameLayout content) {
        chat = new ChatScreen(this, WallpaperDrawable.GREEN);
        content.addView(chat);
        chat.header.setTitle("Grandma Rose").setSubtitle("last seen recently", false).setAvatar("Grandma Rose");
        chat.header.addAction(R.drawable.tg_ic_call);
        chat.header.addAction(R.drawable.tg_ic_ab_other);

        chat.addDate("Today");
        in("Good morning sweetheart ☀️");
        out("Morning Grandma! Could you send me your apple pie recipe? I want to bake it for Sunday 🥧");
        in("Of course! Here it is, exactly how I make it:");
        recipe = in("For the dough: 2½ cups of flour, 1 cup of cold butter cut into cubes, a pinch of salt and 6 tablespoons of ice water. Mix with your fingers until it looks like crumbs, then press into a ball and keep it in the fridge for an hour.");
        in("For the filling: 6 apples, peeled and sliced thin, ¾ cup of sugar, 1 teaspoon of cinnamon, a little nutmeg and 2 tablespoons of lemon juice.");
        in("Bake at 200 °C for 15 minutes, then lower to 180 °C for another 40. The top should be golden ✨");
        out("Thank you!! ❤️");
        in("Call me when it's ready, I want to hear how it went 😊");
        chat.scrollToBottom();

        scaler = new ChatTextScaleController(this, 42L, chat, this);
        chat.touchHook = scaler::onTouchEvent;

        indicator = new LinearLayout(this);
        indicator.setGravity(Gravity.CENTER_VERTICAL);
        indicator.setBackground(ChatHeaderView.glass(AndroidUtilities.dp(20)));
        indicator.setElevation(AndroidUtilities.dp(4));
        indicator.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(8), 0);
        TextView aa = Tg.text(this, 15, Tg.textGray2, true);
        aa.setText("Aa");
        indicator.addView(aa);
        percent = Tg.text(this, 15, Tg.text, true);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-2, -2);
        pl.leftMargin = AndroidUtilities.dp(10);
        indicator.addView(percent, pl);
        reset = Tg.text(this, 14, Tg.accent, true);
        reset.setText("Reset");
        reset.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        reset.setOnClickListener(v -> scaler.reset());
        indicator.addView(reset);
        FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(-2, AndroidUtilities.dp(40), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        il.topMargin = chat.header.barHeight() + AndroidUtilities.dp(8);
        chat.overlay.addView(indicator, il);
        indicator.setAlpha(0);
        applyScale(scaler.getScale());
    }

    private MessageCell in(String text) {
        return chat.add(new MessageCell(this, false).setText(text).setTime("9:1" + chat.list.getChildCount() % 10));
    }

    private void out(String text) {
        chat.add(new MessageCell(this, true).setText(text).setTime("9:1" + chat.list.getChildCount() % 10).setStatus(MessageCell.STATUS_READ));
    }

    private void applyScale(float s) {
        for (int i = 0; i < chat.list.getChildCount(); i++) {
            View v = chat.list.getChildAt(i);
            if (v instanceof MessageCell) {
                ((MessageCell) v).setTextSizeSp(BASE_SP * s);
            }
        }
        percent.setText(String.format(Locale.US, "%d%%", Math.round(s * 100)));
        reset.setVisibility(Math.abs(s - 1f) < 0.01f ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onScale(float scale, float focusY) {
        if (focusY >= 0) {
            findAnchor(focusY);
        }
        applyScale(scale);
        keepAnchor();
        AndroidUtilities.cancelRunOnUIThread(hideIndicator);
        indicator.animate().cancel();
        indicator.setAlpha(1);
        indicator.setTranslationY(0);
    }

    @Override
    public void onScaleEnd(float scale) {
        applyScale(scale);
        AndroidUtilities.runOnUIThread(hideIndicator, 1400);
    }

    /** The message under the fingers stays where it is while the text grows around it. */
    private void findAnchor(float focusY) {
        float contentY = focusY - chat.scroll.getPaddingTop() + chat.scroll.getScrollY();
        anchor = null;
        for (int i = 0; i < chat.list.getChildCount(); i++) {
            View v = chat.list.getChildAt(i);
            if (v.getTop() <= contentY && v.getBottom() >= contentY) {
                anchor = v;
                float f = (contentY - v.getTop()) / Math.max(1, v.getHeight());
                anchorOffset = focusY;
                anchor.setTag(f);
                break;
            }
        }
    }

    private void keepAnchor() {
        if (anchor == null) {
            return;
        }
        View a = anchor;
        float frac = (float) a.getTag();
        float focus = anchorOffset;
        chat.list.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                chat.list.getViewTreeObserver().removeOnPreDrawListener(this);
                int y = (int) (a.getTop() + a.getHeight() * frac + chat.scroll.getPaddingTop() - focus);
                chat.scroll.scrollTo(0, Math.max(0, y));
                return true;
            }
        });
    }

    @Override
    protected void onAutoplay(TouchDriver d) {
        d.wait(900)
                .zoom(1.35f, () -> recipe, 900)
                .wait(500)
                .pinch(() -> recipe, AndroidUtilities.dp(90), AndroidUtilities.dp(230), 1600)
                .wait(1600)
                .resetZoom(800)
                .scroll(AndroidUtilities.dp(260), 900)
                .wait(900)
                .scroll(-AndroidUtilities.dp(380), 900)
                .wait(900)
                .zoom(1.35f, () -> recipe, 800)
                .pinch(() -> recipe, AndroidUtilities.dp(220), AndroidUtilities.dp(140), 1400)
                .wait(1500)
                .resetZoom(800)
                .wait(1200);
    }
}
