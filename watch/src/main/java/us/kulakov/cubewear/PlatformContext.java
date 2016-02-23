package us.kulakov.cubewear;

import android.content.Context;

/**
 * An interface to the underlying platform hosting this app, useful for getting content
 */
public interface PlatformContext {
    Context getContext();
}
