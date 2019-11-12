package extractorUtil;

import org.jsoup.nodes.Document;

import java.util.*;

//当精确抽取抽取失败时,转向基于行块分布的暴力抽取策略
public class ViolenceExtract {
    //最大允许的正文行与行之间允许的空行数
    private static int MAX_ALLOWED_THICK = 3;
    //初始关注字数
    private static int MIN_NOTICED_TEXTLENGTH = 10;
    //延续关注字数
    private static int CONTINUE_NOTICED_TEXTLENGTH = 20;
    //正文抽取阀值(单个文本块需要占全文字数的比例，不是总共的)
    private static int SINGLE_EXTRACT_RATIO = 35;

    private static String[] negativeWords = {"责任编辑","责编","本文来源","编辑：","编辑","来源：","本报记者","Rights","Copyright"};

    private static int NEGATIVEWORDS_MAXLENGTH = 25;

    public static String getText(Document document){
        final String text = document.body().toString();
        //删除不需要的标签
        final String pureText = text.replaceAll("<!--.*?-->", "")
                .replaceAll("<script[\\s\\S]*?>[\\s\\S]*?</script>", "")
                .replaceAll("<style[\\s\\S]*?>[\\s\\S]*?</style>", "")
                .replaceAll("<link[\\s\\S]*?>[\\s\\S]*?</link>", "")
                .replaceAll("<style[\\s\\S]*?>[\\s\\S]*?</style>","")
                .replaceAll("<[\\s\\S]*?>", "");
      //  System.out.println(pureText);

        //统计总长度
        long totalCount =0;
        final String[] splits = pureText.split("\n");
        List<Integer>  featureList = new ArrayList<Integer>();
        for(int i =0;i<splits.length;i++){
            final String s = splits[i];
            featureList.add(s.trim().length());
            totalCount +=s.trim().length();
        }

        Map<StartAndEnd,Integer> ma1 = new HashMap<StartAndEnd, Integer>();
        List<Map.Entry<StartAndEnd, Integer>> li =null;
        if(featureList.size()<=MAX_ALLOWED_THICK){
            throw new RuntimeException("html的行太少了");
        }else{
            for(int i=0;i<featureList.size()-MAX_ALLOWED_THICK;i++){
                Boolean innerFlag = false;
                if(featureList.get(i)>=MIN_NOTICED_TEXTLENGTH){
                    for(int j=0;j<MAX_ALLOWED_THICK;j++){
                        if(featureList.get(i+(j+1))>=CONTINUE_NOTICED_TEXTLENGTH){
                            innerFlag = true;
                        }
                    }
                }
               //  System.out.println(i);
               //找终点
               if(innerFlag){
                   int endNum = i;
                   for(int k=i;k<featureList.size();k++){
                       int count = 0;
                       boolean tmpFlag = false;
                       for(int p=0;p<MAX_ALLOWED_THICK;p++){
                           if(featureList.get(k+(p+1))<MIN_NOTICED_TEXTLENGTH){
                               count++;
                           }
                       }
                       //说明已经有连续MAX_ALLOWED_THICK个小于最小关注长度了
                       if(count==MAX_ALLOWED_THICK){
                           endNum = k;
                           break;
                       }
                   }
                   ma1.put(new StartAndEnd(i,endNum),endNum-i);

               }
            }
            li = new ArrayList<Map.Entry<StartAndEnd, Integer>>(ma1.entrySet());
            Collections.sort(li, new Comparator<Map.Entry<StartAndEnd, Integer>>() {
                public int compare(Map.Entry<StartAndEnd, Integer> o1, Map.Entry<StartAndEnd, Integer> o2) {
                    return o2.getValue()-o1.getValue();
                }
            });
        }
        List<Map.Entry<StartAndEnd, Integer>> newLi= new ArrayList<Map.Entry<StartAndEnd, Integer>>();

        //删除li中重复的区块
        Integer endNum1 =null;
        Integer startNum1 = null;
        Integer endNum2 =null;
        Integer startNum2 = null;
        for(int i = 0;i<li.size()-1;i++){
            startNum1 = li.get(i).getKey().startNum;
            endNum1 = li.get(i).getKey().endNum;
            for(int j=i+1;j<li.size();j++){
                startNum2 = li.get(j).getKey().startNum;
                endNum2 = li.get(j).getKey().endNum;
                if(startNum1<=startNum2 && endNum1>=endNum2){
                    li.get(j).setValue(0);
                }
            }
        }
        //过滤掉li中value等于0的
        for(int i=0;i<li.size();i++){
            if(li.get(i).getValue()!=0){
                newLi.add(li.get(i));
            }
        }
        //计算字数和totalNum的比例,需要大于20%
        Integer endNum =null;
        Integer startNum = null;
        StringBuilder tmp = new StringBuilder();

        for(int i=0;i<newLi.size();i++){
            long currentCount = 0;
            endNum = newLi.get(i).getKey().endNum;
            startNum = newLi.get(i).getKey().startNum;
            for(int j=startNum;j<=endNum;j++){
                currentCount += featureList.get(j);
            }
            final double v = currentCount * 1.0 / totalCount * 1.0;
            if(v*100>=SINGLE_EXTRACT_RATIO*1.0){
                for(int k=startNum;k<=endNum;k++){
                    boolean negativeFlag = false;
                    for(String str:negativeWords){
                        if(splits[k].matches(".*"+str+".*") && splits[k].trim().length()<=NEGATIVEWORDS_MAXLENGTH){
                            negativeFlag = true;
                            break;
                        }
                        if(splits[k].split("\\||│").length>=2){
                            negativeFlag = true;
                            break;
                        }
                    }
                    if(!negativeFlag){
                        tmp.append(splits[k].trim()+"\n");
                    }
                }
            }
        }

        return tmp+"";
    }
}
