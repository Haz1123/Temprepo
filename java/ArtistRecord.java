import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArtistRecord {
    public int wikiPageId;
    public Date birthDate;
    public Date deathDate;
    public String personName;
    public String birthPlace;
    public String field;
    public String genre;
    public String instrument;
    public String nationality;
    public String thumbnail;
    public String description;

    public ArtistRecord(String s) {
        String[] fields = s.split("\",\"");
        if (fields.length != 147) {
            System.err.println("Number of fields parsed incorrect");
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].replace("\"", "");
        }
        try {
            this.wikiPageId = Integer.parseInt(fields[133]);
        } catch (NumberFormatException e) {
            this.wikiPageId = -1;
        }
        this.personName = fields[1];
        this.birthDate = parseDateFromCsv(fields[23]);
        this.deathDate = parseDateFromCsv(fields[40]);
        this.field = fields[50];
        this.genre = fields[52];
        this.instrument = fields[62];
        this.nationality = fields[73];
        this.thumbnail = fields[124];
        this.description = fields[137];
    }

    private Date parseDateFromCsv(String s) {
        try {
            DateFormat df = DateFormat.getDateInstance();
            if (s.contains("{")) { // Check if is an array
                // Remove array brackets and take first value of array
                s = s.substring(1, s.lastIndexOf("}"));
                s = s.split("\\|")[0];
            }
            if (s.equals("NULL") || s.contains("--")) { // Known null date formats.
                return null;
            } else if (s.contains("T") || s.contains(":")) {
                s = s.split("T")[0]; // Get date only
                df = new SimpleDateFormat("yyyy-MM-dd");
            } else if (s.contains("-")) {
                df = new SimpleDateFormat("yyyy-MM-dd");
            } else if (s.contains("/")) {
                df = new SimpleDateFormat("dd/MM/yyyy");
            } else {
                System.err.println("Unknown date format for: " + s);
            }
            return df.parse(s);
        } catch (ParseException e) {
            System.err.println("Date parsing issue with date string: " + s);
            return null;
        }
    }

    public byte[] getRecordAsBytes() {
        byte[] stringBytes = getConcatStrings().getBytes();
        // Add 20 bytes for wikiPageId and dates.
        byte[] output = new byte[20 + stringBytes.length];

        Util.arrayMerge(Util.intToBytes(wikiPageId), output, 4, 0, 0);
        Util.arrayMerge(Util.dateToBytes(birthDate), output, 8, 0, 4);
        Util.arrayMerge(Util.dateToBytes(deathDate), output, 8, 0, 12);
        Util.arrayMerge(stringBytes, output, stringBytes.length, 0, 20);
        return output;
    }

    private String getConcatStrings() {
        StringBuilder output = new StringBuilder();
        for (String s : new String[] { personName, birthPlace, field, genre, instrument, nationality, thumbnail,
                description }) {
            output.append(s != null ? s.replace("$", "\\$") : ""); // Append field with dollar signs escaped using \$.
            output.append("$");
        }
        return output.toString();
    }

}
