import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * MailHandler class implements the IMailHandler interface to handle email operations.
 */
public class MailHandler implements IMailHandler {

    // Internal data structure to store MailInfo.
    Map<String, Set<IMailInfo>> allMails = new HashMap<>();

    /**
     * Enum representing the state of email fields during parsing.
     */
    private enum EmailFieldState {
        START("start"),
        ID("Message-ID:"),
        FROM("From:"),
        TO("To:"),
        SUBJECT("Subject:"),
        TIME("Date:"),
        END("end");

        private String filedName;

        EmailFieldState(String value) {
            this.filedName = value;
        }
    }

    /**
     * Exception class for invalid email format.
     */
    private class InvalidEmailException extends Exception {
        public InvalidEmailException(String msg) {
            super(msg);
        }
    }

    private EmailFieldState currentState;

    private Path rootDir;

    /**
     * Constructor for MailHandler.
     *
     * @param rootDir the root directory containing email files.
     */
    public MailHandler(Path rootDir) {
        this.rootDir = rootDir;
    }

    /**
     * Imports emails from the root directory up to a maximum number.
     *
     * @param maxMails the maximum number of emails to import.
     */
    public void doImport(Integer maxMails) {
        if (maxMails < 0) throw new IllegalArgumentException("Max mails num must be positive.");
        if (maxMails > 100_000) throw new IllegalArgumentException("The max mail number is too big");

        // Loading the raw emails.
        loadEmails(rootDir, maxMails);
    }

    /**
     * Searches for emails received by a specific email address before a given time.
     *
     * @param emailAddress the email address to search for.
     * @param maxTime the maximum time for the emails.
     * @return a list of IMailInfo objects matching the criteria.
     */
    public List<IMailInfo> search(String emailAddress, Date maxTime) {
        if (!allMails.containsKey(emailAddress))
            throw new IllegalArgumentException("the email address is not existed");

        Set<IMailInfo> found = allMails.get(emailAddress);
        return found.stream().filter(x -> x.getTime().before(maxTime)).collect(toList());
    }

    /**
     * Samples a specified number of emails.
     *
     * @param nSamples the number of emails to sample.
     * @return a list of sampled IMailInfo objects.
     */
    public List<IMailInfo> sample(Integer nSamples) {
        List<IMailInfo> samples = allMails.values().stream().flatMap(Collection::stream).limit(nSamples).collect(toList());
        if (nSamples > samples.size()) throw new IllegalArgumentException("nSamples > the size of all mails.");
        return samples;
    }

    /**
     * Loads emails from the specified directory.
     *
     * @param rootDir the root directory containing email files.
     * @param maxMails the maximum number of emails to load.
     */
    private void loadEmails(Path rootDir, Integer maxMails) {
        // Traverse the directory tree.
        try (Stream<Path> allPaths = Files.walk(rootDir)) {
            allPaths.filter(p -> Files.isRegularFile(p) && p.toString().endsWith("_"))
                    .limit(maxMails)
                    //.peek(path -> System.out.println(path))
                    .forEach(p -> readEmailLines(p));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads an email in lines from its path.
     *
     * @param path the path of the email file.
     * @return a list of lines in the email.
     */
    private List<String> readEmailLines(Path path) {
        List<String> lines = new ArrayList<>();

        try {
            lines = Files.readAllLines(path, StandardCharsets.US_ASCII);

            IMailInfo parsed = parsingEmail(lines);
            Optional.ofNullable(parsed).ifPresent(p -> storeMailInternally(p));
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Path: " + path);
            System.out.println(e.getMessage());
        }

        return lines;
    }

    /**
     * Stores a mail into the internal data structure.
     *
     * @param parsed the parsed IMailInfo object.
     */
    private void storeMailInternally(IMailInfo parsed) {
        if (this.allMails.containsKey(parsed.getFrom())) {
            allMails.get(parsed.getFrom()).add(parsed);
        } else {
            Set<IMailInfo> listSameFrom = new TreeSet<>(Comparator.comparing(IMailInfo::getTime).reversed());
            listSameFrom.add(parsed);
            allMails.put(parsed.getFrom(), listSameFrom);
        }
    }

    /**
     * Parses a single mail from a list of lines.
     *
     * @param lines the lines of the email.
     * @return the parsed IMailInfo object.
     */
    private IMailInfo parsingEmail(List<String> lines) {
        // Initial email field state.
        currentState = EmailFieldState.START;

        // Empty email data model
        MailInfo mailInfo = new MailInfo();

        for (String line : lines) {
            if (currentState.equals(EmailFieldState.END)) break;

            try {
                switch (currentState) {
                    case START:
                        mailInfo.setMessageId(extractMessageId(line));
                        break;
                    case TIME:
                        mailInfo.setTime(extractTime(line));
                        break;
                    case FROM:
                        mailInfo.setFrom(extractFrom(line));
                        break;
                    case TO:
                        mailInfo.setTo(extractTo(line));
                        break;
                    case SUBJECT:
                        mailInfo.setSubject(extractSubject(line));
                        break;
                }
            } catch (InvalidEmailException | ParseException e) {
                mailInfo = null;

                // Skip this mail
                break;
            }
        }

        return mailInfo;
    }

    /**
     * Extracts the message ID from a line.
     *
     * @param line the line containing the message ID.
     * @return the extracted message ID.
     * @throws InvalidEmailException if the message ID field is missing.
     */
    private String extractMessageId(String line) throws InvalidEmailException {
        if (!line.startsWith(EmailFieldState.ID.filedName)) throw new InvalidEmailException("missing email id field");
        String[] results = line.split(":");
        currentState = EmailFieldState.TIME;

        return results[1].trim();
    }

    /**
     * Extracts the sender's email address from a line.
     *
     * @param line the line containing the sender's email address.
     * @return the extracted sender's email address.
     * @throws InvalidEmailException if the From field is missing.
     */
    private String extractFrom(String line) throws InvalidEmailException {
        if (!line.startsWith(EmailFieldState.FROM.filedName)) throw new InvalidEmailException("missing From field");
        String[] results = line.split(":");

        // Next state
        currentState = EmailFieldState.TO;

        return results[1].trim();
    }

    /**
     * Extracts the recipient email addresses from a line.
     *
     * @param line the line containing the recipient email addresses.
     * @return a list of extracted recipient email addresses.
     * @throws InvalidEmailException if the To field is missing.
     */
    private List<String> extractTo(String line) throws InvalidEmailException {
        if (!line.startsWith(EmailFieldState.TO.filedName)) throw new InvalidEmailException("missing To field");
        String[] results = line.split("[:,]");

        List<String> tos = Stream.of(results)
                .map(e -> e.trim())
                .filter(e -> !e.equals("To"))
                .collect(toList());

        // Next state.
        currentState = EmailFieldState.SUBJECT;

        return tos;
    }

    /**
     * Extracts the subject from a line.
     *
     * @param line the line containing the subject.
     * @return the extracted subject.
     * @throws InvalidEmailException if the Subject field is missing.
     */
    private String extractSubject(String line) throws InvalidEmailException {
        if (!line.startsWith(EmailFieldState.SUBJECT.filedName))
            throw new InvalidEmailException("missing Subject field");
        String[] results = line.split(":");

        // Next state
        currentState = EmailFieldState.END;

        return results[1].trim();
    }

    /**
     * Extracts the time from a line.
     *
     * @param line the line containing the time.
     * @return the extracted time as a Date object.
     * @throws InvalidEmailException if the Date field is missing.
     * @throws ParseException if the date format is invalid.
     */
    private Date extractTime(String line) throws InvalidEmailException, ParseException {
        if (!line.startsWith(EmailFieldState.TIME.filedName)) throw new InvalidEmailException("missing Date field");
        String[] results = line.split(EmailFieldState.TIME.filedName);

        String time = Optional.ofNullable(results[1])
                .orElseThrow(() -> new IllegalStateException("missing date info.")).trim();

        // Next state
        currentState = EmailFieldState.FROM;

        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z (z)").parse(time);
    }

    /**
     * Main method to demonstrate the functionality of MailHandler.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        Instant start = Instant.now();
        // Load all enron mails from a root dir, and converting into an internal data structure.
        Path rootDir = Paths.get("C:\\Users\\zhaoy\\Downloads\\enron_mail_20110402\\enron_mail_20110402\\maildir");
        MailHandler mailHandler = new MailHandler(rootDir);
        mailHandler.doImport(100);
        Instant end = Instant.now();

        System.out.println("Time cost: " + Duration.between(start, end).toMillis());

        System.out.println("+++ search by email address(From) and MaxTime +++");

        String target = "phillip.allen@enron.com";
        String time = "Fri Aug 25 2000 12:00:00";
        DateFormat parser = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss");
        Date maxDate = new Date();
        try {
            maxDate = parser.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        List<IMailInfo> results = mailHandler.search(target, maxDate);
        results.forEach(e-> System.out.println(e.pretty()));

        System.out.println("+++ sample nInteger +++");

        System.out.println("sampled: ");
        mailHandler.sample(20).forEach(e-> System.out.println(e.pretty()));
    }
}