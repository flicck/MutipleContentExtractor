package extractorUtil;

import java.util.Map;

public class ExtractEntry {
    public static String extractContent(String url){
        final Map<String, String> ma1 = AccurateExtract.generateXpath2(url);
        final String content = AccurateExtract.parseXpath(ma1, url);
        return content;
    }

}
