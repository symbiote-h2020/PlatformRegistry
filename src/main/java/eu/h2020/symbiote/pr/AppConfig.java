package eu.h2020.symbiote.pr;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import eu.h2020.symbiote.pr.repositories.converters.ObjectNodeReadConverter;
import eu.h2020.symbiote.pr.repositories.converters.ObjectNodeWriteConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableMongoRepositories
public class AppConfig extends AbstractMongoConfiguration {

    @Value("${symbiote.cloud.platformRegistry.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.host:localhost}")
    private String mongoHost;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public Mongo mongo() {
        return new MongoClient(mongoHost);
    }

    @Override
    protected Collection<String> getMappingBasePackages() { return Collections.singletonList("com.oreilly.springdata.mongodb"); }

    @Bean
    @Override
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converterList = new ArrayList<>();
        converterList.add(new ObjectNodeReadConverter());
        converterList.add(new ObjectNodeWriteConverter());
        return new CustomConversions(converterList);
    }

}