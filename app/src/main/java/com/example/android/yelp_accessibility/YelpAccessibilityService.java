package com.example.android.yelp_accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class YelpAccessibilityService extends AccessibilityService {

    private static final String TAG = "YelpAccessibility";

    private static SpeechRecognizer sr;

    /**
     * Start the speech recognizer
     */
    public static void buttonClick() {
        if(sr != null) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            sr.startListening(intent);
        }
    }

    /**
     * Initializes speech recognizer and starts the floating button
     */
    @Override
    protected void onServiceConnected() {
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new SRListener());
        final Intent intent = new Intent(this, FloatingButton.class);
        ContextCompat.startForegroundService(this, intent);
        Log.d(TAG, "Service Connected");
    }

    /**
     * Listener for speech recognizer
     */
    private class SRListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle bundle) { Log.d(TAG, "onReadyForSpeech"); }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onRmsChanged(float v) {
            Log.d(TAG, "onRMSChanged");
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            Log.d(TAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int i) {
            Log.d(TAG, "onError " + i);
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults");
            ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            parseCommand(data.get(0));
        }

        @Override
        public void onPartialResults(Bundle bundle) {
            Log.d(TAG, "onPartialResults");
        }

        @Override
        public void onEvent(int i, Bundle bundle) {
            Log.d(TAG, "onEvent");
        }
    }

    /**
     * Splits the request into a command and a target
     * @param request The user request from the speech recognizer
     */
    public void parseCommand(String request) {
        if(!checkYelp()) {
            Log.d(TAG, "Not Yelp");
            return;
        }

        if(request.equals("back") || request.equals("go back")) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            return;
        }

        String[] parts = request.split(" ", 2);
        String command = parts[0].toLowerCase();
        String target = parts[1].toLowerCase();
        Log.d(TAG, "command: " + command + " target: " + target);

        if(command.equals("click") || command.equals("quick") || command.equals("clique"))
            clickCommand(target);
        else if(command.equals("scroll"))
            scrollCommand(target);
    }

    /**
     * Checks if the current app is Yelp
     * @return true if the current app is Yelp. false otherwise
     */
    public boolean checkYelp() {
        return this.getRootInActiveWindow().getPackageName().toString().equals("com.yelp.android");
    }

    /**
     * Clicks on target if it's clickable
     * @param target Target text
     */
    public void clickCommand(String target) {
        Log.d(TAG, "click command");
        List<AccessibilityNodeInfo> nodes = this.getRootInActiveWindow().findAccessibilityNodeInfosByText(target);
        boolean clicked = false;
        if(nodes.isEmpty())
            Log.d(TAG, "is empty");
        for(AccessibilityNodeInfo info : nodes) {
            if(info != null) {
                if(!clicked) {
                    clicked = tryPerformAction(info, AccessibilityNodeInfo.ACTION_CLICK);
                }
                info.recycle();
            }
            Log.d(TAG, info.toString());
        }

        if(!clicked)
            Log.d(TAG, "Can't click");
    }

    /**
     * Scrolls up or down if there is a srollable view.
     * @param target either "up" or "down"
     */
    public void scrollCommand(String target) {
        AccessibilityNodeInfo root = this.getRootInActiveWindow();
        AccessibilityNodeInfo info = findScrollable(root);
        if(info != null) {
            if(target.equals("down"))
                info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            else if(target.equals("up"))
                info.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        } else
            Log.d(TAG, "Can't scroll");
    }

    /**
     * Finds a scrollable view recursively
     * @param info the AccessibilityNodeInfo to check if scrollable
     * @return a scrollable view or null
     */
    public AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo info) {
        List<AccessibilityNodeInfo.AccessibilityAction> actList = info.getActionList();
        for(AccessibilityNodeInfo.AccessibilityAction action : actList) {
            if(action.getId() == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD || action.getId() == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                return info;
            }
        }

        for(int i = 0; i < info.getChildCount(); i++) {
            AccessibilityNodeInfo ret = info.getChild(i);
            ret = findScrollable(ret);
            if(ret != null) {
                info.recycle();
                return ret;
            }
        }

        info.recycle();
        return null;
    }

    /**
     * Tries to perform action on info or its parent. (Probably only needed for clickCommand)
     * @param info
     * @param action
     * @return
     */
    public boolean tryPerformAction(AccessibilityNodeInfo info, int action) {

        List<AccessibilityNodeInfo.AccessibilityAction> actList = info.getActionList();
        for(AccessibilityNodeInfo.AccessibilityAction act : actList) {
            if(act.getId() == action) {
                info.performAction(action);
                return true;
            }
        }

        AccessibilityNodeInfo parent = info.getParent();
        actList = parent.getActionList();
        for(AccessibilityNodeInfo.AccessibilityAction act : actList) {
            if(act.getId() == action) {
                parent.performAction(action);
                parent.recycle();
                return true;
            }
        }

        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    /**
     * Destroys the speech recognizer
     * @param intent
     * @return
     */
    @Override
    public boolean onUnbind(Intent intent) {
        if(sr != null) {
            sr.destroy();
            sr = null;
        }
        return false;
    }
}
