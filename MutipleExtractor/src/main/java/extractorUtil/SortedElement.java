
package extractorUtil;

import org.jsoup.nodes.Element;

public class SortedElement implements Comparable<SortedElement> {

	private Element element;
	private int textLength;
	
	public SortedElement(Element element, int textLength) {
		super();
		this.element = element;
		this.textLength = textLength;
	}
	
	public Element getElement() {
		return element;
	}
	public void setElement(Element element) {
		this.element = element;
	}
	public int getTextLength() {
		return textLength;
	}
	public void setTextLength(int textLength) {
		this.textLength = textLength;
	}

	public int compareTo(SortedElement o) {
		
		return o.getTextLength()-textLength;
	}

	@Override
	public String toString() {
		return "SortedElement [element.attr=" + element.attributes() + ", textLength=" + textLength + "]";
	}
}

