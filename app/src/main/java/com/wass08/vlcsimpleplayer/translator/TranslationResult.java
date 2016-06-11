package com.wass08.vlcsimpleplayer.translator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ewnd9 on 11.06.16.
 */
public class TranslationResult {

    public List<Definitions> def;

    public List<Translation> getTranslations() {
        List<Translation> result = new ArrayList<>();

        for (Definitions def : this.def) {
            result.addAll(def.tr);
        }

        return result;
    }

    public class Translation {
        public String text;
    }

    public class Definitions {
        public List<Translation> tr;
    }
}
