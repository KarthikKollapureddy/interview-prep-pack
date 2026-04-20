# Spring Batch & ETL Processing — Interview Q&A

> 10 questions covering Job/Step architecture, Reader/Writer, chunk processing, partitioning, skip/retry  
> Priority: **P2** — Asked in FedEx/NPCI for batch data processing, report generation, migrations

---

### Q1. What is Spring Batch? When do you need it?

**Answer:**

**Spring Batch** = Framework for robust batch processing of large volumes of data.

**Use cases:**
```
✅ Report generation (daily sales reports, monthly statements)
✅ Data migration (import 10M records from CSV to database)
✅ ETL pipelines (Extract → Transform → Load)
✅ Bulk email / notifications (send to 1M users)
✅ File processing (parse and validate bank transactions)
✅ Data synchronization (sync between systems nightly)
✅ Billing / invoicing (process all charges at month-end)

Real examples:
  FedEx: Nightly batch to process all shipment tracking updates
  NPCI:  Daily settlement file processing (millions of UPI transactions)
  Bank:  EOD interest calculation for all accounts
```

**Why not just a for-loop?**
```
Batch for-loop:
  ❌ No restart/resume after failure (start from scratch)
  ❌ No skip/retry for bad records
  ❌ No monitoring (how far along? how many failed?)
  ❌ No parallel processing built-in
  ❌ No transaction management per chunk

Spring Batch:
  ✅ Restart from where it failed (job metadata)
  ✅ Skip bad records, retry transient failures
  ✅ Job monitoring via JobExplorer/JobRepository
  ✅ Partitioning for parallel processing
  ✅ Transaction per chunk (100 records at a time)
```

---

### Q2. Explain the Spring Batch architecture — Job, Step, Reader, Processor, Writer.

**Answer:**

```
Job
 └── Step 1 (e.g., "Read CSV file")
 │    ├── ItemReader    → reads one item at a time (CSV row, DB row, JSON record)
 │    ├── ItemProcessor → transforms/validates the item
 │    └── ItemWriter    → writes a chunk of items (batch INSERT)
 │
 └── Step 2 (e.g., "Generate report")
 │    └── Tasklet       → simple task (run SQL, delete temp files, send email)
 │
 └── Step 3 (e.g., "Archive files")
      └── Tasklet       → move files to archive directory

Chunk-oriented processing (the core model):
  ┌─────────────────────────────────────────────────────┐
  │ Chunk (size = 100)                                   │
  │                                                      │
  │  Read item-1 → Process item-1 ─┐                     │
  │  Read item-2 → Process item-2  ├── Write 100 items   │
  │  ...                           │   (batch INSERT)    │
  │  Read item-100 → Process item-100┘                    │
  │                                                      │
  │  COMMIT transaction                                  │
  └─────────────────────────────────────────────────────┘
  Repeat until all items are read
```

```java
@Configuration
@RequiredArgsConstructor
public class ImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;

    @Bean
    public Job importUserJob(Step importStep, Step notifyStep) {
        return new JobBuilder("importUserJob", jobRepository)
            .start(importStep)
            .next(notifyStep)
            .build();
    }

    @Bean
    public Step importStep(ItemReader<UserCsv> reader,
                           ItemProcessor<UserCsv, User> processor,
                           ItemWriter<User> writer) {
        return new StepBuilder("importStep", jobRepository)
            .<UserCsv, User>chunk(100, txManager)  // commit every 100 items
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skip(FlatFileParseException.class)
            .skipLimit(50)              // skip up to 50 bad records
            .retry(DeadlockLoserDataAccessException.class)
            .retryLimit(3)              // retry deadlocks 3 times
            .build();
    }

    @Bean
    public Step notifyStep() {
        return new StepBuilder("notifyStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                // Send completion email
                emailService.sendBatchComplete();
                return RepeatStatus.FINISHED;
            }, txManager)
            .build();
    }
}
```

---

### Q3. Built-in ItemReaders and ItemWriters.

**Answer:**

**Readers:**
```java
// 1. FlatFileItemReader — CSV/TSV files:
@Bean
public FlatFileItemReader<UserCsv> csvReader() {
    return new FlatFileItemReaderBuilder<UserCsv>()
        .name("userCsvReader")
        .resource(new ClassPathResource("users.csv"))
        .delimited()
        .names("name", "email", "role")
        .targetType(UserCsv.class)
        .linesToSkip(1)  // skip header
        .build();
}

// 2. JdbcCursorItemReader — database (cursor-based, memory efficient):
@Bean
public JdbcCursorItemReader<Order> dbReader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<Order>()
        .name("orderDbReader")
        .dataSource(dataSource)
        .sql("SELECT id, amount, status FROM orders WHERE status = 'PENDING'")
        .rowMapper(new BeanPropertyRowMapper<>(Order.class))
        .build();
}

// 3. JdbcPagingItemReader — database (page-based, restartable):
@Bean
public JdbcPagingItemReader<Order> pagingReader(DataSource dataSource) {
    return new JdbcPagingItemReaderBuilder<Order>()
        .name("orderPagingReader")
        .dataSource(dataSource)
        .selectClause("id, amount, status")
        .fromClause("orders")
        .whereClause("status = 'PENDING'")
        .sortKeys(Map.of("id", Order.ASCENDING))
        .pageSize(100)
        .rowMapper(new BeanPropertyRowMapper<>(Order.class))
        .build();
}

// 4. JsonItemReader — JSON files
// 5. StaxEventItemReader — XML files
// 6. JpaPagingItemReader — JPA entities
```

**Writers:**
```java
// 1. JdbcBatchItemWriter — batch INSERT (fastest for JDBC):
@Bean
public JdbcBatchItemWriter<User> dbWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<User>()
        .dataSource(dataSource)
        .sql("INSERT INTO users (name, email, role) VALUES (:name, :email, :role)")
        .beanMapped()
        .build();
}

// 2. FlatFileItemWriter — write to CSV:
@Bean
public FlatFileItemWriter<Report> csvWriter() {
    return new FlatFileItemWriterBuilder<Report>()
        .name("reportWriter")
        .resource(new FileSystemResource("output/report.csv"))
        .delimited()
        .names("orderId", "amount", "date")
        .headerCallback(writer -> writer.write("Order ID,Amount,Date"))
        .build();
}

// 3. JpaItemWriter — JPA persist
// 4. CompositeItemWriter — write to multiple destinations
```

---

### Q4. ItemProcessor — transformation and validation.

**Answer:**

```java
// Simple processor:
@Component
public class UserProcessor implements ItemProcessor<UserCsv, User> {

    @Override
    public User process(UserCsv csv) throws Exception {
        // Return null to SKIP this record (filter out):
        if (csv.getEmail() == null || csv.getEmail().isBlank()) {
            return null;  // filtered out, not written
        }

        // Transform:
        return User.builder()
            .name(csv.getName().trim())
            .email(csv.getEmail().toLowerCase())
            .role(mapRole(csv.getRole()))
            .createdAt(LocalDateTime.now())
            .build();
    }
}

// Chain multiple processors:
@Bean
public CompositeItemProcessor<UserCsv, User> compositeProcessor() {
    return new CompositeItemProcessorBuilder<UserCsv, User>()
        .delegates(
            new ValidationProcessor(),  // validate
            new TransformProcessor(),   // transform
            new EnrichmentProcessor()   // enrich with external data
        )
        .build();
}
```

---

### Q5. Chunk-oriented vs Tasklet — when to use which?

**Answer:**

| Aspect | Chunk-oriented | Tasklet |
|--------|:---:|:---:|
| **Pattern** | Read → Process → Write (repeat) | Single task |
| **Data volume** | Large (millions of records) | Small or no data |
| **Transaction** | Per chunk (e.g., every 100 items) | Per execution |
| **Restartable** | ✅ (resumes from last committed chunk) | ✅ (re-runs entire task) |
| **Use case** | CSV import, DB migration, ETL | Send email, clean up files, run SQL |

```java
// Tasklet example — clean up temp files:
@Bean
public Step cleanupStep() {
    return new StepBuilder("cleanup", jobRepository)
        .tasklet((contribution, chunkContext) -> {
            FileUtils.deleteDirectory(new File("/tmp/batch-work"));
            return RepeatStatus.FINISHED;
        }, txManager)
        .build();
}

// Chunk example — process 5M records:
@Bean
public Step processStep() {
    return new StepBuilder("process", jobRepository)
        .<RawTransaction, ProcessedTransaction>chunk(500, txManager)
        .reader(rawTransactionReader())
        .processor(transactionProcessor())
        .writer(processedTransactionWriter())
        .build();
}
```

---

### Q6. Skip and Retry policies — fault tolerance.

**Answer:**

```java
@Bean
public Step importStep() {
    return new StepBuilder("importStep", jobRepository)
        .<UserCsv, User>chunk(100, txManager)
        .reader(csvReader())
        .processor(userProcessor())
        .writer(dbWriter())
        .faultTolerant()

        // Skip: bad records (data quality issues)
        .skip(FlatFileParseException.class)     // malformed CSV row
        .skip(ValidationException.class)         // invalid data
        .skipLimit(100)                          // allow up to 100 bad records
        .skipPolicy(new AlwaysSkipItemSkipPolicy()) // or custom policy

        // Retry: transient failures (will succeed on retry)
        .retry(DeadlockLoserDataAccessException.class)  // DB deadlock
        .retry(OptimisticLockingFailureException.class)  // optimistic lock
        .retryLimit(3)                                    // max 3 attempts
        .backOffPolicy(new ExponentialBackOffPolicy())    // wait between retries

        // Listener: log skipped items
        .listener(new SkipListener<UserCsv, User>() {
            @Override
            public void onSkipInRead(Throwable t) {
                log.warn("Skipped read: {}", t.getMessage());
            }
            @Override
            public void onSkipInProcess(UserCsv item, Throwable t) {
                log.warn("Skipped process: {} - {}", item, t.getMessage());
            }
        })
        .build();
}
```

---

### Q7. Partitioning — parallel processing with Spring Batch.

**Answer:**

```
Partitioning splits data across multiple threads/processes:

  Manager Step (partitioner):
    ├── Worker Step (id 1-10000)     → Thread 1
    ├── Worker Step (id 10001-20000) → Thread 2
    ├── Worker Step (id 20001-30000) → Thread 3
    └── Worker Step (id 30001-40000) → Thread 4

  Each worker processes its own range independently.
  4x faster (if 4 threads).
```

```java
// Partitioner — divides work:
@Component
public class RangePartitioner implements Partitioner {
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long min = orderRepo.findMinId();
        long max = orderRepo.findMaxId();
        long range = (max - min) / gridSize + 1;

        Map<String, ExecutionContext> partitions = new HashMap<>();
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", min + (i * range));
            ctx.putLong("maxId", min + ((i + 1) * range) - 1);
            partitions.put("partition-" + i, ctx);
        }
        return partitions;
    }
}

// Partitioned step:
@Bean
public Step managerStep(Step workerStep, Partitioner partitioner) {
    return new StepBuilder("managerStep", jobRepository)
        .partitioner("workerStep", partitioner)
        .step(workerStep)
        .gridSize(4)
        .taskExecutor(new SimpleAsyncTaskExecutor())  // 4 threads
        .build();
}

// Other parallel options:
// Multi-threaded Step: .taskExecutor(taskExecutor) on chunk step (simple but items may overlap)
// AsyncItemProcessor/AsyncItemWriter: process items async, write in order
// Remote partitioning: distribute across multiple JVM instances (via messaging)
```

---

### Q8. JobRepository and metadata — restart/resume capability.

**Answer:**

```
Spring Batch stores job execution metadata in DB tables:

  BATCH_JOB_INSTANCE      — unique job + parameters combination
  BATCH_JOB_EXECUTION     — each run attempt
  BATCH_JOB_EXECUTION_PARAMS — job parameters
  BATCH_STEP_EXECUTION    — each step run
  BATCH_STEP_EXECUTION_CONTEXT — step state (read count, commit count)

Restart flow:
  1. Job runs, processes 500,000 records
  2. Fails at record 350,000 (DB connection lost)
  3. Step execution context saved: readCount=350,000
  4. Fix DB connection, restart job
  5. Job resumes from record 350,001 (not from scratch!)
  
This works because:
  - JdbcPagingItemReader saves current page in ExecutionContext
  - FlatFileItemReader saves current line number
  - Each committed chunk updates the metadata
```

```yaml
# Auto-create batch metadata tables:
spring:
  batch:
    jdbc:
      initialize-schema: always  # create tables if not exist
    job:
      enabled: false  # don't auto-run all jobs on startup
```

---

### Q9. How do you schedule and trigger batch jobs?

**Answer:**

```java
// Option 1: @Scheduled (simple, in-process):
@Component
@RequiredArgsConstructor
public class JobScheduler {
    private final JobLauncher jobLauncher;
    private final Job importJob;

    @Scheduled(cron = "0 0 2 * * *")  // Every day at 2 AM
    public void runImportJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
            .addLocalDateTime("runTime", LocalDateTime.now())
            .toJobParameters();

        JobExecution execution = jobLauncher.run(importJob, params);
        log.info("Job finished with status: {}", execution.getStatus());
    }
}

// Option 2: REST API trigger:
@RestController
@RequiredArgsConstructor
public class JobController {
    private final JobLauncher jobLauncher;
    private final Job importJob;

    @PostMapping("/jobs/import")
    public ResponseEntity<String> triggerImport(@RequestParam String fileName) {
        JobParameters params = new JobParametersBuilder()
            .addString("fileName", fileName)
            .addLocalDateTime("runTime", LocalDateTime.now())
            .toJobParameters();

        JobExecution execution = jobLauncher.run(importJob, params);
        return ResponseEntity.ok("Job started: " + execution.getId());
    }
}

// Option 3: AWS CloudWatch Events / EventBridge → Lambda → API → trigger job
// Option 4: Jenkins/CI pipeline runs the batch job
// Option 5: Kubernetes CronJob
```

---

### Q10. Real-world Spring Batch design — monthly billing job.

**Answer:**

```
Job: "Monthly Billing"
  Step 1: Validate prerequisites (Tasklet)
    - Check: Is it month-end?
    - Check: Previous month settled?
    - Check: Data sources available?

  Step 2: Calculate charges (Chunk, partitioned)
    - Reader: JdbcPagingItemReader (all active accounts)
    - Processor: Calculate monthly charges per account
    - Writer: JdbcBatchItemWriter (write to billing table)
    - Chunk size: 500
    - Partitioned by account range (8 threads)
    - Skip: accounts with incomplete data (log to DLQ table)

  Step 3: Generate invoices (Chunk)
    - Reader: Read from billing table
    - Processor: Generate PDF invoice
    - Writer: Upload to S3

  Step 4: Send notifications (Chunk)
    - Reader: Read from billing table
    - Processor: Build email content
    - Writer: Send via SES

  Step 5: Reconciliation report (Tasklet)
    - Total accounts processed
    - Total amount billed
    - Skipped accounts (need manual review)
    - Send summary email to operations team

Error handling:
  - Step 2 failure → resume from last committed chunk
  - Step 4 failure → retry 3x (transient email failures)
  - Any step failure → alert operations team (SNS)

Monitoring:
  - Custom metrics: accounts_processed, amount_billed, skip_count
  - CloudWatch alarm if skip_count > threshold
  - Job execution dashboard (Spring Batch Admin or custom)
```

---

## Quick Reference

| Concept | Key Point |
|---------|-----------|
| **Job** | Top-level container, runs Steps sequentially |
| **Step** | Unit of work: chunk-oriented or Tasklet |
| **Chunk** | Read N → Process N → Write N → Commit (repeat) |
| **Tasklet** | Single task (cleanup, notification, SQL) |
| **ItemReader** | Reads one item at a time (CSV, DB, JSON) |
| **ItemProcessor** | Transform/validate (return null to skip) |
| **ItemWriter** | Writes a chunk (batch INSERT) |
| **Skip** | Skip bad records (FlatFileParseException) |
| **Retry** | Retry transient failures (deadlocks) |
| **Partitioning** | Split data across threads for parallel processing |
| **JobRepository** | Stores metadata → enables restart/resume |
| **Scheduling** | @Scheduled, REST trigger, K8s CronJob |
