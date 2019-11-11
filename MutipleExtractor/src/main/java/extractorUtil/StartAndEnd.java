package extractorUtil;

public class StartAndEnd{
    Integer startNum=null;
    Integer endNum = null;
    public StartAndEnd(int startNum,int endNum){
        this.startNum = startNum;
        this.endNum = endNum;
    }

    @Override
    public String toString() {
        return startNum+","+endNum;
    }
}