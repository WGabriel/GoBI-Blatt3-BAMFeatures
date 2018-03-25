
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.TreeSet;

import net.sf.samtools.*;

public class Runner {

    public static void main(String[] args) {
        File gtf = null;
        File bam = null;
        boolean frstrand = false;
        File output = null;

        if (args.length < 6 || args.length > 8) {
            System.err.println("Required 6 or 8 parameters. Found: " + args.length);
            String consoleParameters = "";
            for (int i = 0; i < args.length; i++) {
                consoleParameters += " args[" + i + "]=" + args[i];
            }
            System.err.println("ConsoleParameters:" + consoleParameters);
        } else {
            // Console parameters are correct!
            for (int i = 0; i < args.length; i = i + 2) {
                switch (args[i]) {
                    case "-gtf":
                        gtf = new File(args[i + 1]);
                        continue;
                    case "-bam":
                        bam = new File(args[i + 1]);
                        continue;
                    case "-frstrand":
                        frstrand = (Boolean.parseBoolean(args[i + 1]));
                        continue;
                    case "-o":
                        output = new File(args[i + 1]);
                        continue;
                }
                // System.out.println("|User Input| "+i+args[i]);
            }
        }
        System.out.println("|UserInput|\ngtf:\t" + gtf.getAbsolutePath() + "\nbam:\t" + bam.getAbsolutePath()
                + "\nfrstrand:\t" + frstrand + "\noutput:\t" + output.getAbsolutePath());

        TreeSet<GeneRegion> geneRegions = parseGenesFromGtf(gtf);

        SAMFileReader sam_reader = new SAMFileReader(bam, false);
        sam_reader.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        Iterator<SAMRecord> it = sam_reader.iterator();
        HashMap<String, SAMRecord> samRecords = new HashMap<String, SAMRecord>();

        LinkedHashSet<String> result = new LinkedHashSet<String>();

        int counter = 0;
        int printToConsole = 5;
        // printToConsole = Integer.MAX_VALUE;

        while (it.hasNext()) {
            SAMRecord sr = it.next();
            counter++;
            if (printToConsole > 0) {
                // method getReadName(), getMateUnmappedFlag, getMateAlignmentStart()

                printToConsole--;
                System.out.println("######################");
                System.out.println("readName: " + sr.getReadName());
                System.out.println("getNotPrimaryAlignmentFlag: " + sr.getNotPrimaryAlignmentFlag());
                System.out.println("getReferenceName: " + sr.getReferenceName());
                System.out.println("getReadPairedFlag: " + sr.getReadPairedFlag());
                System.out.println("getReadNegativeStrandFlag: " + sr.getReadNegativeStrandFlag());
                System.out.println("getFirstOfPairFlag: " + sr.getFirstOfPairFlag());
                System.out.println("getMateUnmappedFlag: " + sr.getMateUnmappedFlag());
                System.out.println("getMateReferenceName: " + sr.getMateReferenceName());
                System.out.println("getMateAlignmentStart:" + sr.getMateAlignmentStart());
                System.out.println("getReferenceIndex:" + sr.getReferenceIndex());
                System.out.println("getReadString:" + sr.getReadString());
                System.out.println("getSAMString:" + sr.getSAMString());
                System.out.println("start: " + sr.getAlignmentStart() + " end: " + sr.getAlignmentEnd());
            }

            if (sr.getNotPrimaryAlignmentFlag()) {
                // Ignore, if read is secondary
                System.err.println("Ignored: " + sr.getReadName() + " (secondary)");
            } else if (sr.getReadUnmappedFlag()) {
                // Ignore, if supplementary or unmapped
                System.err.println("Ignored: " + sr.getReadName() + " (supplementary or unmapped)");
            } else if (sr.getMateUnmappedFlag()) {
                // Ignore, if mate unmapped
                System.err.println("Ignored: " + sr.getReadName() + " (mate unmapped)");
            } else if (sr.getReadNegativeStrandFlag() == sr.getMateNegativeStrandFlag()) {
                // Ignore, if mate is on same strand
                System.err.println("Ignored: " + sr.getReadName() + " (mate is on same strand)");
            } else if (!sr.getReferenceName().equals(sr.getMateReferenceName())) {
                // Ignore, if mate is not on same chromosome
                System.err.println("Ignored: " + sr.getReadName() + " (mate is on not on same chromosome)");
            } else {
                // A read pair is intergenic, if no gene is contained between its first base and
                // its last base
                boolean isIntergenic = true;
                for (GeneRegion geneRegion : geneRegions) {
                    // System.out.println( "Start: " + geneRegion.start + " End: " + geneRegion.end
                    // + " ID: " + geneRegion.id);
                    // System.out.println("GeneRegions.size:" + geneRegions.size());
                    if (sr.getAlignmentStart() <= geneRegion.start && sr.getAlignmentEnd() >= geneRegion.end) {
                        isIntergenic = false;
                        break;
                    }
                }
                // Pre-check: Ignore, if read pair is intergenic
                if (!isIntergenic) {
                    String resultLine = sr.getReadName();
                    result.add(resultLine);

                    samRecords.put(sr.getReadName(), sr);
                    // System.out.println("Put SAMRecord: " + sr.getReadName());
                }
            }
        }

        HashMap<String, String> readPairs = new HashMap<String, String>();
        for (String readName : samRecords.keySet()) {
            for (String readName2 : samRecords.keySet()) {
                if (samRecords.get(readName).getMateAlignmentStart() == samRecords.get(readName2).getAlignmentStart()) {
                    readPairs.put(readName, readName2);
                    System.out.println("Matching mates found! readName1: " + readName + " alignmentStart: "
                            + samRecords.get(readName).getAlignmentStart() + " mateAlignmentStart: "
                            + samRecords.get(readName).getMateAlignmentStart() + " firstOfPairFlag: "
                            + samRecords.get(readName).getFirstOfPairFlag() + " || readName2: " + readName2
                            + " alignmentStart2: " + samRecords.get(readName2).getAlignmentStart()
                            + " mateAlignmentStart2: " + samRecords.get(readName2).getMateAlignmentStart()
                            + " firstOfPairFlag: " + samRecords.get(readName2).getFirstOfPairFlag());
                    break;
                } else {
                    continue;
                }
            }
            if (!readPairs.containsKey(readName)) {
                System.err.println("The mate for readName: " + readName + " (mateAlignmentStart: "
                        + samRecords.get(readName).getMateAlignmentStart() + ") was NOT found.");
            }
        }

        System.out.println("Begin: write  bam.annot.");
        try {
            // Create or overwrite new file
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(output + "/bam.annot", false)));
            for (String string : result) {
                out.println(string);
                counter++;
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finished: write  bam.annot.");

        System.out.println("samRecords.size()=" + samRecords.size());
        System.out.println("Total reads: " + counter);
        sam_reader.close();
        System.out.println("End of main method.");
    }

    public static TreeSet<GeneRegion> parseGenesFromGtf(File gtfInput) {
        TreeSet<GeneRegion> result = new TreeSet<GeneRegion>();
        // int counter = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(gtfInput));
            String line = null;
            System.out.println("Begin: Parsing gtf file.");
            while ((line = br.readLine()) != null) {
                // ignore comments (beginning with "#")
                if (!line.startsWith("#")) {
                    // For every line in input
                    String[] tabSeparated = line.split("\\t");
                    // String seqname = tabSeparated[0];
                    String feature = tabSeparated[2];
                    String start = tabSeparated[3];
                    String end = tabSeparated[4];
                    // String score = tabSeparated[5];
                    // String strand = tabSeparated[6];
                    // String frame = tabSeparated[7];
                    String attribute = tabSeparated[8];
                    if (feature.equals("gene")) {
                        String gene_id = "";
                        String[] attributeSeparated = attribute.split(";");
                        // search in attributeSeparated for parameters
                        for (int i = 0; i < attributeSeparated.length; i++) {
                            if (attributeSeparated[i].contains("gene_id")) {
                                gene_id = attributeSeparated[i].substring(attributeSeparated[i].indexOf("\"") + 1,
                                        attributeSeparated[i].lastIndexOf("\""));
                                break;
                            }
                        }
                        GeneRegion gr = new GeneRegion(gene_id, Integer.parseInt(start) - 1, Integer.parseInt(end));
                        // counter++;
                        result.add(gr);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // System.out.println("|parseGenesFromGtf| " + counter);
        return result;
    }

}