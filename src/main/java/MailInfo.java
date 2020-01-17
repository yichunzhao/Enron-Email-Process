import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MailInfo implements IMailInfo {
    private List<String> to = new ArrayList<String>();
    private String from;
    private String subject;
    private Date time;

    public MailInfo(){

    }

    public MailInfo(List<String> to, String from, String subject, Date time) {
        this.to = to;
        this.from = from;
        this.subject = subject;
        this.time = time;
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
        sb.append(" To: "+ to.toString());
        sb.append(" Subject: " + subject);
        sb.append(" Time: " + time);
        return sb.toString();
    }
}
