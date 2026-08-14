package org.xyplugin.xysoulspace;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class XySoulSpacePluginTest {
    @Test
    public void normalModesPreferTheActualCustomDisplayName() {
        assertEquals("未净化的赤牙墨魄", XySoulSpacePlugin.selectMessageItemName(
                "id", "xyitems:chiyamopo", "未净化的赤牙墨魄", "钻石剑"));
    }

    @Test
    public void namelessCustomItemsFallBackToTheirLibraryId() {
        assertEquals("xyitems:chiyamopo", XySoulSpacePlugin.selectMessageItemName(
                "name", "xyitems:chiyamopo", "", "钻石剑"));
    }

    @Test
    public void vanillaItemsUseTheirFriendlyMaterialName() {
        assertEquals("铁锭", XySoulSpacePlugin.selectMessageItemName(
                "id", "minecraft:IRON_INGOT", "", "铁锭"));
    }

    @Test
    public void rawIdModeRemainsAvailableForDiagnostics() {
        assertEquals("xyitems:chiyamopo", XySoulSpacePlugin.selectMessageItemName(
                "raw-id", "xyitems:chiyamopo", "未净化的赤牙墨魄", "钻石剑"));
    }
}
