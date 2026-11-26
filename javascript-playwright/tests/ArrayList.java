import java.util.ArrayList;

public class Main{
    public static void main(String [] args){
        ArrayList<String> list=new ArrayList<String>();
        list.add("Nayana");
        list.add("Bhavya");
        list.add("Rohit");
        list.add("Kavya");
        list.add("Megha");

        for (String name : list) {
            System.out.println(name);
        }
    }
}