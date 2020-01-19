import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.toList;


public class MailHandler implements IMailHandler {

    //internal data structure to store MailInfo.
    Map<String,List<IMailInfo>> internalMails = new HashMap<>();
    List<IMailInfo> list = new ArrayList<>(100);

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
        return null;
    }

    public List<IMailInfo> sample(Integer nSamples) {
        return null;
    }

    private void loadEmails(Path rootDir, Integer maxMails) {
        //traverse the directory tree.
        try (Stream<Path> allPaths = Files.walk(rootDir)) {
            allPaths.filter(p -> Files.isRegularFile(p) && p.toString().endsWith("_"))
                    .limit(maxMails).peek(path -> System.out.println(path))
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
            Optional.ofNullable(parsed).ifPresent(p -> list.add(p));
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Path: " + path);
            System.out.println(e.getMessage());
        }

        return lines;
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
                        extractMessageId(line);
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

    public int listSize() {
        return list.size();
    }

    public List<IMailInfo> getMails() {
        return list;
    }

    public static void main(String[] args) {
        Instant start = Instant.now();
        Path rootDir = Paths.get("C:\\Users\\zhaoy\\Downloads\\enron_mail_20110402\\enron_mail_20110402\\maildir");
        MailHandler mailHandler = new MailHandler(rootDir);
        mailHandler.doImport(3);
        Instant end = Instant.now();

        System.out.println("Time cost: " + Duration.between(start, end).toMillis());
        System.out.println(mailHandler.listSize());
        long num = mailHandler.getMails().stream().filter(x -> x.getTo().size() > 1).count();
        System.out.println(num);
    }

}
