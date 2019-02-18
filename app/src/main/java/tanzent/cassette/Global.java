package tanzent.cassette;

import java.util.ArrayList;
import java.util.List;

import tanzent.cassette.bean.mp3.PlayList;

public class Global {
    public static int Operation = -1;

    public static List<PlayList> PlayList = new ArrayList<>();

    public static void setOperation(int operation) {
        Operation = operation;
    }

    public static int getOperation() {
        return Operation;
    }

    private static boolean IsHeadsetOn = false;

    public static void setHeadsetOn(boolean headsetOn) {
        IsHeadsetOn = headsetOn;
    }

    public static boolean getHeadsetOn() {
        return IsHeadsetOn;
    }

    private static boolean NotifyShowing = false;

    public static void setNotifyShowing(boolean isshow) {
        NotifyShowing = isshow;
    }

    public static boolean isNotifyShowing() {
        return NotifyShowing;
    }

    public static int PlayQueueID = 0;

    public static int MyLoveID = 0;
}
