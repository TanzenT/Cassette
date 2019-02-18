package tanzent.cassette.lyric.bean;

import tanzent.cassette.lyric.UpdateLyricThread;

import static tanzent.cassette.lyric.UpdateLyricThread.EMPTY_ROW;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/5/10 13:37
 */

public class LyricRowWrapper {
    public LrcRow LineOne = EMPTY_ROW;
    public LrcRow LineTwo = EMPTY_ROW;
    public UpdateLyricThread.Status mStatus;

    public LrcRow getLineOne() {
        return LineOne;
    }

    public void setLineOne(LrcRow lineOne) {
        LineOne = lineOne;
    }

    public LrcRow getLineTwo() {
        return LineTwo;
    }

    public void setLineTwo(LrcRow lineTwo) {
        LineTwo = lineTwo;
    }

    public UpdateLyricThread.Status getStatus() {
        return mStatus;
    }

    public void setStatus(UpdateLyricThread.Status status) {
        mStatus = status;
    }

    @Override
    public String toString() {
        return "LyricRowWrapper{" +
                "LineOne=" + LineOne +
                ", LineTwo=" + LineTwo +
                '}';
    }
}
