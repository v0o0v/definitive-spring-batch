package com.example.Chapter07.lab.lab1;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

@EnableBatchProcessing
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class FixedWidthJob extends DefaultBatchConfigurer {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Override
    public void setDataSource(DataSource dataSource) {
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Customer> customerItemReader(
            @Value("#{jobParameters['customerFile']}") Resource inputFile) {

        final FlatFileItemReader<Customer> customerItemReader = new FlatFileItemReaderBuilder<Customer>()
                .name("customerItemReader")//ItemStream에 이름 제공하여 EC에서 사용.
                .resource(inputFile)
                .fixedLength()//FixedLengthTokenizer 사용
                .columns(new Range[]{
                        new Range(1, 11)
                        , new Range(12, 12)
                        , new Range(13, 22)
                        , new Range(23, 26)
                        , new Range(27, 46)
                        , new Range(47, 62)
                        , new Range(63, 64)
                        , new Range(65, 69)})
                .names(new String[]{
                        "firstName", "middleInitial", "lastName", "addressNumber", "street", "city", "state", "zipCode"})
                .targetType(Customer.class)
                .build();
        return customerItemReader;
    }

    @Bean
    public ItemWriter<Customer> itemWriter() {
        return (items) -> items.forEach(System.out::println);
    }

    @Bean
    public ItemProcessor<Customer, Customer> itemProcessor() {
        return item -> item;
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
                .<Customer, Customer>chunk(2)
                .reader(customerItemReader(null))
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("job")
                .start(copyFileStep())
                .build();
    }


    public static void main(String[] args) {
        List<String> realArgs = Arrays.asList("customerFile=/input/customerFixedWidth.txt");

        SpringApplication.run(FixedWidthJob.class, realArgs.toArray(new String[1]));
    }

}

