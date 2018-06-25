package player;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;

import com.afap.ijkplayer.R;
import com.shuyu.gsyvideoplayer.utils.CommonUtil;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

public class TVPlayerView extends StandardGSYVideoPlayer{
    public TVPlayerView(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public TVPlayerView(Context context) {
        super(context);
    }

    public TVPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    private boolean changeProgressByKey = false;
    private int oldProgressValue = -1;
    private int newProgressValue = -1;
    private int keyDownComboCount = 0;
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.e("dodo", "onKeyup: "+event.getKeyCode());
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.e("dodo", "onKeyup: ");
                if(changeProgressByKey){
                    changeProgressByKey = false;
                    oldProgressValue = -1;
                    endGesture();
                }
                break;

        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("dodo", "onKeyDown: "+event.getKeyCode());
        switch (keyCode){

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:

                if(!changeProgressByKey)changeProgressByKey = true;
                if(oldProgressValue == -1){
                    oldProgressValue = 0;
                    newProgressValue = oldProgressValue;
                }
                newProgressValue += keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -5 : 5;
                int max = this.getDuration();
                //Log.d(TAG, "newProgressValue = " + newProgressValue);
                if(newProgressValue < (0 - max))newProgressValue = (0 - max);
                if(newProgressValue > max)newProgressValue = max;
                float deltaP = oldProgressValue - newProgressValue;
                String seekTime = CommonUtil.stringForTime(newProgressValue);
                String totalTime = CommonUtil.stringForTime(max);
                showProgressDialog(0, seekTime, newProgressValue, totalTime, max);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_UP:

                break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                doPauseResume();

                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 手势结束
     */
    private void endGesture() {
        getGSYVideoManager().seekTo(newProgressValue);

    }
    private void doPauseResume()
    {
        if (this.getCurrentState()==CURRENT_STATE_PLAYING){

            this.onVideoPause();
            Log.e("dodo", "onVideoPause: ");
        }else
        {
            this.onVideoResume();
            Log.e("dodo", "onVideoResume: ");

        }
    }
//    @Override
//    public int getLayoutId() {
//        return R.layout.tvplayer_view;
//    }
}
