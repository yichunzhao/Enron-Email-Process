import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * The MailInfo class implements the IMailInfo interface and represents an email with various attributes.
 */
public class MailInfo implements IMailInfo {
    private String messageId;
    private List<String> to = new ArrayList<String>();
    private String from;
    private String subject;
    private Date time;

    /**
     * Default constructor for MailInfo.
     */
    public MailInfo() {

    }

    /**
     * Sets the message ID of the email.
     *
     * @param messageId the message ID to set.
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Sets the recipient email addresses.
     *
     * @param to the list of recipient email addresses to set.
     */
    public void setTo(List<String> to) {
        this.to = to;
    }

    /**
     * Sets the sender's email address.
     *
     * @param from the sender's email address to set.
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Sets the subject of the email.
     *
     * @param subject the subject to set.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Sets the time the email was sent.
     *
     * @param time the time to set.
     */
    public void setTime(Date time) {
        this.time = time;
    }

    /**
     * Gets the recipient email addresses.
     *
     * @return the list of recipient email addresses.
     */
    public List<String> getTo() {
        return this.to;
    }

    /**
     * Gets the sender's email address.
     *
     * @return the sender's email address.
     */
    public String getFrom() {
        return this.from;
    }

    /**
     * Gets the subject of the email.
     *
     * @return the subject of the email.
     */
    public String getSubject() {
        return this.subject;
    }

    /**
     * Gets the time the email was sent.
     *
     * @return the time the email was sent.
     */
    public Date getTime() {
        return this.time;
    }

    /**
     * Returns a formatted string representation of the email.
     *
     * @return a formatted string representation of the email.
     */
    public String pretty() {
        StringBuilder sb = new StringBuilder();
        sb.append("From: " + from);
        sb.append(" To: " + to.toString());
        sb.append(" Subject: " + subject);
        sb.append(" Time: " + time);
        return sb.toString();
    }

    /**
     * Checks if this MailInfo object is equal to another object.
     *
     * @param o the object to compare with.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MailInfo mailInfo = (MailInfo) o;
        return messageId.equals(mailInfo.messageId) &&
                to.equals(mailInfo.to) &&
                from.equals(mailInfo.from) &&
                subject.equals(mailInfo.subject) &&
                time.equals(mailInfo.time);
    }

    /**
     * Returns the hash code of this MailInfo object.
     *
     * @return the hash code of this MailInfo object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(messageId, to, from, subject, time);
    }
}