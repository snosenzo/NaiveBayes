import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    private static double[] elementMins = new double[58];
    private static double[] elementMaxs = new double[58];
    private static double[] elementAverages = new double[58];
    private static int numElements = 0;
    private static double[][][] groups = new double[5][921][58];
    private static double[][] trainedProb = new double[58][2];
    private static ArrayList<TrainedSet> trainedSets;



    public static void main(String[] args) throws IOException {
        if(args.length != 1) {
            System.out.println("Please enter a data file");
            System.exit(3);
        }

        File dataFile = new File(args[0]);
        Scanner inScan = new Scanner(dataFile);
        populateGroups(inScan);
        inScan.close();
        populateOverallStats();
        trainedSets = new ArrayList<>();
        System.out.println("Data Read in");
        System.out.println("Iteration \t NumPosTrain \t NumNegTrain \t NumPosDev \t NumNegDev");
        for(int i = 0; i < groups.length; i++) {
            TrainedSet t = new TrainedSet(groups.length - (i+1));
            System.out.println((i+1) + "\t\t\t\t" + t.getSampleInfo());
            trainedSets.add(t);
        }

        for(TrainedSet t: trainedSets) {
            t.trainProb();
            System.out.println(t.getProbabilityTable());
        }

        System.out.println("\nResults:\n");
        double CORR = 0;
        double FN = 0;
        double FP = 0;
        double ERR = 0;

        for(TrainedSet t: trainedSets) {
            t.testData();
            CORR += t.CORR;
            FN += t.FN;
            FP += t.FP;
            ERR += t.ERR;
        }
        CORR /= groups.length;
        FN /= groups.length;
        FP /= groups.length;
        ERR /= groups.length;

        System.out.println("Avg,\tFP: " + FP + ", FN: " + FN + ", ERR: " + ERR);



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
            numElements++;
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

    public static void populateOverallStats() {
        Arrays.fill(elementAverages, 0);
        Arrays.fill(elementMaxs, 0);
        Arrays.fill(elementMins, Double.MAX_VALUE);
        for(int i = 0; i < groups.length; i++) {
            for(int j = 0; j < groups[i].length; j++) {
                double[] element = groups[i][j];
                if(element[i] == -1){
                    break;
                }
                for(int k = 0; k < element.length; k++) {
                    if(element[k] < elementMins[k]) {
                        elementMins[k] = element[k];
                    }
                    if(element[k] > elementMaxs[k]) {
                        elementMaxs[k] = element[k];
                    }
                    elementAverages[k]+=element[k];
                }
            }
        }
        for(int i = 0; i < elementAverages.length; i++) {
            elementAverages[i] = elementAverages[i] / ((double) numElements);
        }
    }

    private static class TrainedSet {
        private int testSetIndex;
        private int numPosTrainSamples;
        private int numNegTrainSamples;
        private int numTrainSamples;
        private int numPosDevSamples;
        private int numNegDevSamples;
        private int numDevSamples;
        private double[][] featureProb;

        private double TP;
        private double FP;
        private double TN;
        private double FN;
        private double ERR;
        private double CORR;

        public TrainedSet(int testSetIndex) {
            this.testSetIndex = testSetIndex;
            getNumSamples();
            featureProb = new double[58][4];
        }

        public void getNumSamples() {
            numPosTrainSamples = 0;
            numNegTrainSamples = 0;
            numTrainSamples = 0;

            numPosDevSamples = 0;
            numNegDevSamples = 0;
            numDevSamples = 0;
            for(int i = 0; i < groups.length; i++) {
                for(int j = 0; j < groups[i].length; j++) {
                    double[] element = groups[i][j];
                    if(element[0] == -1) {
                        break;
                    }
                    if(testSetIndex != i) {
                        numPosTrainSamples += element[element.length - 1];
                        numTrainSamples++;
                    } else {
                        numPosDevSamples += element[element.length - 1];
                        numDevSamples++;
                    }
                }
            }
            numNegTrainSamples = numTrainSamples - numPosTrainSamples;
            numNegDevSamples = numDevSamples - numPosDevSamples;
        }

        public void trainProb() {
            double[] featureGivenSpam = new double[58];
            double[] featureGivenNotSpam = new double[58];
            Arrays.fill(featureGivenSpam, 0);
            Arrays.fill(featureGivenNotSpam, 0);
            for(int i = 0; i < groups.length; i++) {
                if(testSetIndex == i) continue;
                for(int j = 0; j < groups[i].length; j++) {
                    double[] element = groups[i][j];
                    if(element[0] == -1) {
                        break;
                    }
                    boolean isSpam = element[element.length-1] == 1 ? true : false;
                    for(int k = 0; k < element.length; k++) {
                        if(element[k]<=elementAverages[k] && isSpam) {
                            featureGivenSpam[k]++;
                        }
                        if(element[k]<=elementAverages[k] && !isSpam) {
                            featureGivenNotSpam[k]++;
                        }
                    }

                }
            }

            for(int i = 0; i < featureGivenSpam.length; i++) {
                featureGivenSpam[i] = featureGivenSpam[i] / ((double) numPosTrainSamples);
                featureGivenNotSpam[i] = featureGivenNotSpam[i] / ((double) numNegTrainSamples);
                featureProb[i][0] = featureGivenSpam[i];
                featureProb[i][1] = 1 - featureProb[i][0];
                featureProb[i][2] = featureGivenNotSpam[i];
                featureProb[i][3] = 1 - featureProb[i][2];
            }
            for(int i = 0 ; i < featureProb.length; i++) {
                for(int j= 0; j < featureProb[i].length; j++) {
                    if(featureProb[i][j] == 1) featureProb[i][j]-=.0014;
                    if(featureProb[i][j] == 0) featureProb[i][j]+=.0014;
                }
            }
        }

        public String getProbabilityTable() {
            StringBuilder s = new StringBuilder();

            for (int i = 0; i < featureProb.length; i++) {
                for(int j = 0; j < 4; j++) {
                    s.append(featureProb[i][j] + ",");

                }
                if(i == featureProb.length - 1) {
                    s.deleteCharAt(s.length() -1);
                }
//                s.append("\n");
            }
            return s.toString();
        }

        public String getSampleInfo() {
            return numPosTrainSamples + "\t\t\t" + numNegTrainSamples + "\t\t\t" + numPosDevSamples + "\t\t\t" + numNegDevSamples;
        }

        public void testData() {
            double featureProductSpam = 1;
            double featureProductNotSpam = 1;
            int falsePos = 0;
            int truePos = 0;
            int falseNeg = 0;
            int trueNeg = 0;
            double[][] elementList = groups[testSetIndex];
            for(int i = 0; i < elementList.length; i++) {
                double[] element = elementList[i];
                if(element[0] == -1) {
                    break;
                }
                double[][] probLoader = new double[element.length][2];

                boolean isSpam = element[element.length-1] == 1 ? true : false;
                for(int j = 0; j < element.length; j++) {
                    if(element[j]<=elementAverages[j]) {
                        probLoader[j][0] = featureProb[j][0];
                        probLoader[j][1] = featureProb[j][2];
                    } else {
                        probLoader[j][0] = featureProb[j][1];
                        probLoader[j][1] = featureProb[j][3];
                    }
                }
                double yPredSpam = 1;
                double yPredNotSpam = 1;
                for(int j = 0; j < probLoader.length - 1; j++) {

                    yPredSpam *= probLoader[j][0];
                    yPredNotSpam *= probLoader[j][1];

                }

                yPredSpam *= ( (double) numPosTrainSamples / numTrainSamples);
                yPredNotSpam *= ( (double) numNegTrainSamples / numTrainSamples);

                if(yPredSpam > yPredNotSpam) {
                    truePos += isSpam ? 1 : 0;
                    falsePos += !isSpam ? 1 : 0;
                } else {
                    trueNeg += !isSpam ? 1 : 0;
                    falseNeg += isSpam ? 1 : 0;
                }
            }
            FP = (double) falsePos / (trueNeg + falsePos);
            FN = (double) falseNeg / (falseNeg + truePos);
            ERR = (double) (falseNeg + falsePos) / (truePos + trueNeg + falsePos + falseNeg);
//            System.out.print("truePos: " + truePos + "\t FalsePos: " + falsePos);
//            System.out.println("\t trueNeg: " + trueNeg + "\t FalseNeg: " + falseNeg);
//            System.out.println("% Correct: " + (double) (truePos + trueNeg) / (truePos + trueNeg + falsePos + falseNeg));
            CORR = (double) (truePos + trueNeg) / (truePos + trueNeg + falsePos + falseNeg);
            System.out.println("Fold " + (groups.length - testSetIndex) + ", FP: " + FP + ", FN: " + FN + ", ERR: " + ERR);
        }
    }
}
