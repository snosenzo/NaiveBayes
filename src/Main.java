import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    private static double[][][] groups = new double[5][921][58];
    public static void main(String[] args) throws IOException {
        if(args.length != 1) {
            System.out.println("Please enter a data file");
            System.exit(3);
        }

        File dataFile = new File(args[0]);
        Scanner inScan = new Scanner(dataFile);
        populateGroups(inScan);
        inScan.close();
        System.out.println("Data Read in");
    }

    public static void populateGroups(Scanner inScan) {
        int currentGroup = 0;
        int[] groupsElementIndex = new int[5];
        Arrays.fill(groupsElementIndex, 0);
        while(inScan.hasNextLine()) {
            String nextLine = inScan.nextLine();
            populateElement(nextLine, groups[currentGroup][groupsElementIndex[currentGroup]]);
            groupsElementIndex[currentGroup]++;
            currentGroup = (currentGroup + 1) % groups.length;
        }
        for(int i = 0; i < groups.length; i++) {
            for(int j = groupsElementIndex[i]; j < groups[i].length; j++) {
                Arrays.fill(groups[i][j], -1);
            }
        }
    }

    public static void populateElement(String lineData, double[] element) {
        String[] separated = lineData.split(",");
        for(int i = 0; i < separated.length; i++) {
            element[i] = Double.parseDouble(separated[i]);
        }
    }
}
