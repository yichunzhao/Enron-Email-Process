## Getting Started

### Prerequisites

- Java 8 or higher
- Maven 3.6.0 or higher

### Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/yichunzhao/mailhandler.git
    cd mailhandler
    ```

2. Build the project using Maven:
    ```sh
    mvn clean install
    ```

### Usage

1. **Import Emails**:
    ```java
    Path rootDir = Paths.get("path/to/email/directory");
    MailHandler mailHandler = new MailHandler(rootDir);
    mailHandler.doImport(100);
    ```

2. **Search Emails**:
    ```java
    String target = "example@example.com";
    Date maxDate = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss").parse("Fri Aug 25 2000 12:00:00");
    List<IMailInfo> results = mailHandler.search(target, maxDate);
    results.forEach(e -> System.out.println(e.pretty()));
    ```

3. **Sample Emails**:
    ```java
    List<IMailInfo> samples = mailHandler.sample(20);
    samples.forEach(e -> System.out.println(e.pretty()));
    ```

### Running Tests

To run the tests, use the following Maven command:
```sh
mvn test
