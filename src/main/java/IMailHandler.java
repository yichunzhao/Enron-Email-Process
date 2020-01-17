import java.util.List;

public interface IMailHandler {

    void doImport(Integer maxMails);  // handle a relatively small number (<100k) of mails from the enron-dataset. (this involves parsing of text - you are allowed to skip strange emails.)

    List<IMailInfo> search(String emailAddress, java.util.Date maxTime); // Search in the handled mails for all mails
    // sent OR received by an email-addresss. The result List should be d sorted by descending time.
    // The time of the newest result should be smaller than maxTime.

    List<IMailInfo> sample(Integer nSamples);  // return a number of IMailInfo that are handled. Order is not required.

}
