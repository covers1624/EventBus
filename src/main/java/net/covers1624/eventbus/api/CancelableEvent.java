package net.covers1624.eventbus.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Created by covers1624 on 22/3/21.
 */
@ApiStatus.Experimental // Not yet implemented.
public interface CancelableEvent extends Event {

    boolean isCanceled();

    void setCanceled();

}
