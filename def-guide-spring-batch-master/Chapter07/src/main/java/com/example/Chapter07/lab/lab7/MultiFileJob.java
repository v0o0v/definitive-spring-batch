package com.example.Chapter07.lab.lab7;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableBatchProcessing
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MultiFileJob extends DefaultBatchConfigurer {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

    @Override
    public void setDataSource(DataSource dataSource) {
    }

    @Bean
	@StepScope
	public FlatFileItemReader customerItemReader() {

		return new FlatFileItemReaderBuilder<Customer>()
				.name("customerItemReader")
				.lineMapper(lineTokenizer())
				.build();
	}

	@Bean
	@StepScope
	public MultiResourceItemReader multiCustomerReader(@Value("#{jobParameters['customerFile']}") Resource[] inputFiles) {
		return new MultiResourceItemReaderBuilder<>()
				.name("multiCustomerReader")
				.resources(inputFiles)
				.delegate(customerFileReader())
				.build();
	}

	@Bean
	public CustomerFileReader customerFileReader() {
		return new CustomerFileReader(customerItemReader());
	}

	@Bean
	public PatternMatchingCompositeLineMapper lineTokenizer() {
		Map<String, LineTokenizer> lineTokenizers =
				new HashMap<>(2);

		lineTokenizers.put("CUST*", customerLineTokenizer());
		lineTokenizers.put("TRANS*", transactionLineTokenizer());

		Map<String, FieldSetMapper> fieldSetMappers =
				new HashMap<>(2);

		BeanWrapperFieldSetMapper<Customer> customerFieldSetMapper =
				new BeanWrapperFieldSetMapper<>();
		customerFieldSetMapper.setTargetType(Customer.class);

		fieldSetMappers.put("CUST*", customerFieldSetMapper);
		fieldSetMappers.put("TRANS*", new TransactionFieldSetMapper());

		PatternMatchingCompositeLineMapper lineMappers =
				new PatternMatchingCompositeLineMapper();

		lineMappers.setTokenizers(lineTokenizers);
		lineMappers.setFieldSetMappers(fieldSetMappers);

		return lineMappers;
	}

	@Bean
	public DelimitedLineTokenizer transactionLineTokenizer() {
		DelimitedLineTokenizer lineTokenizer =
				new DelimitedLineTokenizer();

		lineTokenizer.setNames("prefix",
				"accountNumber",
				"transactionDate",
				"amount");

		return lineTokenizer;
	}

	@Bean
	public DelimitedLineTokenizer customerLineTokenizer() {
		DelimitedLineTokenizer lineTokenizer =
				new DelimitedLineTokenizer();

		lineTokenizer.setNames("firstName",
				"middleInitial",
				"lastName",
				"address",
				"city",
				"state",
				"zipCode");

		lineTokenizer.setIncludedFields(1, 2, 3, 4, 5, 6, 7);

		return lineTokenizer;
	}

	@Bean
	public ItemWriter itemWriter() {
		return (items) -> items.forEach(System.out::println);
	}

	@Bean
	public Step copyFileStep() {
		return this.stepBuilderFactory.get("copyFileStep")
				.<Customer, Customer>chunk(10)
				.reader(multiCustomerReader(null))
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
		List<String> realArgs = Collections.singletonList("customerFile=/input/customerMultiFormat*");

		SpringApplication.run(MultiFileJob.class, realArgs.toArray(new String[1]));
	}

}

