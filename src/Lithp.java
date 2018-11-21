import java.util.Scanner;

/**
 * Main class of the Lithp interpreter.
 * Handles the REPL.
 *
 * @author Max Kopinsky
 */
public class Lithp {
    private static String input;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        System.out.println("Lithp version 0.0.0.2\nUse exit() to exit.");

        boolean done = false;
        while(!done) {
            System.out.print("Lithp>");

            input = in.nextLine();
            done = input.equals("exit()"); // todo: make this a builtin function
            System.out.println(String.format("%s", input));
        }
    }
}
