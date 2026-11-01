package org.telegram.ui.Components;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Pinch the message list to change the text size of this one chat.
 *
 * Telegram for Android integration: ChatActivity forwards chatListView touches to
 * {@link #onTouchEvent(MotionEvent)} first; {@link Listener#onScale(float)} sets a per-dialog
 * font size override used by MessageObject.generateLayout (instead of the global
 * SharedConfig.fontSize), invalidates text layouts and keeps the message under the fingers
 * anchored. The value is stored per dialog, so other chats keep their size.
 */
public class ChatTextScaleController {

    public interface Listener {
        /** Called continuously while pinching; focusY is where the fingers are. */
        void onScale(float scale, float focusY);

        void onScaleEnd(float scale);
    }

    public static final float MIN = 0.8f, MAX = 1.8f;
    private static final float SNAP = 0.05f;

    private final ScaleGestureDetector detector;
    private final SharedPreferences prefs;
    private final String key;
    private final Listener listener;
    private final View hapticView;
    private float scale;
    private boolean scaling, gestureWasScale, snapped;

    public ChatTextScaleController(Context context, long dialogId, View hapticView, Listener listener) {
        this.prefs = context.getSharedPreferences("chat_text_scale", Context.MODE_PRIVATE);
        this.key = "s_" + dialogId;
        this.listener = listener;
        this.hapticView = hapticView;
        scale = prefs.getFloat(key, 1f);
        detector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector d) {
                scaling = true;
                gestureWasScale = true;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float raw = Math.max(MIN, Math.min(MAX, scale * d.getScaleFactor()));
                boolean near = Math.abs(raw - 1f) < SNAP;
                if (near && !snapped) {
                    hapticView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
                snapped = near;
                scale = raw;
                ChatTextScaleController.this.listener.onScale(near ? 1f : scale, d.getFocusY());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector d) {
                scaling = false;
                if (snapped) {
                    scale = 1f;
                }
                prefs.edit().putFloat(key, scale).apply();
                ChatTextScaleController.this.listener.onScaleEnd(scale);
            }
        });
        detector.setQuickScaleEnabled(false);
    }

    public float getScale() {
        return scale;
    }

    public void reset() {
        scale = 1f;
        prefs.edit().remove(key).apply();
        listener.onScale(1f, -1);
        listener.onScaleEnd(1f);
    }

    /** Returns true while the gesture belongs to the pinch, so the list must not scroll. */
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            gestureWasScale = false;
        }
        detector.onTouchEvent(e);
        return scaling || gestureWasScale || e.getPointerCount() > 1;
    }
}
