
package extractorUtil;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.io.IOException;
import java.util.*;

public class AccurateExtract {
	
	private static final int P = 50;
	private static final double T = 0.65d;

	private static String[] excludeTags = {"a","link","script","input","form","noscript","style","iframe","ul"};
	
	private static String[] negativeWords = {"责任编辑","责编","本文来源","编辑：","编辑","来源：","本报记者"};

    private static int NEGATIVEWORDS_MAXLENGTH = 15;

	private static String CONTENT = "正文";
	
	private static boolean exclude(String tag){
		for(String e : excludeTags){
			if(e.equals(tag)){
				return true;
			}
		}
		return false;
	}

	public static Map<String,String> generateXpath(String html){
		//Document dom = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0").get();
		Document dom = Jsoup.parse(html);
		Map<String,String> map = generateXpath(dom);
		return map;
	}
	
	public static Map<String,String> generateXpath2(String url){
		Document dom;
		Map<String,String> xpath = null;
		try {
			dom = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0").get();

            //暴力去除掉<noscript>标签，不然新浪新闻是没法爬的
            String str = dom.toString().replace("<noscript>", "").replace("</noscript>","");
            dom = Jsoup.parse(str);

			xpath = generateXpath(dom);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return xpath;
	}

	
	/**
	 * 	该递归算法是找子节点中是否有P标签
	 *  1. 如果有P标签，判断P标签所占字数 / 节点总字数  是否 < T，如果小于 则认为是非法的P标签 ，无视掉
	 *  2. 如果没有P标签则选出子节点中文字比例 >= T 的继续递归查找，直到没有子节点为止
	 *     如果子节点中没有中文字比例 >= T 的，直接返回自身
	 *     
	 *  问题：如果一直没有P，则可能会返回一个错误的节点
	 */
	private static Element getDeepElementWithPTag(Element parent){
		Elements elements = parent.children();
		if(elements.size() == 0){
			return parent;
		}
		
		int pCount = 0;
		int pTextCount = 0;
		for(int i=0;i<elements.size();i++){
			Element e = elements.get(i);
			if(e.tagName().equals("p")){
				pCount ++;
				pTextCount += e.text().trim().length();
			}
		}
		if(pTextCount > 0){
			double rate = pTextCount * 1d / parent.text().trim().length();
			if(rate < T){
				pCount = 0;
			}
		}
		
		if(pCount == 0){
			for(int i=0;i<elements.size();i++){
				Element e = elements.get(i);
				double rate = e.text().trim().length() * 1d / parent.text().trim().length();
				//System.out.println(e.text().trim());
				if(rate >= T){
					return getDeepElementWithPTag(e);
				}
			}
		}
		return parent;
	}
	
	public static String getXPath(Element e){
		StringBuilder sb = new StringBuilder();
		while(e.parent()!=null){
			if(!"".equals(e.attr("id"))){
				if(e.tagName().equals("html") || e.tagName().equals("body")){
					sb.insert(0, e.tagName());
				}else{
					sb.insert(0, e.tagName() + "[@id='"+e.attr("id")+"']");
				}
				sb.insert(0, "//");
				break;
			}else if(!"".equals(e.attr("class"))) {
				if(e.tagName().equals("html") || e.tagName().equals("body")){
					sb.insert(0, e.tagName());
				}else{
					sb.insert(0, e.tagName() + "[@class='"+e.attr("class")+"']");
				}
				sb.insert(0, "/");
			}else{
				sb.insert(0, e.tagName());
				sb.insert(0, "/");
			}
			e = e.parent();
		}
		while(!sb.toString().startsWith("//")){
			sb.insert(0, "/");
		}
		return sb.toString();
	}
	
	private static Map<String,String> generateXpath(Document dom){
		Map<String,String> resultMap = null;
		try {
			//Document dom = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0").get();
			Element body = dom.body();
			Elements elements = body.getAllElements();
			for(int i=0;i<elements.size();i++){
				if(exclude(elements.get(i).tagName())){
					elements.get(i).remove();
				}
			}
			
			elements = body.children();
			List<SortedElement> candidacyElements = new ArrayList<SortedElement>();
			for(int i=0;i<elements.size();i++){
				Element e = elements.get(i);  //body下第一层 Element
				if(e.text().length() >= P){
					candidacyElements.add(new SortedElement(e,e.text().length()));
				}
			}
			
			if(candidacyElements.size() == 0){
				//一个大于50字的子节点都没有，就认为无正文
				return null;
			}
			
			Collections.sort(candidacyElements);
			
			Element contentElement = null;
			SortedElement se = candidacyElements.get(0);	//取得body下 文字最多的那个元素，遍历其子节点
			//System.out.println(se);
			Elements subChilds = se.getElement().children();
			for(int j=0;j<subChilds.size();j++){
				Element e = subChilds.get(j);
				String text = e.text();
				int len = text.length();
				double rate  = len * 1d / se.getTextLength();
				//System.out.println(rate);
				if(rate >= T) {
					contentElement = e;
					break;
				}
			}
			
			if(contentElement == null){
				//如果没有子节点、或者子节点中没有>=65%的，就把它自己返回
				contentElement = se.getElement();
			}
			
			//递归查找包含P的子节点，这是一个不断精确的过程
			Element finalElement = getDeepElementWithPTag(contentElement);
			Elements pElements = finalElement.children();
			int pCount = 0;
			for(int i=0;i<pElements.size();i++){
				Element e = pElements.get(i);
				if(e.tagName().equals("p")){
					pCount ++;
				}
			}
			
			if(pCount == 0){
				//递归之后，还是不包含P（通常是另类新闻模板或者陈旧的html写法）, 则使用之前找到的节点
				finalElement = contentElement;
			}
			//System.out.println(finalElement.tagName() +"\t" + finalElement.attributes());
			
			String xpath = getXPath(finalElement);
			xpath = xpath + "//p";

			JXDocument jx = JXDocument.create(dom.toString());
			
			
			resultMap = negativeXPath(finalElement,xpath,jx);

			resultMap.put(xpath, CONTENT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultMap;
	}
	
	private static Map<String,String> negativeXPath(Element element,String xpath,JXDocument jx){
		Elements elements = element.children();
		int len = element.text().length();
		Map<String,String> set = new HashMap<String,String>();
		for(int i=0;i<elements.size();i++){
			Element e = elements.get(i);
			for(String nw : negativeWords){
				if(e.text().indexOf(nw) != -1){
					int len1 = e.text().length();
					double rate  = len1 * 1d / len;
					//System.out.println(rate);
					if(rate > (1-T)) {
						//如果选出来的这个反规则占的字数 > 0.35 就不要了
						continue;
					}
                    String newXpath ="";
                    if(!e.id().equals("")) {
                        set.put(xpath + "/" + e.tagName() + "[@id='" + e.id() + "']", nw);
                    }else{
                        try {
                            String newXpath1 = "//div[contains(text(),'" + nw + "')]";
                            String newXpath2 = "//span[contains(text(),'" + nw + "')]";
                            String newXpath3 = "//strong[contains(text(),'" + nw + "')]";
                            String newXpath4 = "//p[contains(text(),'" + nw + "')]";
                            List sel1 = jx.sel(newXpath1);
                            Elements e1 = new Elements(sel1);
                            if (e1.size() != 0) {
                                set.put(newXpath1, nw);
                            }
                            List sel2 = jx.sel(newXpath2);
                            Elements e2 = new Elements(sel2);
                            if (e2.size() != 0) {
                                set.put(newXpath2, nw);
                            }
                            List sel3 = jx.sel(newXpath3);
                            Elements e3 = new Elements(sel3);
                            if (e3.size() != 0) {
                                set.put(newXpath3, nw);
                            }
                            List sel4 = jx.sel(newXpath4);
                            Elements e4 = new Elements(sel4);
                            if (e4.size() != 0) {
                                set.put(newXpath4, nw);
                            }
                        }catch (Exception k){
                            k.printStackTrace();
                        }

                    }

                }
			}
		}

		return set;
	}
	//传入xpath,方便返回的是抽取的内容
	public static String parseXpath(Map<String, String> resultMap,String url){
        if(resultMap!=null) {
            final Set<Map.Entry<String, String>> entries = resultMap.entrySet();
            //拆分txpath和fxpath
            Map<String, String> fxpaths = new HashMap<String, String>() {
            };
            String txpath = "";
            for (Map.Entry<String, String> me1 : entries) {
                if (me1.getValue().equals("正文")) {
                    txpath = me1.getKey();
                } else {
                    fxpaths.put(me1.getKey(), me1.getValue());
                }
            }
            //尝试抽取正文
            Document document = null;
            String outText = "";
            String outText1 = "";
            JXDocument jxDocument = null;
            try {
                document = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0").get();
                outText1 = ViolenceExtract.getText(document);
                jxDocument = JXDocument.create(document);
                final List<JXNode> jxNodes = jxDocument.selN(txpath);
                if (jxNodes != null && jxNodes.size() != 0) {

                    for (JXNode jxNode : jxNodes) {
                        outText = outText + jxNode.asElement().text() + "\n";
                    }
                    //走到这里说明正文已经抽取成功
                    //	System.out.println(outText);

                }
            } catch (Exception e) {
               // e.printStackTrace();
                //走到这里说明抽取失败,转向暴力抽取策略
                outText1 = ViolenceExtract.getText(document);
            }
            //  StringBuilder tmp = new StringBuilder();
            if ("".equals(outText)) {
                //没有内容也说明抽取失败,转向暴力抽取策略
                if ("".equals(outText1)) {
                    outText1 = ViolenceExtract.getText(document);
                }
            } else {
                //有内容说明抽取成功,对fxpaths的内容进行过滤
                Set<Map.Entry<String, String>> entries1 = fxpaths.entrySet();
                for (Map.Entry<String, String> me : entries1) {
                    try {
                        final List<JXNode> jxNodes = jxDocument.selN(me.getKey());
                        if (jxNodes != null && jxNodes.size() != 0) {
                            for (JXNode jxn : jxNodes) {
                                if (jxn.asElement().text().length() <= NEGATIVEWORDS_MAXLENGTH) {
                                    outText = outText.replace(jxn.asElement().text().trim(), "");
                                }
                            }
                        }
                    } catch (Exception e) {
                      //  e.printStackTrace();
                        continue;
                    }
                }

            }
         //   System.out.println("accurate:" + outText);
         //   System.out.println("violence:" + outText1);

            return !"".equals(outText) ? outText : outText1;
        }else{
            String outText1 ="";
            try{
                final Document document = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0").get();
                outText1 = ViolenceExtract.getText(document);
            }catch(Exception e){
                e.printStackTrace();
            }
            System.out.println(outText1);
            return outText1;
        }
	}

	public static void main(String[] args) {
	  //  String url = "http://www.bjnews.com.cn/finance/2019/11/05/645810.html";
      //  String url = "https://world.huanqiu.com/article/9CaKrnKnIyB";
     //   String url ="https://news.163.com/19/1111/01/ETLQ46P90001875P.html";
        String url ="https://m.haiwainet.cn/ttc/3542515/2019/1111/content_31662049_1.html?tt_group_id=6757875043717431822";
		final Map<String, String> stringStringMap = AccurateExtract.generateXpath2(url);
        final String s = parseXpath(stringStringMap, url);

	}
}

