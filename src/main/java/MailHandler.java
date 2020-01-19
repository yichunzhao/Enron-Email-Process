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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


public class MailHandler implements IMailHandler {

    //internal data structure to store MailInfo.

    //a list mail received on the same date.
    //List<IMailInfo> listMails = new ArrayList<>(100);

    //a tree map sorted by date
    //Map<Date, List<IMailInfo>> mailSameDate = new TreeMap<>();

    //Map<From, List>
    //Map<String, Map<Date, List<IMailInfo>>> mails = new HashMap<>();

    //Map<String, List<IMailInfo>> allMails = new HashMap<>();
    Map<String, Set<IMailInfo>> allMails = new HashMap<>();

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

    private class InvalidEmailException extends Exception {
        public InvalidEmailException(String msg) {
            super(msg);
        }
    }

    private EmailFieldState currentState;

    private Path rootDir;

    public MailHandler(Path rootDir) {
        this.rootDir = rootDir;
    }

    public void doImport(Integer maxMails) {
        if (maxMails < 0) throw new IllegalArgumentException("Max mails num must be positive.");
        if (maxMails > 100_000) throw new IllegalArgumentException("The max mail number is too big");

        //loading the raw emails.
        loadEmails(rootDir, maxMails);
    }

    public List<IMailInfo> search(String emailAddress, Date maxTime) {
        if (!allMails.containsKey(emailAddress))
            throw new IllegalArgumentException("the email address is not existed");

        Set<IMailInfo> found = allMails.get(emailAddress);
        return found.stream().filter(x -> x.getTime().before(maxTime)).collect(Collectors.toList());
    }

    public List<IMailInfo> sample(Integer nSamples) {
        return null;
    }

    private void loadEmails(Path rootDir, Integer maxMails) {
        //traverse the directory tree.
        try (Stream<Path> allPaths = Files.walk(rootDir)) {
            allPaths.filter(p -> Files.isRegularFile(p) && p.toString().endsWith("_"))
                    .limit(maxMails)
                    //.peek(path -> System.out.println(path))
                    .forEach(p -> readEmailLines(p));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //read an email in lines from its path.
    private List<String> readEmailLines(Path path) {
        List<String> lines = new ArrayList<>();

        try {
            lines = Files.readAllLines(path, StandardCharsets.US_ASCII);

            IMailInfo parsed = parsingEmail(lines);
            //Optional.ofNullable(parsed).ifPresent(p -> list.add(p));
            //Optional.ofNullable(parsed).ifPresent(p -> allMails.put(p.getFrom(), p));
            Optional.ofNullable(parsed).ifPresent(p -> storeMailInternally(p));
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Path: " + path);
            System.out.println(e.getMessage());
        }

        return lines;
    }

    //storing a mail into the internal data structure.
    private void storeMailInternally(IMailInfo parsed) {
        if (this.allMails.containsKey(parsed.getFrom())) {
            allMails.get(parsed.getFrom()).add(parsed);
        } else {
            Set<IMailInfo> listSameFrom = new TreeSet<>(Comparator.comparing(IMailInfo::getTime).reversed());
            listSameFrom.add(parsed);
            allMails.put(parsed.getFrom(), listSameFrom);
        }
    }

    //parsing a single mail here, which consists of lines
    private IMailInfo parsingEmail(List<String> lines) {
        //initial email field state.
        currentState = EmailFieldState.START;

        //empty email data model
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

                //skip this mail
                break;
            }
        }

        return mailInfo;
    }

    private String extractMessageId(String line) throws InvalidEmailException {
        if (!line.startsWith(EmailFieldState.ID.filedName)) throw new InvalidEmailException("missing email id field");
        String[] results = line.split(":");
        currentState = EmailFieldState.TIME;

        return results[1].trim();
    }

    private String extractFrom(String line) throws InvalidEmailException {
        if (!line.startsWith(EmailFieldState.FROM.filedName)) throw new InvalidEmailException("missing From field");
        String[] results = line.split(":");

        //next state
        currentState = EmailFieldState.TO;

        return results[1].trim();
    }

    private List<String> extractTo(String line) throws InvalidEmailException {
        if (!line.startsWith(EmailFieldState.TO.filedName)) throw new InvalidEmailException("missing To field");
        String[] results = line.split("[:,]");

        List<String> tos = Stream.of(results)
                .map(e -> e.trim())
                .filter(e -> !e.equals("To"))
                .collect(toList());

        //next state.
        currentState = EmailFieldState.SUBJECT;

        return tos;
    }

    private String extractSubject(String line) throws InvalidEmailException {
        if (!line.startsWith(EmailFieldState.SUBJECT.filedName))
            throw new InvalidEmailException("missing Subject field");
        String[] results = line.split(":");

        //next state
        currentState = EmailFieldState.END;

        return results[1].trim();
    }

    private Date extractTime(String line) throws InvalidEmailException, ParseException {
        if (!line.startsWith(EmailFieldState.TIME.filedName)) throw new InvalidEmailException("missing Date field");
        String[] results = line.split(EmailFieldState.TIME.filedName);

        String time = Optional.ofNullable(results[1])
                .orElseThrow(() -> new IllegalStateException("missing date info.")).trim();

        //next state
        currentState = EmailFieldState.FROM;

        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z (z)").parse(time);
    }


    public static void main(String[] args) {
        Instant start = Instant.now();
        //load all enron mails from a root dir, and converting into an internal data structure.
        Path rootDir = Paths.get("C:\\Users\\zhaoy\\Downloads\\enron_mail_20110402\\enron_mail_20110402\\maildir");
        MailHandler mailHandler = new MailHandler(rootDir);
        mailHandler.doImport(100);
        Instant end = Instant.now();

        System.out.println("Time cost: " + Duration.between(start, end).toMillis());

        //search by email address(From) and MaxTime
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


    }

}
