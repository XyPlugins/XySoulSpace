package org.xyplugin.xysoulspace.api;

import org.xyplugin.xysoulspace.XySoulSpacePlugin;

public final class XySoulSpace {
    private XySoulSpace() {
    }

    public static XySoulSpaceApi get() {
        XySoulSpacePlugin plugin = XySoulSpacePlugin.getInstance();
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException("XySoulSpace is not enabled");
        }
        return plugin.getApi();
    }
}
