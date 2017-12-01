package com.sander.updateliudemo;

/**
 * <p>Copyright © 2017 Zego. All rights reserved.</p>
 *
 * @author realuei on 30/10/2017.
 */

public interface IStateChangedListener {
    void onRoomStateUpdate();

    void onVideoCaptureSizeChanged(int width, int height, int channelIndex);

    void onPublishStateUpdate(int stateCode, String streamId);

    void onDisconnect();
}
