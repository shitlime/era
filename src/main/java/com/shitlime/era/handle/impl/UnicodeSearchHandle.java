package com.shitlime.era.handle.impl;

import com.shitlime.era.common.Dataset;
import com.shitlime.era.handle.DatasetSearchHandle;
import com.shitlime.era.utils.UnicodeUtils;
import org.springframework.stereotype.Component;

/**
 * Unicode搜索处理器
 */
@Component
public class UnicodeSearchHandle implements DatasetSearchHandle {

    @Override
    public void load(Dataset dataset) {
        // do nothing
    }

    @Override
    public String search(Dataset dataset, String character) {
        return String.format("U+%s (%s)",
                UnicodeUtils.characterToUnicode(character).toUpperCase(),
                UnicodeUtils.characterUnicodeBlock(character));
    }

    @Override
    public String searchByHex(Dataset dataset, String hex) {
        return search(dataset, UnicodeUtils.unicodeToCharacter(hex));
    }

    public String searchCharacter(Dataset dataset, String hex) {
        return UnicodeUtils.unicodeToCharacter(hex);
    }
}
