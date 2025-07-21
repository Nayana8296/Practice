import java.util.Scanner;

public class AreaOfTriangle{
public static void main(String [] args){

Scanner scanner = new Scanner(System.in);
System.out.println("enter a breadth value:");
int b = scanner.nextInt();

System.out.println("enter a height value:");
int h = scanner.nextInt();

int area;
 area = .5*b*h;

System.out.println("Area of Triangle:"+area);
}
}

