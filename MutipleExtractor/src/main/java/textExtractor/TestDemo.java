package textExtractor;

import extractorUtil.ExtractEntry;

public class TestDemo {
    public static void main(String[] args) {
        final String s1 = ExtractEntry.extractContent("https://m.haiwainet.cn/ttc/3542515/2019/1111/content_31662049_1.html?tt_group_id=6757875043717431822");
        System.out.println(s1);
        System.out.println("-----------------------------");
        final String s2 = ExtractEntry.extractContent("http://www.bjnews.com.cn/finance/2019/11/05/645810.html");
        System.out.println(s2);
        System.out.println("-----------------------------");
        final String s3 = ExtractEntry.extractContent("http://news.cctv.com/2019/11/11/ARTILrBTdIkXbwz10fAmYRU6191111.shtml?spm=C94212.P4YnMod9m2uD.ENPMkWvfnaiV.5");
        System.out.println(s3);
        System.out.println("-----------------------------");
        final String s4 = ExtractEntry.extractContent("http://www.xinhuanet.com/world/2019-11/09/c_1125212437.htm");
        System.out.println(s4);
        System.out.println("-----------------------------");
        final String s5 = ExtractEntry.extractContent("http://www.sohu.com/a/352923093_220095?spm=smpc.news-home.top-news2.5.1573453976586KUei05z&_f=index_chan08news_4");
        System.out.println(s5);
        System.out.println("-----------------------------");
        final String s6 = ExtractEntry.extractContent("https://news.ifeng.com/c/7rVShztNS8L");
        System.out.println(s6);
    }

}
