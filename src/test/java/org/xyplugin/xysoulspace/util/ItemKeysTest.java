package org.xyplugin.xysoulspace.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ItemKeysTest {
    @Test
    public void optimizedHexEncodingKeepsStandardSha256Output() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ItemKeys.sha256("abc"));
    }
}
