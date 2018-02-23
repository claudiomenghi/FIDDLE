package ltsa.lts.util;

public class Counter {
    private int count;

    public Counter(int i){
        count = i;
    }

    public Integer label() {
        return new Integer(count++);
    }

    public Integer lastLabel() {
        return new Integer(count);
    }
    
    public Integer interval(int size) {
        int tmp = count;
        count+=size;
        return new Integer(tmp);
    }
        
}