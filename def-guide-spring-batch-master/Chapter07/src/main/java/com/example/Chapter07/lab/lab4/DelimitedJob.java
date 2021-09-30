package com.example.Chapter07.lab.lab4;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
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
public class DelimitedJob extends DefaultBatchConfigurer {

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

        BeanWrapperFieldSetMapper<Customer> customerFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        customerFieldSetMapper.setTargetType(Customer.class);

        FlatFileItemReader<Customer> customerItemReader = new FlatFileItemReaderBuilder<Customer>()
                .name("customerItemReader")
                .lineTokenizer(new CustomerFileLineTokenizer())
                .fieldSetMapper(customerFieldSetMapper)
                .resource(inputFile)
                .build();



//        토크나이저를 설정하고 필드셋맵퍼를 설정하지 않으면 오류 발생
//        FlatFileItemReader<Customer> customerItemReader = new FlatFileItemReaderBuilder<Customer>()
//                .name("customerItemReader")
//                .lineTokenizer(new CustomerFileLineTokenizer())
////                .fieldSetMapper(new CustomerFieldSetMapper())
//                .resource(inputFile)
//                .targetType(Customer.class)
//                .build();



        return customerItemReader;
    }

    @Bean
    public ItemWriter<Customer> itemWriter() {
        return (items) -> items.forEach(System.out::println);
    }

    @Bean
    public Step copyFileStep() {
        return this.stepBuilderFactory.get("copyFileStep")
                .<Customer, Customer>chunk(10)
                .reader(customerItemReader(null))
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
        List<String> realArgs = Arrays.asList("customerFile=/input/customer.csv");

        SpringApplication.run(DelimitedJob.class, realArgs.toArray(new String[1]));
    }

}


