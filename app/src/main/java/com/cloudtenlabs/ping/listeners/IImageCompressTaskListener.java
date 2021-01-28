package com.cloudtenlabs.ping.listeners;

import java.io.File;
import java.util.List;

/**
 * Created by root on 9/14/17.
 */

public interface IImageCompressTaskListener {

    void onComplete(List<File> compressed);
    void onError(Throwable error);
}