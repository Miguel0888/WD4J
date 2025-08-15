package de.bund.zrb.meta;

/** Receive meta events; implement on UI or service layer. */
public interface MetaEventListener {
    void onMetaEvent(MetaEvent event);
}
