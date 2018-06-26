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

    private int keyDownComboCount = 0;
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.e("dodo", "onKeyup: "+event.getKeyCode());
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (changeProgressByKey){
                    changeProgressByKey = false;
                    Log.e("dodo", "onKeyup: ");
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

                changeProgressByKey = true;
                mSeekTimePosition = (int)getGSYVideoManager().getCurrentPosition();
                oldProgressValue = mSeekTimePosition;
                mSeekTimePosition += (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) ? -15*1000 : 15*1000;
                int max = this.getDuration();
                Log.e("dodo", "mSeekTimePosition = "+mSeekTimePosition+ "max"+max+"oldProgressValue"+oldProgressValue);
                if(mSeekTimePosition < 0)mSeekTimePosition = 0;
                if(mSeekTimePosition > max)mSeekTimePosition = max;
                float deltaP = oldProgressValue - mSeekTimePosition;
                String seekTime = CommonUtil.stringForTime(mSeekTimePosition);
                String totalTime = CommonUtil.stringForTime(max);
//                showProgressDialog(deltaP, seekTime, mSeekTimePosition, totalTime, max);
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
        Log.e("dodo", "seekTo: "+mSeekTimePosition );
        getGSYVideoManager().seekTo(mSeekTimePosition);

    }
    private void doPauseResume()
    {
        if (!mHadPlay) {
        return;
        }
        clickStartIcon();
    }
//    @Override
//    public int getLayoutId() {
//        return R.layout.tvplayer_view;
//    }
}
