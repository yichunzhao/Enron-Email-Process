import java.util.List;

public interface IMailInfo {
    List<String> getTo();    // recipients ( email addresses)

    String getFrom();        // sender ( email address)

    String getSubject();    // subject line

    java.util.Date getTime();        // The sent time of the email

    String pretty();        // En readable representation of the data in a MailInfo

}
