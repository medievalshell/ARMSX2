package kr.co.iefriends.pcsx2.core.input;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;

import kr.co.iefriends.pcsx2.NativeApp;
import kr.co.iefriends.pcsx2.R;
import kr.co.iefriends.pcsx2.activities.MainActivity;

public class InputManager {
    private boolean hatUp, hatDown, hatLeft, hatRight;
    private final SparseIntArray analogStates = new SparseIntArray();

    private static final float ANALOG_DEADZONE = 0.08f;
    private static final float TRIGGER_DEADZONE = 0.04f;
    public static volatile int sLastControllerDeviceId = -1;

    private static WeakReference<MainActivity> sInstanceRef = new WeakReference<>(null);
    private final MainActivity mActivity;
    public static volatile boolean sVibrationEnabled = true;
    public InputManager(MainActivity activity) {
        this.mActivity = activity;
        sInstanceRef = new WeakReference<>(activity);
    }
    public static void updateLastControllerDeviceId(int deviceId) {
        if (deviceId >= 0) {
            sLastControllerDeviceId = deviceId;
        }
    }
    public void handleGamepadMotion(MotionEvent e) {
        updateLastControllerDeviceId(e.getDeviceId());
        float lx = getCenteredAxis(e, MotionEvent.AXIS_X);
        float ly = getCenteredAxis(e, MotionEvent.AXIS_Y);
        sendAnalog(111, Math.max(0f, lx));
        sendAnalog(113, Math.max(0f, -lx));
        sendAnalog(112, Math.max(0f, ly));
        sendAnalog(110, Math.max(0f, -ly));

        float rx = getCenteredAxis(e, MotionEvent.AXIS_RX);
        float ry = getCenteredAxis(e, MotionEvent.AXIS_RY);
        if (rx == 0f && ry == 0f) {
            rx = getCenteredAxis(e, MotionEvent.AXIS_Z);
            ry = getCenteredAxis(e, MotionEvent.AXIS_RZ);
        }
        sendAnalog(121, Math.max(0f, rx));
        sendAnalog(123, Math.max(0f, -rx));
        sendAnalog(122, Math.max(0f, ry));
        sendAnalog(120, Math.max(0f, -ry));

        float ltrig = e.getAxisValue(MotionEvent.AXIS_LTRIGGER);
        float rtrig = e.getAxisValue(MotionEvent.AXIS_RTRIGGER);
        if (ltrig == 0f) ltrig = e.getAxisValue(MotionEvent.AXIS_BRAKE);
        if (rtrig == 0f) rtrig = e.getAxisValue(MotionEvent.AXIS_GAS);
        sendAnalog(KeyEvent.KEYCODE_BUTTON_L2, normalizeTrigger(ltrig), TRIGGER_DEADZONE);
        sendAnalog(KeyEvent.KEYCODE_BUTTON_R2, normalizeTrigger(rtrig), TRIGGER_DEADZONE);

        float hatX = e.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = e.getAxisValue(MotionEvent.AXIS_HAT_Y);
        final float hatThreshold = 0.4f;
        boolean nowLeft = hatX < -hatThreshold;
        boolean nowRight = hatX > hatThreshold;
        boolean nowUp = hatY < -hatThreshold;
        boolean nowDown = hatY > hatThreshold;
        setAxisState(hatLeft, nowLeft, KeyEvent.KEYCODE_DPAD_LEFT);  hatLeft = nowLeft;
        setAxisState(hatRight, nowRight, KeyEvent.KEYCODE_DPAD_RIGHT); hatRight = nowRight;
        setAxisState(hatUp, nowUp, KeyEvent.KEYCODE_DPAD_UP); hatUp = nowUp;
        setAxisState(hatDown, nowDown, KeyEvent.KEYCODE_DPAD_DOWN); hatDown = nowDown;
    }
    public void makeButtonTouch() {
        boolean isNether = MainActivity.STYLE_NETHER.equals(mActivity.currentOnScreenUiStyle);
        kr.co.iefriends.pcsx2.core.input.view.PSButtonView btn_pad_select = mActivity.findViewById(R.id.btn_pad_select);
        if (btn_pad_select != null) {
            mActivity.applyButtonIcon(btn_pad_select, R.drawable.playstation3_button_select, "ic_controller_select_button.png");
            btn_pad_select.setRectangular(true);
            float selectScale = isNether ? 0.75f : 1.0f;
            btn_pad_select.setScaleX(selectScale);
            btn_pad_select.setScaleY(selectScale);
            btn_pad_select.setOnPSButtonListener(pressed -> NativeApp.setPadButton(KeyEvent.KEYCODE_BUTTON_SELECT, 0, pressed));
        }

        kr.co.iefriends.pcsx2.core.input.view.PSButtonView btn_pad_start = mActivity.findViewById(R.id.btn_pad_start);
        if (btn_pad_start != null) {
            mActivity.applyButtonIcon(btn_pad_start, R.drawable.playstation3_button_start, "ic_controller_start_button.png");
            float selectScale = isNether ? 0.75f : 1.0f;
            btn_pad_start.setScaleX(selectScale);
            btn_pad_start.setScaleY(selectScale);
            btn_pad_start.setOnPSButtonListener(pressed -> NativeApp.setPadButton(KeyEvent.KEYCODE_BUTTON_START, 0, pressed));
        }

        float faceScale = isNether ? 0.9f : 1.0f;

        kr.co.iefriends.pcsx2.core.input.view.PSButtonView btn_pad_a = mActivity.findViewById(R.id.btn_pad_a);
        if (btn_pad_a != null) {
            mActivity.applyButtonIcon(btn_pad_a, R.drawable.playstation_button_color_cross_outline, "ic_controller_cross_button.png");
            btn_pad_a.setScaleX(faceScale);
            btn_pad_a.setScaleY(faceScale);
            btn_pad_a.setOnPSButtonListener(pressed -> {
                int action = pressed ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
                mActivity.sendKeyAction(btn_pad_a, action, KeyEvent.KEYCODE_BUTTON_A);
            });
        }

        kr.co.iefriends.pcsx2.core.input.view.PSButtonView btn_pad_b = mActivity.findViewById(R.id.btn_pad_b);
        if (btn_pad_b != null) {
            mActivity.applyButtonIcon(btn_pad_b, R.drawable.playstation_button_color_circle_outline, "ic_controller_circle_button.png");
            btn_pad_b.setScaleX(faceScale);
            btn_pad_b.setScaleY(faceScale);
            btn_pad_b.setOnPSButtonListener(pressed -> {
                int action = pressed ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
                mActivity.sendKeyAction(btn_pad_b, action, KeyEvent.KEYCODE_BUTTON_B);
            });
        }

        kr.co.iefriends.pcsx2.core.input.view.PSButtonView btn_pad_x = mActivity.findViewById(R.id.btn_pad_x);
        if (btn_pad_x != null) {
            mActivity.applyButtonIcon(btn_pad_x, R.drawable.playstation_button_color_square_outline, "ic_controller_square_button.png");
            btn_pad_x.setScaleX(faceScale);
            btn_pad_x.setScaleY(faceScale);
            btn_pad_x.setOnPSButtonListener(pressed -> {
                int action = pressed ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
                mActivity.sendKeyAction(btn_pad_x, action, KeyEvent.KEYCODE_BUTTON_X);
            });
        }

        kr.co.iefriends.pcsx2.core.input.view.PSButtonView btn_pad_y = mActivity.findViewById(R.id.btn_pad_y);
        if (btn_pad_y != null) {
            mActivity.applyButtonIcon(btn_pad_y, R.drawable.playstation_button_color_triangle_outline, "ic_controller_triangle_button.png");
            btn_pad_y.setScaleX(faceScale);
            btn_pad_y.setScaleY(faceScale);
            btn_pad_y.setOnPSButtonListener(pressed -> {
                int action = pressed ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
                mActivity.sendKeyAction(btn_pad_y, action, KeyEvent.KEYCODE_BUTTON_Y);
            });
        }

        kr.co.iefriends.pcsx2.core.input.view.PSShoulderButtonView btn_pad_l1 = mActivity.findViewById(R.id.btn_pad_l1);
        if (btn_pad_l1 != null) {
            mActivity.applyShoulderIcon(btn_pad_l1, R.drawable.playstation_trigger_l1_alternative_outline, "ic_controller_l1_button.png");
            btn_pad_l1.setScaleX(1.0f);
            btn_pad_l1.setScaleY(isNether ? 0.6f : 1.0f);
            btn_pad_l1.setOnPSShoulderButtonListener(pressed -> NativeApp.setPadButton(KeyEvent.KEYCODE_BUTTON_L1, 0, pressed));
        }

        kr.co.iefriends.pcsx2.core.input.view.PSShoulderButtonView btn_pad_r1 = mActivity.findViewById(R.id.btn_pad_r1);
        if (btn_pad_r1 != null) {
            mActivity.applyShoulderIcon(btn_pad_r1, R.drawable.playstation_trigger_r1_alternative_outline, "ic_controller_r1_button.png");
            btn_pad_r1.setScaleX(1.0f);
            btn_pad_r1.setScaleY(isNether ? 0.6f : 1.0f);
            btn_pad_r1.setOnPSShoulderButtonListener(pressed -> NativeApp.setPadButton(KeyEvent.KEYCODE_BUTTON_R1, 0, pressed));
        }

        kr.co.iefriends.pcsx2.core.input.view.PSShoulderButtonView btn_pad_l2 = mActivity.findViewById(R.id.btn_pad_l2);
        if (btn_pad_l2 != null) {
            mActivity.applyShoulderIcon(btn_pad_l2, R.drawable.playstation_trigger_l2_alternative_outline, "ic_controller_l2_button.png");
            btn_pad_l2.setScaleX(1.0f);
            btn_pad_l2.setScaleY(isNether ? 0.6f : 1.0f);
            btn_pad_l2.setOnPSShoulderButtonListener(pressed -> NativeApp.setPadButton(KeyEvent.KEYCODE_BUTTON_L2, 0, pressed));
        }

        kr.co.iefriends.pcsx2.core.input.view.PSShoulderButtonView btn_pad_r2 = mActivity.findViewById(R.id.btn_pad_r2);
        if (btn_pad_r2 != null) {
            mActivity.applyShoulderIcon(btn_pad_r2, R.drawable.playstation_trigger_r2_alternative_outline, "ic_controller_r2_button.png");
            btn_pad_r2.setScaleX(1.0f);
            btn_pad_r2.setScaleY(isNether ? 0.6f : 1.0f);
            btn_pad_r2.setOnPSShoulderButtonListener(pressed -> NativeApp.setPadButton(KeyEvent.KEYCODE_BUTTON_R2, 0, pressed));
        }

        kr.co.iefriends.pcsx2.core.input.view.PSButtonView btn_pad_l3 = mActivity.findViewById(R.id.btn_pad_l3);
        if (btn_pad_l3 != null) {
            mActivity.applyButtonIcon(btn_pad_l3, R.drawable.playstation_button_l3_outline, "ic_controller_l3_button.png");
            btn_pad_l3.setOnPSButtonListener(pressed -> NativeApp.setPadButton(KeyEvent.KEYCODE_BUTTON_THUMBL, 0, pressed));
        }

        kr.co.iefriends.pcsx2.core.input.view.PSButtonView btn_pad_r3 = mActivity.findViewById(R.id.btn_pad_r3);
        if (btn_pad_r3 != null) {
            mActivity.applyButtonIcon(btn_pad_r3, R.drawable.playstation_button_r3_outline, "ic_controller_r3_button.png");
            btn_pad_r3.setOnPSButtonListener(pressed -> NativeApp.setPadButton(KeyEvent.KEYCODE_BUTTON_THUMBR, 0, pressed));
        }

        mActivity.applyUserUiScale();

        kr.co.iefriends.pcsx2.core.input.view.JoystickView joystickLeft = mActivity.findViewById(R.id.joystick_left);
        if (joystickLeft != null) {
            mActivity.applyJoystickStyle(joystickLeft);
            joystickLeft.setOnJoystickMoveListener((x, y) -> {
                float clampedX = Math.max(-1f, Math.min(1f, x));
                float clampedY = Math.max(-1f, Math.min(1f, y));
                sendAnalog(111, Math.max(0f, clampedX));
                sendAnalog(113, Math.max(0f, -clampedX));
                sendAnalog(112, Math.max(0f, clampedY));
                sendAnalog(110, Math.max(0f, -clampedY));
                mActivity.lastInput = MainActivity.InputSource.TOUCH;
                mActivity.lastTouchTimeMs = System.currentTimeMillis();
                mActivity.maybeAutoHideControls();
            });
        }

        kr.co.iefriends.pcsx2.core.input.view.JoystickView joystickRight = mActivity.findViewById(R.id.joystick_right);
        if (joystickRight != null) {
            mActivity.applyJoystickStyle(joystickRight);
            joystickRight.setOnJoystickMoveListener((x, y) -> {
                float clampedX = Math.max(-1f, Math.min(1f, x));
                float clampedY = Math.max(-1f, Math.min(1f, y));
                sendAnalog(121, Math.max(0f, clampedX));
                sendAnalog(123, Math.max(0f, -clampedX));
                sendAnalog(122, Math.max(0f, clampedY));
                sendAnalog(120, Math.max(0f, -clampedY));
                mActivity.lastInput = MainActivity.InputSource.TOUCH;
                mActivity.lastTouchTimeMs = System.currentTimeMillis();
                mActivity.maybeAutoHideControls();
            });
        }

        kr.co.iefriends.pcsx2.core.input.view.DPadView dpadView = mActivity.findViewById(R.id.dpad_view);
        if (dpadView != null) {
            mActivity.applyDpadStyle(dpadView);
            dpadView.setOnDPadListener((direction, pressed) -> {
                int keycode = -1;
                switch (direction) {
                    case UP:
                        keycode = KeyEvent.KEYCODE_DPAD_UP;
                        break;
                    case DOWN:
                        keycode = KeyEvent.KEYCODE_DPAD_DOWN;
                        break;
                    case LEFT:
                        keycode = KeyEvent.KEYCODE_DPAD_LEFT;
                        break;
                    case RIGHT:
                        keycode = KeyEvent.KEYCODE_DPAD_RIGHT;
                        break;
                }

                if (keycode != -1) {
                    int action = pressed ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
                    mActivity.sendKeyAction(dpadView, action, keycode);
                }
            });
        }
        mActivity.applyUserUiScale();
    }
    private void setAxisState(boolean prev, boolean now, int code) {
        if (prev == now) return;
        if (!kr.co.iefriends.pcsx2.core.input.ControllerMappingManager.isPadCodeBound(code)) {
            return;
        }
        NativeApp.setPadButton(code, 0, now);
    }
    private float getCenteredAxis(MotionEvent e, int axis) {
        final InputDevice device = e.getDevice();
        if (device != null) {
            final InputDevice.MotionRange range = device.getMotionRange(axis, e.getSource());
            if (range != null) {
                float value = e.getAxisValue(axis);
                float flat = range.getFlat();
                if (Math.abs(value) > flat) return value;
            }
        }
        return 0f;
    }
    private void sendAnalog(int keyCode, float normalized) {
        sendAnalog(keyCode, normalized, ANALOG_DEADZONE);
    }

    private void sendAnalog(int keyCode, float normalized, float deadzone) {
        if (Float.isNaN(normalized)) normalized = 0f;
        int padCode = kr.co.iefriends.pcsx2.core.input.ControllerMappingManager.getPadCodeForKey(keyCode);
        if (padCode == kr.co.iefriends.pcsx2.core.input.ControllerMappingManager.NO_MAPPING) {
            padCode = keyCode;
        }
        if (!kr.co.iefriends.pcsx2.core.input.ControllerMappingManager.isPadCodeBound(padCode)) {
            analogStates.put(padCode, 0);
            NativeApp.setPadButton(padCode, 0, false);
            return;
        }
        float value = Math.min(1f, Math.max(0f, normalized));
        if (value < deadzone) value = 0f;
        int scaled = Math.round(value * 255f);
        int prev = analogStates.get(padCode, -1);
        if (prev == scaled) return;
        analogStates.put(padCode, scaled);
        NativeApp.setPadButton(padCode, scaled, scaled > 0);
    }
    private float normalizeTrigger(float raw) {
        if (Float.isNaN(raw)) return 0f;
        if (raw < 0f) {
            return Math.min(1f, Math.max(0f, (raw + 1f) * 0.5f));
        }
        return Math.min(1f, raw);
    }
    public static void stopControllerRumbleStatic() {
        final int deviceId = sLastControllerDeviceId;
        try {
            if (deviceId >= 0) kr.co.iefriends.pcsx2.core.util.SDLControllerManager.hapticStop(deviceId);
        } catch (Throwable ignored) {}
        try {
            kr.co.iefriends.pcsx2.core.util.SDLControllerManager.hapticStop(999999);
        } catch (Throwable ignored) {}
    }
    public void stopControllerRumble() {
        stopControllerRumbleStatic();
    }
    public static float clamp01(float value) {
        if (Float.isNaN(value)) return 0f;
        if (value <= 0f) return 0f;
        return Math.min(1f, value);
    }
    public static void requestControllerRumble(float large, float small) {
        MainActivity activity = sInstanceRef.get();
        if (activity == null) {
            if (!sVibrationEnabled) stopControllerRumbleStatic();
            return;
        }
        activity.runOnUiThread(() -> activity.dispatchControllerRumble(large, small));
    }
    public static void setVibrationPreference(boolean enabled) {
        sVibrationEnabled = enabled;
        MainActivity activity = sInstanceRef.get();
        if (!enabled) {
            if (activity != null) {
                activity.runOnUiThread(() -> activity.stopControllerRumble());
            } else {
                stopControllerRumbleStatic();
            }
        }
    }
}
