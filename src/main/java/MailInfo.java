import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MailInfo implements IMailInfo {
    private String messageId;
    private List<String> to = new ArrayList<String>();
    private String from;
    private String subject;
    private Date time;

    public MailInfo() {

    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public List<String> getTo() {
        return this.to;
    }

    public String getFrom() {
        return this.from;
    }

    public String getSubject() {
        return this.subject;
    }

    public Date getTime() {
        return this.time;
    }

    public String pretty() {
        StringBuilder sb = new StringBuilder();
        sb.append("From: " + from);
        sb.append(" To: " + to.toString());
        sb.append(" Subject: " + subject);
        sb.append(" Time: " + time);
        return sb.toString();
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(messageId, to, from, subject, time);
    }
}
