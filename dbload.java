import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class dbload {

    // Minimum possible number of bytes a record will occupy
    static final int MIN_RECORD_SIZE = 50;

    String inputDataFile = "";
    Integer pageSize = -1;

    /**
     * @param args
     *            command line arguments
     */
    public static void main(String[] args) {
        dbload dbload = new dbload();
        // Read and verify command line arguments
        dbload.readCommandLineArgs(args);
        if (!dbload.verifyCommandLineArgs()) {
            System.exit(0);
        }
        // Setup I/O files
        File outputFile = new File("heap." + dbload.pageSize.toString());
        File artistsFile = new File(dbload.inputDataFile);

        // Read input from artistsFile and write to outputFile
        try {
            dbload.transferRecordsToHeap(outputFile, artistsFile);
        } catch (FileNotFoundException e) {
            System.err.println("File not found exception, check that input file exists");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("IOException");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * @param outputFile
     *            Heap file to write records to
     * @param inputFile
     *            CSV file to read records from
     */
    private void transferRecordsToHeap(File outputFile, File inputFile) throws FileNotFoundException, IOException {
        FileReader fileReader = new FileReader(inputFile);
        BufferedReader inputFileBuffer = new BufferedReader(fileReader);
        if (outputFile.exists()) {
            System.err.println("Output heap file already exists, removing.");
            outputFile.delete();
        }
        FileOutputStream outputStream = new FileOutputStream(outputFile, true);

        byte[] currentPage = new byte[this.pageSize];
        // pageHeader array is larger than likely neccecary but memory usage shouldn't
        // be too high for normal page sizes.
        int[] pageHeader = new int[this.pageSize / dbload.MIN_RECORD_SIZE];
        int pageCount = 0;
        int currentPageSize = 0;
        int pageHeaderSize = 2; // First two numbers in pageHeader are needed for pointers
        Arrays.fill(currentPage, (byte) 0);
        int totalRecordsRead = 0;

        String readString = "";
        // Remove 4 header lines from CSV.
        for (int i = 0; i < 5; i++) {
            readString = inputFileBuffer.readLine();
        }
        while (readString != null) {
            ArtistRecord record = new ArtistRecord(readString);
            byte[] recordBytes = record.getRecordAsBytes();
            totalRecordsRead++;

            // Make new page if current record won't fit on current page
            if (currentPageSize + ((pageHeaderSize + 1) * 4) + recordBytes.length > this.pageSize) {
                addHeaderToPage(currentPage, pageHeader, currentPageSize, pageHeaderSize);
                // Write page to file
                this.writePage(outputStream, currentPage, pageCount);
                pageCount++;
                // Reset temp and per page variables
                currentPageSize = 0;
                pageHeaderSize = 2;
                Arrays.fill(currentPage, (byte) 0);
            }

            // Add record to current page
            Util.arrayMerge(recordBytes, currentPage, recordBytes.length, 0, currentPageSize);
            // Add pointers to header
            pageHeader[pageHeaderSize] = currentPageSize;
            pageHeaderSize++;
            currentPageSize += recordBytes.length;
            readString = inputFileBuffer.readLine();
        }

        // Write last page
        addHeaderToPage(currentPage, pageHeader, currentPageSize, pageHeaderSize);
        this.writePage(outputStream, currentPage, pageCount);

        inputFileBuffer.close();
        outputStream.close();

        System.out.println(String.format("Wrote %s records to %s pages", totalRecordsRead, pageCount));
    }

    /**
     * Adds a page header to the end of a page.
     * 
     * @param page
     *            array of bytes representing the current page without a header
     * @param pageHeader
     *            array of integers to representing the page header
     * @param currentPageSize
     *            number of bytes currently in page
     * @param pageHeaderSize
     *            number of integers in the header
     */
    private static void addHeaderToPage(byte[] page, int[] pageHeader, int currentPageSize, int pageHeaderSize) {
        pageHeader[0] = currentPageSize;
        pageHeader[1] = pageHeaderSize - 2;
        // Add header to page
        byte[] pageHeaderBytes = new byte[pageHeaderSize * 4];
        for (int i = 0; i < pageHeaderSize; i++) {
            Util.arrayMerge(Util.intToBytes(pageHeader[pageHeaderSize - 1 - i]), pageHeaderBytes, 4, 0, i * 4);
        }
        Util.arrayMerge(pageHeaderBytes, page, pageHeaderBytes.length, 0,
                page.length - pageHeaderBytes.length);
    }

    /**
     * Sets dbload variables as needed
     * 
     * @param args
     *            Arguments to read
     */
    private void readCommandLineArgs(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case ("-p"):
                    case ("--pagesize"):
                        this.pageSize = Integer.valueOf(args[i + 1]);
                        i++;
                    default:
                        this.inputDataFile = args[i];

                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Expected an argument after a flag and didn't recieve it.");
        }
    }

    /**
     * Checks if the required dbload variables are set correctly, does not check if
     * files exist.
     * 
     * @return boolean true if arguments are set correctly, false if not.
     */
    private boolean verifyCommandLineArgs() {
        if (this.inputDataFile == "") {
            System.err.println("Input data file not specified");
            return false;
        } else if (this.pageSize == -1) {
            System.err.println("Pagesize not specified.");
            return false;
        }
        return true;
    }

    /**
     * Writes a 'page' to outputFile.
     * 
     * @param outputFile
     *            File to write to.
     * @param bytes
     *            Bytes to write.
     */
    private boolean writePage(FileOutputStream outputFile, byte[] bytes, int pageNum) {
        boolean success = true;
        try {
            outputFile.write(bytes);
            outputFile.flush();
        } catch (IOException e) {
            System.err.println("Issue writing to output file.");
            success = false;
        }
        return success;
    }

}
